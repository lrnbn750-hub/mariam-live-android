const { AccessToken } = require("livekit-server-sdk");

module.exports = function registerLiveKitRoutes(app, db, authenticateToken) {
  const liveKitUrl = process.env.LIVEKIT_URL || "wss://voicenokiraq.duckdns.org";
  const apiKey = process.env.LIVEKIT_API_KEY;
  const apiSecret = process.env.LIVEKIT_API_SECRET;

  app.post("/api/voice-rooms/:roomId/token", authenticateToken, async (req, res) => {
    const roomId = String(req.params.roomId || "");
    if (!/^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(roomId)) {
      return res.status(400).json({ error: "Invalid room ID" });
    }

    if (!apiKey || !apiSecret) {
      return res.status(503).json({ error: "LiveKit credentials are not configured" });
    }

    try {
      const membership = await db.query(
        `SELECT r.id
         FROM voice_rooms r
         JOIN room_members m ON m.room_id = r.id
         WHERE r.id = $1
           AND r.status = 'active'
           AND m.user_id = $2
         LIMIT 1`,
        [roomId, req.userId]
      );

      if (!membership.rows.length) {
        return res.status(403).json({ error: "Join the room before requesting an audio token" });
      }

      const token = new AccessToken(apiKey, apiSecret, {
        identity: String(req.userId),
        ttl: "10m"
      });
      token.addGrant({
        roomJoin: true,
        room: roomId,
        canPublish: true,
        canSubscribe: true,
        canPublishData: true
      });

      const participantToken = await token.toJwt();
      return res.json({ serverUrl: liveKitUrl, participantToken });
    } catch (error) {
      console.error("LiveKit token error:", error.message);
      return res.status(500).json({ error: "Could not create audio token" });
    }
  });
};
