const { AccessToken } = require("livekit-server-sdk");

module.exports = function registerLiveKitRoutes(app, db, authenticateToken) {
  const liveKitUrl = process.env.LIVEKIT_URL || "wss://voicenokiraq.duckdns.org";
  const apiKey = process.env.LIVEKIT_API_KEY;
  const apiSecret = process.env.LIVEKIT_API_SECRET;

  // Eight persistent speaker seats and hand-raise queue. Created safely without
  // touching existing users, rooms, memberships, or LiveKit credentials.
  let schemaReady;
  const ensureVoiceSchema = () => {
    if (!schemaReady) {
      schemaReady = db.query(`
        CREATE TABLE IF NOT EXISTS voice_room_seats (
          room_id UUID NOT NULL REFERENCES voice_rooms(id) ON DELETE CASCADE,
          seat_number SMALLINT NOT NULL CHECK (seat_number BETWEEN 1 AND 8),
          user_id UUID REFERENCES users(id) ON DELETE SET NULL,
          is_muted BOOLEAN NOT NULL DEFAULT FALSE,
          assigned_at TIMESTAMPTZ,
          PRIMARY KEY (room_id, seat_number),
          UNIQUE (room_id, user_id)
        );
        CREATE TABLE IF NOT EXISTS voice_room_hand_raises (
          room_id UUID NOT NULL REFERENCES voice_rooms(id) ON DELETE CASCADE,
          user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
          requested_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
          PRIMARY KEY (room_id, user_id)
        );
        INSERT INTO voice_room_seats(room_id, seat_number, user_id, assigned_at)
        SELECT id, 1, owner_id, NOW() FROM voice_rooms
        ON CONFLICT (room_id, seat_number) DO NOTHING;
      `);
    }
    return schemaReady;
  };

  async function getRoom(roomId) {
    const result = await db.query(
      "SELECT id, owner_id, status FROM voice_rooms WHERE id = $1 LIMIT 1",
      [roomId]
    );
    return result.rows[0] || null;
  }

  async function isMember(roomId, userId) {
    const result = await db.query(
      "SELECT 1 FROM room_members WHERE room_id = $1 AND user_id = $2 LIMIT 1",
      [roomId, userId]
    );
    return result.rows.length > 0;
  }

  function validRoomId(value) {
    return /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(String(value || ""));
  }

  app.get("/api/voice-rooms/:roomId/seats", authenticateToken, async (req, res) => {
    try {
      await ensureVoiceSchema();
      const roomId = req.params.roomId;
      if (!validRoomId(roomId)) return res.status(400).json({ error: "Invalid room ID" });
      if (!(await isMember(roomId, req.userId))) return res.status(403).json({ error: "Join the room first" });
      const [seats, hands] = await Promise.all([
        db.query(`
          SELECT s.seat_number, s.user_id, s.is_muted, s.assigned_at,
                 COALESCE(p.display_name, u.username) AS display_name,
                 p.avatar_url, u.username
          FROM voice_room_seats s
          LEFT JOIN users u ON u.id = s.user_id
          LEFT JOIN user_profiles p ON p.user_id = u.id
          WHERE s.room_id = $1 ORDER BY s.seat_number
        `, [roomId]),
        db.query(`
          SELECT h.user_id, COALESCE(p.display_name, u.username) AS display_name,
                 h.requested_at
          FROM voice_room_hand_raises h
          JOIN users u ON u.id = h.user_id
          LEFT JOIN user_profiles p ON p.user_id = u.id
          WHERE h.room_id = $1 ORDER BY h.requested_at
        `, [roomId])
      ]);
      const occupied = new Map(seats.rows.map(s => [Number(s.seat_number), s]));
      const eightSeats = Array.from({ length: 8 }, (_, i) =>
        occupied.get(i + 1) || { seat_number: i + 1, user_id: null, is_muted: false, display_name: null }
      );
      const room = await getRoom(roomId);
      return res.json({ seats: eightSeats, handRaises: hands.rows, ownerId: room && room.owner_id });
    } catch (error) {
      console.error("List voice seats:", error.message);
      return res.status(500).json({ error: "Could not load voice seats" });
    }
  });

  app.post("/api/voice-rooms/:roomId/hand-raise", authenticateToken, async (req, res) => {
    try {
      await ensureVoiceSchema();
      const roomId = req.params.roomId;
      if (!validRoomId(roomId)) return res.status(400).json({ error: "Invalid room ID" });
      if (!(await isMember(roomId, req.userId))) return res.status(403).json({ error: "Join the room first" });
      await db.query(
        "INSERT INTO voice_room_hand_raises(room_id,user_id) VALUES($1,$2) ON CONFLICT(room_id,user_id) DO NOTHING",
        [roomId, req.userId]
      );
      return res.json({ message: "Hand raised" });
    } catch (error) {
      console.error("Raise hand:", error.message);
      return res.status(500).json({ error: "Could not raise hand" });
    }
  });

  app.delete("/api/voice-rooms/:roomId/hand-raise", authenticateToken, async (req, res) => {
    try {
      await ensureVoiceSchema();
      await db.query("DELETE FROM voice_room_hand_raises WHERE room_id=$1 AND user_id=$2", [req.params.roomId, req.userId]);
      return res.json({ message: "Hand lowered" });
    } catch (error) {
      console.error("Lower hand:", error.message);
      return res.status(500).json({ error: "Could not lower hand" });
    }
  });

  app.post("/api/voice-rooms/:roomId/seats/:seatNumber/assign", authenticateToken, async (req, res) => {
    const client = await db.connect();
    try {
      await ensureVoiceSchema();
      const { roomId, seatNumber } = req.params;
      const seat = Number(seatNumber);
      if (!validRoomId(roomId) || !Number.isInteger(seat) || seat < 1 || seat > 8) {
        return res.status(400).json({ error: "Invalid room or seat" });
      }
      await client.query("BEGIN");
      const roomResult = await client.query("SELECT owner_id FROM voice_rooms WHERE id=$1 AND status='active' FOR UPDATE", [roomId]);
      if (!roomResult.rows.length) {
        await client.query("ROLLBACK");
        return res.status(404).json({ error: "Room not found" });
      }
      if (String(roomResult.rows[0].owner_id) !== String(req.userId)) {
        await client.query("ROLLBACK");
        return res.status(403).json({ error: "Only the room owner can assign seats" });
      }
      const targetUserId = req.body && req.body.userId;
      if (!targetUserId || !(await isMember(roomId, targetUserId))) {
        await client.query("ROLLBACK");
        return res.status(400).json({ error: "User must be in the room" });
      }
      await client.query(
        "INSERT INTO voice_room_seats(room_id,seat_number,user_id,assigned_at) VALUES($1,$2,$3,NOW()) ON CONFLICT(room_id,seat_number) DO UPDATE SET user_id=EXCLUDED.user_id,assigned_at=NOW()",
        [roomId, seat, targetUserId]
      );
      await client.query("DELETE FROM voice_room_hand_raises WHERE room_id=$1 AND user_id=$2", [roomId, targetUserId]);
      await client.query("COMMIT");
      return res.json({ message: "Seat assigned", seatNumber: seat, userId: targetUserId });
    } catch (error) {
      try { await client.query("ROLLBACK"); } catch (_) {}
      console.error("Assign voice seat:", error.message);
      return res.status(500).json({ error: "Could not assign seat" });
    } finally {
      client.release();
    }
  });

  app.delete("/api/voice-rooms/:roomId/seats/:seatNumber", authenticateToken, async (req, res) => {
    try {
      await ensureVoiceSchema();
      const { roomId, seatNumber } = req.params;
      const room = await getRoom(roomId);
      if (!room) return res.status(404).json({ error: "Room not found" });
      if (String(room.owner_id) !== String(req.userId)) return res.status(403).json({ error: "Only the room owner can release seats" });
      const seat = Number(seatNumber);
      if (!Number.isInteger(seat) || seat < 1 || seat > 8) return res.status(400).json({ error: "Invalid seat" });
      await db.query("UPDATE voice_room_seats SET user_id=NULL,is_muted=FALSE,assigned_at=NULL WHERE room_id=$1 AND seat_number=$2", [roomId, seat]);
      return res.json({ message: "Seat released" });
    } catch (error) {
      console.error("Release voice seat:", error.message);
      return res.status(500).json({ error: "Could not release seat" });
    }
  });

  app.post("/api/voice-rooms/:roomId/seats/:seatNumber/mute", authenticateToken, async (req, res) => {
    try {
      await ensureVoiceSchema();
      const { roomId, seatNumber } = req.params;
      const room = await getRoom(roomId);
      if (!room) return res.status(404).json({ error: "Room not found" });
      if (String(room.owner_id) !== String(req.userId)) return res.status(403).json({ error: "Only the room owner can mute speakers" });
      const seat = Number(seatNumber);
      const muted = Boolean(req.body && req.body.muted);
      const result = await db.query("UPDATE voice_room_seats SET is_muted=$3 WHERE room_id=$1 AND seat_number=$2 AND user_id IS NOT NULL RETURNING seat_number", [roomId, seat, muted]);
      if (!result.rows.length) return res.status(404).json({ error: "Occupied seat not found" });
      return res.json({ message: muted ? "Speaker muted" : "Speaker unmuted" });
    } catch (error) {
      console.error("Mute voice speaker:", error.message);
      return res.status(500).json({ error: "Could not update mute state" });
    }
  });

  app.post("/api/voice-rooms/:roomId/token", authenticateToken, async (req, res) => {
    const roomId = String(req.params.roomId || "");
    if (!validRoomId(roomId)) return res.status(400).json({ error: "Invalid room ID" });
    if (!apiKey || !apiSecret) return res.status(503).json({ error: "LiveKit credentials are not configured" });

    try {
      await ensureVoiceSchema();
      const membership = await db.query(
        `SELECT r.id, r.owner_id
         FROM voice_rooms r
         JOIN room_members m ON m.room_id = r.id
         WHERE r.id = $1 AND r.status = 'active' AND m.user_id = $2
         LIMIT 1`,
        [roomId, req.userId]
      );
      if (!membership.rows.length) return res.status(403).json({ error: "Join the room before requesting an audio token" });

      const seat = await db.query(
        "SELECT s.seat_number, s.is_muted FROM voice_room_seats s WHERE s.room_id=$1 AND s.user_id=$2 LIMIT 1",
        [roomId, req.userId]
      );
      const isOwner = String(membership.rows[0].owner_id) === String(req.userId);
      const canPublish = isOwner || (seat.rows.length > 0 && !seat.rows[0].is_muted);
      const token = new AccessToken(apiKey, apiSecret, { identity: String(req.userId), ttl: "10m" });
      token.addGrant({
        roomJoin: true, room: roomId, canPublish,
        canSubscribe: true, canPublishData: true
      });
      const participantToken = await token.toJwt();
      return res.json({ serverUrl: liveKitUrl, participantToken, canPublish });
    } catch (error) {
      console.error("LiveKit token error:", error.message);
      return res.status(500).json({ error: "Could not create audio token" });
    }
  });
};
