const { RoomServiceClient } = require("livekit-server-sdk");

module.exports = (app, db, authenticateToken) => {
  const liveKitHttpUrl = (process.env.LIVEKIT_URL || "wss://voicenokiraq.duckdns.org")
    .replace(/^wss:/, "https:").replace(/^ws:/, "http:");
  const liveKitApiKey = process.env.LIVEKIT_API_KEY;
  const liveKitApiSecret = process.env.LIVEKIT_API_SECRET;
  const roomService = liveKitApiKey && liveKitApiSecret
    ? new RoomServiceClient(liveKitHttpUrl, liveKitApiKey, liveKitApiSecret)
    : null;

  const disconnectRoomParticipants = async (roomId) => {
    if (!roomService) return;
    try {
      const participants = await roomService.listParticipants(String(roomId));
      for (const participant of participants) {
        try { await roomService.removeParticipant(String(roomId), participant.identity); }
        catch (error) { console.warn("LiveKit participant removal:", error.message); }
      }
    } catch (error) {
      console.warn("LiveKit room participant listing:", error.message);
    }
  };

  // Automatically close rooms that have had no occupied audio or camera seat for 3 minutes.
  // The timestamp is persisted so the countdown survives backend restarts.
  let emptyRoomCleanupRunning = false;
  const cleanupEmptyRooms = async () => {
    if (emptyRoomCleanupRunning) return;
    emptyRoomCleanupRunning = true;
    try {
      await db.query(`
        UPDATE voice_rooms r
        SET empty_since = CASE
          WHEN EXISTS (
            SELECT 1 FROM voice_room_seats s
            WHERE s.room_id = r.id AND s.user_id IS NOT NULL
          ) OR EXISTS (
            SELECT 1 FROM voice_room_camera_seats cs
            WHERE cs.room_id = r.id AND cs.user_id IS NOT NULL
          ) THEN NULL
          ELSE COALESCE(r.empty_since, NOW())
        END
        WHERE r.status = 'active'
      `);

      const closed = await db.query(`
        UPDATE voice_rooms r
        SET status = 'closed'
        WHERE r.status = 'active'
          AND r.empty_since <= NOW() - INTERVAL '3 minutes'
          AND NOT EXISTS (
            SELECT 1 FROM voice_room_seats s
            WHERE s.room_id = r.id AND s.user_id IS NOT NULL
          )
          AND NOT EXISTS (
            SELECT 1 FROM voice_room_camera_seats cs
            WHERE cs.room_id = r.id AND cs.user_id IS NOT NULL
          )
        RETURNING r.id
      `);
      if (closed.rows.length) {
        console.log("Auto-closed empty BAK LIVE rooms:", closed.rows.map(row => row.id).join(", "));
      }
    } catch (error) {
      console.error("Empty room cleanup:", error.message);
    } finally {
      emptyRoomCleanupRunning = false;
    }
  };

  const emptyRoomCleanupTimer = setInterval(cleanupEmptyRooms, 15000);
  if (typeof emptyRoomCleanupTimer.unref === "function") emptyRoomCleanupTimer.unref();


  // Return the authenticated user's saved profile, including the persisted avatar URL.
  app.get("/api/profile", authenticateToken, async (req, res) => {
    try {
      const result = await db.query(
        `SELECT u.username, u.public_id, p.display_name, p.bio, p.country, p.avatar_url
         FROM users u
         LEFT JOIN user_profiles p ON p.user_id = u.id
         WHERE u.id = $1 LIMIT 1`,
        [req.userId]
      );
      if (!result.rows.length) return res.status(404).json({ error: "User profile not found" });
      return res.json({ profile: result.rows[0] });
    } catch (error) {
      console.error("Load profile:", error.message);
      return res.status(500).json({ error: "Could not load profile" });
    }
  });

  // Upload the authenticated user's profile photo and publish its public URL.
  app.post("/api/profile/avatar", authenticateToken, async (req, res) => {
    let savedPath = null;
    try {
      const encoded = String(req.body && req.body.avatar_base64 || "");
      if (!encoded || encoded.length > 2 * 1024 * 1024) {
        return res.status(413).json({ error: "Profile image must be smaller than 1.5 MB" });
      }
      const buffer = Buffer.from(encoded, "base64");
      const validJpeg = buffer.length >= 3 && buffer[0] === 0xff && buffer[1] === 0xd8 && buffer[2] === 0xff;
      if (!validJpeg || buffer.length > 1536 * 1024) {
        return res.status(400).json({ error: "Choose a valid JPG profile image" });
      }
      const fs = require("fs");
      const path = require("path");
      const crypto = require("crypto");
      const directory = "/opt/mariam-live/uploads/avatars";
      fs.mkdirSync(directory, { recursive: true });
      const filename = crypto.randomUUID() + ".jpg";
      savedPath = path.join(directory, filename);
      fs.writeFileSync(savedPath, buffer, { flag: "wx", mode: 0o640 });
      const avatarUrl = "https://nokiraq.duckdns.org/uploads/avatars/" + filename;
      await db.query(
        "INSERT INTO user_profiles (user_id, avatar_url) VALUES ($1, $2) ON CONFLICT (user_id) DO UPDATE SET avatar_url = EXCLUDED.avatar_url",
        [req.userId, avatarUrl]
      );
      return res.json({ message: "Profile photo updated", avatar_url: avatarUrl });
    } catch (error) {
      if (savedPath) { try { require("fs").unlinkSync(savedPath); } catch (_) {} }
      console.error("Upload profile avatar:", error.message);
      return res.status(500).json({ error: "Could not update profile photo" });
    }
  });


  // Update the room background. Only the room owner may change it.
  app.put("/api/voice-rooms/:roomId/background", authenticateToken, async (req, res) => {
    let savedPath = null;
    try {
      const roomId = String(req.params.roomId || "");
      if (!/^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(roomId)) {
        return res.status(400).json({ error: "Invalid room ID" });
      }
      const room = await db.query(
        "SELECT owner_id FROM voice_rooms WHERE id=$1 AND status='active' LIMIT 1",
        [roomId]
      );
      if (!room.rows.length) return res.status(404).json({ error: "Room not found" });
      if (String(room.rows[0].owner_id) !== String(req.userId)) {
        return res.status(403).json({ error: "Only the room owner can change the background" });
      }

      const encoded = String(req.body && req.body.background_base64 || "");
      if (!encoded || encoded.length > 6 * 1024 * 1024) {
        return res.status(413).json({ error: "Background image must be 4 MB or smaller" });
      }
      const buffer = Buffer.from(encoded, "base64");
      const mime = String(req.body && req.body.image_mime || "image/jpeg");
      const valid = mime === "image/jpeg"
        ? buffer.length >= 3 && buffer[0] === 0xff && buffer[1] === 0xd8 && buffer[2] === 0xff
        : mime === "image/png"
          ? buffer.length >= 8 && buffer.subarray(0, 8).equals(Buffer.from([137,80,78,71,13,10,26,10]))
          : mime === "image/webp"
            ? buffer.length >= 12 && buffer.toString("ascii", 0, 4) === "RIFF" && buffer.toString("ascii", 8, 12) === "WEBP"
            : false;
      if (!valid || buffer.length > 4 * 1024 * 1024) {
        return res.status(400).json({ error: "Choose a valid JPG, PNG or WebP image up to 4 MB" });
      }

      const fs = require("fs/promises");
      const path = require("path");
      const crypto = require("crypto");
      const directory = "/opt/mariam-live/uploads/rooms";
      await fs.mkdir(directory, { recursive: true, mode: 0o750 });
      const extension = mime === "image/jpeg" ? "jpg" : mime === "image/png" ? "png" : "webp";
      const filename = crypto.randomUUID() + "-background." + extension;
      savedPath = path.join(directory, filename);
      await fs.writeFile(savedPath, buffer, { flag: "wx", mode: 0o640 });
      const backgroundUrl = "https://nokiraq.duckdns.org/uploads/rooms/" + filename;
      await db.query(
        "UPDATE voice_rooms SET room_background_url=$2 WHERE id=$1 AND owner_id=$3",
        [roomId, backgroundUrl, req.userId]
      );
      return res.json({ message: "Room background updated", background_url: backgroundUrl });
    } catch (error) {
      if (savedPath) {
        try { await require("fs/promises").unlink(savedPath); } catch (_) {}
      }
      console.error("Update room background:", error.message);
      return res.status(500).json({ error: "Could not update room background" });
    }
  });

  // Create a room with a required, validated image selected by the owner.
  app.post("/api/voice-rooms", authenticateToken, async (req, res) => {
    let savedFile = null;
    let savedBackgroundFile = null;
    try {
      const name = String(req.body.name || "").trim();
      const roomMode = String(req.body.room_mode || "audio");
      const cameraSeatCount = Number(req.body.camera_seat_count || 0);
      const max_members = Number(req.body.max_members || 8);
      if (!["audio", "camera"].includes(roomMode)) return res.status(400).json({ error: "Invalid room mode" });
      if ((roomMode === "audio" && cameraSeatCount !== 0) || (roomMode === "camera" && ![4, 6].includes(cameraSeatCount))) return res.status(400).json({ error: "Invalid camera seat count" });
      if (roomMode === "camera" && max_members !== cameraSeatCount) return res.status(400).json({ error: "Room capacity must match camera seats" });
      if (roomMode === "audio" && ![8, 15].includes(max_members)) return res.status(400).json({ error: "Audio room capacity must be 8 or 15" });
      const imageData = String(req.body.image_base64 || "");
      const mimeType = String(req.body.image_mime || "");
      const backgroundData = String(req.body.background_base64 || "");
      if (name.length < 2 || name.length > 100) return res.status(400).json({ error: "Room name must be 2-100 characters" });
      if (!Number.isInteger(max_members) || max_members < 2 || max_members > 15 ||
          (roomMode === "audio" && ![8, 15].includes(max_members)) ||
          (roomMode === "camera" && ![4, 6].includes(max_members))) {
        return res.status(400).json({ error: "Audio rooms must have 8 or 15 seats; camera rooms must have 4 or 6 seats" });
      }
      if (!["image/jpeg", "image/png", "image/webp"].includes(mimeType) || !imageData) return res.status(400).json({ error: "Choose a JPG, PNG or WebP room image" });
      const buffer = Buffer.from(imageData, "base64");
      if (!buffer.length || buffer.length > 4 * 1024 * 1024) return res.status(413).json({ error: "Room image must be 4 MB or smaller" });
      const signatures = {
        "image/jpeg": buffer.length >= 3 && buffer[0] === 0xff && buffer[1] === 0xd8 && buffer[2] === 0xff,
        "image/png": buffer.length >= 8 && buffer.subarray(0, 8).equals(Buffer.from([137,80,78,71,13,10,26,10])),
        "image/webp": buffer.length >= 12 && buffer.toString("ascii",0,4) === "RIFF" && buffer.toString("ascii",8,12) === "WEBP"
      };
      if (!signatures[mimeType]) return res.status(400).json({ error: "Invalid image file" });
      const fs = require("fs");
      const path = require("path");
      const crypto = require("crypto");
      const directory = "/opt/mariam-live/uploads/rooms";
      fs.mkdirSync(directory, { recursive: true });
      const extension = mimeType === "image/jpeg" ? "jpg" : mimeType === "image/png" ? "png" : "webp";
      const filename = crypto.randomUUID() + "." + extension;
      savedFile = path.join(directory, filename);
      fs.writeFileSync(savedFile, buffer, { flag: "wx", mode: 0o640 });
      const imageUrl = "https://nokiraq.duckdns.org/uploads/rooms/" + filename;
      let backgroundUrl = null;
      if (backgroundData) {
        const backgroundBuffer = Buffer.from(backgroundData, "base64");
        const validJpeg = backgroundBuffer.length >= 3 && backgroundBuffer[0] === 0xff && backgroundBuffer[1] === 0xd8 && backgroundBuffer[2] === 0xff;
        if (!validJpeg || backgroundBuffer.length > 4 * 1024 * 1024) throw new Error("Invalid room background image");
        const backgroundFilename = crypto.randomUUID() + "-background.jpg";
        savedBackgroundFile = path.join(directory, backgroundFilename);
        fs.writeFileSync(savedBackgroundFile, backgroundBuffer, { flag: "wx", mode: 0o640 });
        backgroundUrl = "https://nokiraq.duckdns.org/uploads/rooms/" + backgroundFilename;
      }
      const client = await db.connect();
      try {
        await client.query("BEGIN");
        await client.query("ALTER TABLE voice_rooms ADD COLUMN IF NOT EXISTS room_mode VARCHAR(12) NOT NULL DEFAULT 'audio'");
        await client.query("ALTER TABLE voice_rooms ADD COLUMN IF NOT EXISTS camera_seat_count SMALLINT NOT NULL DEFAULT 0");
        const result = await client.query(
          `INSERT INTO voice_rooms (owner_id, name, max_members, status, image_url, room_background_url, room_mode, camera_seat_count)
           VALUES ($1, $2, $3, 'active', $4, $5, $6, $7) RETURNING *`,
          [req.userId, name, max_members, imageUrl, backgroundUrl, roomMode, cameraSeatCount]
        );
        await client.query("INSERT INTO room_members (room_id, user_id) VALUES ($1, $2)", [result.rows[0].id, req.userId]);
        await client.query("COMMIT");
        return res.status(201).json({ message: "Voice room created", room: result.rows[0] });
      } catch (error) {
        await client.query("ROLLBACK");
        throw error;
      } finally { client.release(); }
    } catch (error) {
      if (savedFile) { try { require("fs").unlinkSync(savedFile); } catch (_) {} }
      if (savedBackgroundFile) { try { require("fs").unlinkSync(savedBackgroundFile); } catch (_) {} }
      console.error("Create room:", error.message);
      return res.status(500).json({ error: "Could not create room" });
    }
  });

  // List active rooms
  app.get("/api/voice-rooms", authenticateToken, async (req, res) => {
    try {
      const result = await db.query(`
        SELECT
          r.*,
          COUNT(m.user_id)::int AS member_count
        FROM voice_rooms r
        LEFT JOIN room_members m ON m.room_id = r.id
        WHERE r.status = 'active'
        GROUP BY r.id
        ORDER BY r.created_at DESC
      `);

      res.json({ rooms: result.rows });

    } catch (error) {
      console.error("List rooms:", error.message);
      res.status(500).json({ error: "Could not load rooms" });
    }
  });

  // Join voice room
  app.post("/api/voice-rooms/:roomId/join", authenticateToken, async (req, res) => {
    const client = await db.connect();

    try {
      await client.query("BEGIN");

      const roomId = req.params.roomId;

      await client.query(
        "SELECT pg_advisory_xact_lock(hashtext($1))",
        [roomId]
      );

      const room = await client.query(
        `SELECT * FROM voice_rooms
         WHERE id = $1 AND status = 'active'`,
        [roomId]
      );

      if (!room.rows.length) {
        await client.query("ROLLBACK");
        return res.status(404).json({ error: "Room not found" });
      }

      const existing = await client.query(
        `SELECT 1 FROM room_members
         WHERE room_id = $1 AND user_id = $2`,
        [roomId, req.userId]
      );

      if (!existing.rows.length) {
        const count = await client.query(
          "SELECT COUNT(*)::int AS total FROM room_members WHERE room_id = $1",
          [roomId]
        );

        if (count.rows[0].total >= room.rows[0].max_members) {
          await client.query("ROLLBACK");
          return res.status(409).json({ error: "Room is full" });
        }

        await client.query(
          `INSERT INTO room_members (room_id, user_id)
           VALUES ($1, $2)`,
          [roomId, req.userId]
        );
      }

      await client.query("COMMIT");

      res.json({ message: "Joined room successfully" });

    } catch (error) {
      await client.query("ROLLBACK");
      console.error("Join room:", error.message);
      res.status(500).json({ error: "Could not join room" });

    } finally {
      client.release();
    }
  });

  // Leave voice room
  app.post("/api/voice-rooms/:roomId/leave", authenticateToken, async (req, res) => {
    try {
      await db.query(
        "UPDATE voice_room_seats SET user_id=NULL,is_muted=FALSE,assigned_at=NULL WHERE room_id=$1 AND user_id=$2",
        [req.params.roomId, req.userId]
      );
      await db.query(
        "UPDATE voice_room_camera_seats SET user_id=NULL,is_muted=FALSE,assigned_at=NULL WHERE room_id=$1 AND user_id=$2",
        [req.params.roomId, req.userId]
      );
      await db.query(
        "DELETE FROM voice_room_presence WHERE room_id=$1 AND user_id=$2",
        [req.params.roomId, req.userId]
      );
      const result = await db.query(
        `DELETE FROM room_members
         WHERE room_id = $1 AND user_id = $2
         RETURNING room_id`,
        [req.params.roomId, req.userId]
      );

      await db.query(
        "DELETE FROM voice_room_presence WHERE room_id=$1 AND user_id=$2",
        [req.params.roomId, req.userId]
      );
      if (!result.rows.length) {
        return res.status(404).json({ error: "Membership not found" });
      }

      // Explicit leave must stop the published microphone immediately, not just
      // clear the database seat.
      if (roomService) {
        try { await roomService.removeParticipant(String(req.params.roomId), String(req.userId)); }
        catch (error) {
          if (!/not found|does not exist|participant/i.test(String(error.message || ""))) {
            console.warn("Leave room LiveKit disconnect:", error.message);
          }
        }
      }
      res.json({ message: "Left room successfully" });

    } catch (error) {
      console.error("Leave room:", error.message);
      res.status(500).json({ error: "Could not leave room" });
    }
  });

  // Close room (owner only)
  app.delete("/api/voice-rooms/:roomId", authenticateToken, async (req, res) => {
    try {
      const client = await db.connect();
      let roomId = null;
      try {
        await client.query("BEGIN");
        const result = await client.query(
          `UPDATE voice_rooms
           SET status = 'closed'
           WHERE id = $1 AND owner_id = $2
           RETURNING id`,
          [req.params.roomId, req.userId]
        );
        if (!result.rows.length) {
          await client.query("ROLLBACK");
          return res.status(404).json({ error: "Room not found or you are not the owner" });
        }
        roomId = result.rows[0].id;
        await client.query("UPDATE voice_room_seats SET user_id=NULL,is_muted=FALSE,assigned_at=NULL WHERE room_id=$1",[roomId]);
        await client.query("UPDATE voice_room_camera_seats SET user_id=NULL,is_muted=FALSE,assigned_at=NULL WHERE room_id=$1",[roomId]);
        await client.query("DELETE FROM room_members WHERE room_id=$1",[roomId]);
        await client.query("DELETE FROM voice_room_presence WHERE room_id=$1",[roomId]);
        await client.query("COMMIT");
      } catch (error) {
        try { await client.query("ROLLBACK"); } catch (_) {}
        throw error;
      } finally {
        client.release();
      }
      await disconnectRoomParticipants(roomId);
      res.json({ message: "Room closed successfully" });

    } catch (error) {
      console.error("Close room:", error.message);
      res.status(500).json({ error: "Could not close room" });
    }
  });


  // BAK LIVE watch-together: official YouTube Data API search and room playback state.
  // Keep YOUTUBE_API_KEY on the backend environment; never ship it in the Android app.
  let watchStateTableReady = null;
  const ensureWatchStateTable = async () => {
    if (!watchStateTableReady) {
      watchStateTableReady = db.query(`
        CREATE TABLE IF NOT EXISTS bak_room_watch_state (
          room_id UUID PRIMARY KEY,
          video_id VARCHAR(32) NOT NULL DEFAULT '',
          video_title TEXT NOT NULL DEFAULT '',
          playback_state VARCHAR(12) NOT NULL DEFAULT 'paused',
          "current_time" DOUBLE PRECISION NOT NULL DEFAULT 0,
          updated_at BIGINT NOT NULL DEFAULT 0,
          updated_by TEXT
        )
      `).catch((error) => {
        watchStateTableReady = null;
        throw error;
      });
    }
    return watchStateTableReady;
  };

  const validWatchRoomId = (value) =>
    /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(String(value || ""));

  app.get("/api/watch/youtube/search", authenticateToken, async (req, res) => {
    try {
      const query = String(req.query.q || "").trim().slice(0, 100);
      if (query.length < 2) return res.status(400).json({ error: "اكتب كلمتين على الأقل للبحث" });
      const apiKey = String(process.env.YOUTUBE_API_KEY || "").trim();
      if (!apiKey) return res.status(503).json({ error: "بحث YouTube غير مهيأ على الخادم بعد" });
      const params = new URLSearchParams({
        part: "snippet",
        type: "video",
        videoEmbeddable: "true",
        maxResults: "15",
        safeSearch: "moderate",
        q: query,
        key: apiKey
      });
      const response = await fetch("https://www.googleapis.com/youtube/v3/search?" + params.toString());
      const payload = await response.json();
      if (!response.ok) {
        console.error("YouTube search API:", payload && payload.error ? payload.error.message : response.status);
        return res.status(response.status === 403 ? 503 : 502).json({ error: "تعذر البحث في YouTube حالياً" });
      }
      const items = Array.isArray(payload.items) ? payload.items : [];
      return res.json({
        results: items.map((item) => ({
          video_id: String(item.id && item.id.videoId || ""),
          title: String(item.snippet && item.snippet.title || ""),
          channel: String(item.snippet && item.snippet.channelTitle || ""),
          thumbnail: String(item.snippet && item.snippet.thumbnails && (item.snippet.thumbnails.medium || item.snippet.thumbnails.default) && (item.snippet.thumbnails.medium || item.snippet.thumbnails.default).url || "")
        })).filter((item) => /^[A-Za-z0-9_-]{11}$/.test(item.video_id))
      });
    } catch (error) {
      console.error("YouTube search:", error.message);
      return res.status(500).json({ error: "حدث خطأ أثناء البحث في YouTube" });
    }
  });

  app.get("/api/voice-rooms/:roomId/watch/state", authenticateToken, async (req, res) => {
    try {
      const roomId = String(req.params.roomId || "");
      if (!validWatchRoomId(roomId)) return res.status(400).json({ error: "Invalid room ID" });
      await ensureWatchStateTable();
      const membership = await db.query(
        `SELECT r.owner_id, EXISTS(
           SELECT 1 FROM room_members m WHERE m.room_id = r.id AND m.user_id = $2
         ) AS is_member
         FROM voice_rooms r WHERE r.id = $1 AND r.status = 'active' LIMIT 1`,
        [roomId, req.userId]
      );
      if (!membership.rows.length) return res.status(404).json({ error: "Room not found" });
      if (!membership.rows[0].is_member) return res.status(403).json({ error: "انضم إلى الغرفة أولاً" });
      const state = await db.query("SELECT * FROM bak_room_watch_state WHERE room_id=$1 LIMIT 1", [roomId]);
      const row = state.rows[0] || {};
      return res.json({
        is_owner: String(membership.rows[0].owner_id) === String(req.userId),
        state: {
          video_id: row.video_id || "",
          video_title: row.video_title || "",
          playback_state: row.playback_state || "paused",
          current_time: Number(row.current_time || 0),
          updated_at: Number(row.updated_at || 0),
          updated_by: row.updated_by || ""
        }
      });
    } catch (error) {
      console.error("Get room watch state:", error.message);
      return res.status(500).json({ error: "تعذر تحميل حالة المشاهدة" });
    }
  });

  app.put("/api/voice-rooms/:roomId/watch/state", authenticateToken, async (req, res) => {
    try {
      const roomId = String(req.params.roomId || "");
      if (!validWatchRoomId(roomId)) return res.status(400).json({ error: "Invalid room ID" });
      const videoId = String(req.body && req.body.video_id || "");
      const title = String(req.body && req.body.video_title || "").trim().slice(0, 300);
      const playbackState = String(req.body && req.body.playback_state || "paused");
      const currentTime = Number(req.body && req.body.current_time || 0);
      if (videoId && !/^[A-Za-z0-9_-]{11}$/.test(videoId)) return res.status(400).json({ error: "Invalid YouTube video ID" });
      if (!["playing", "paused"].includes(playbackState)) return res.status(400).json({ error: "Invalid playback state" });
      if (!Number.isFinite(currentTime) || currentTime < 0 || currentTime > 86400) return res.status(400).json({ error: "Invalid playback position" });
      const room = await db.query(
        "SELECT owner_id FROM voice_rooms WHERE id=$1 AND status='active' LIMIT 1",
        [roomId]
      );
      if (!room.rows.length) return res.status(404).json({ error: "Room not found" });
      if (String(room.rows[0].owner_id) !== String(req.userId)) {
        return res.status(403).json({ error: "صاحب الغرفة فقط يستطيع التحكم بالمشاهدة" });
      }
      await ensureWatchStateTable();
      const updatedAt = Date.now();
      const result = await db.query(
        `INSERT INTO bak_room_watch_state
          (room_id, video_id, video_title, playback_state, "current_time", updated_at, updated_by)
         VALUES ($1,$2,$3,$4,$5,$6,$7)
         ON CONFLICT (room_id) DO UPDATE SET
          video_id=EXCLUDED.video_id,
          video_title=EXCLUDED.video_title,
          playback_state=EXCLUDED.playback_state,
          current_time=EXCLUDED.current_time,
          updated_at=EXCLUDED.updated_at,
          updated_by=EXCLUDED.updated_by
         RETURNING video_id, video_title, playback_state, "current_time", updated_at, updated_by`,
        [roomId, videoId, title, playbackState, currentTime, updatedAt, String(req.userId)]
      );
      return res.json({ state: result.rows[0] });
    } catch (error) {
      console.error("Update room watch state:", error.message);
      return res.status(500).json({ error: "تعذر تحديث المشاهدة الجماعية" });
    }
  });

};