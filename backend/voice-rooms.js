module.exports = (app, db, authenticateToken) => {

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


  // Create a room with a required, validated image selected by the owner.
  app.post("/api/voice-rooms", authenticateToken, async (req, res) => {
    let savedFile = null;
    let savedBackgroundFile = null;
    try {
      const name = String(req.body.name || "").trim();
      const max_members = Number(req.body.max_members || 8);
      const imageData = String(req.body.image_base64 || "");
      const mimeType = String(req.body.image_mime || "");
      const backgroundData = String(req.body.background_base64 || "");
      if (name.length < 2 || name.length > 100) return res.status(400).json({ error: "Room name must be 2-100 characters" });
      if (!Number.isInteger(max_members) || max_members < 2 || max_members > 8) return res.status(400).json({ error: "Room capacity must be 2-8" });
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
        const result = await client.query(
          `INSERT INTO voice_rooms (owner_id, name, max_members, status, image_url, room_background_url)
           VALUES ($1, $2, $3, 'active', $4, $5) RETURNING *`,
          [req.userId, name, max_members, imageUrl, backgroundUrl]
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

  // Change or remove the room background while the room is live (owner only).
  app.post("/api/voice-rooms/:roomId/background", authenticateToken, async (req, res) => {
    let savedPath = null;
    try {
      const encoded = String(req.body && req.body.background_base64 || "");
      if (!encoded || encoded.length > 6 * 1024 * 1024) {
        return res.status(413).json({ error: "Room background must be 4 MB or smaller" });
      }
      const buffer = Buffer.from(encoded, "base64");
      const validJpeg = buffer.length >= 3 && buffer[0] === 0xff && buffer[1] === 0xd8 && buffer[2] === 0xff;
      if (!validJpeg || buffer.length > 4 * 1024 * 1024) {
        return res.status(400).json({ error: "Choose a valid JPG background image" });
      }
      const fs = require("fs");
      const path = require("path");
      const crypto = require("crypto");
      const directory = "/opt/mariam-live/uploads/rooms";
      fs.mkdirSync(directory, { recursive: true });
      const filename = crypto.randomUUID() + "-background.jpg";
      savedPath = path.join(directory, filename);
      fs.writeFileSync(savedPath, buffer, { flag: "wx", mode: 0o640 });
      const backgroundUrl = "https://nokiraq.duckdns.org/uploads/rooms/" + filename;
      const result = await db.query(
        "UPDATE voice_rooms SET room_background_url = $1 WHERE id = $2 AND owner_id = $3 AND status = 'active' RETURNING id",
        [backgroundUrl, req.params.roomId, req.userId]
      );
      if (!result.rows.length) {
        try { fs.unlinkSync(savedPath); } catch (_) {}
        savedPath = null;
        return res.status(404).json({ error: "Room not found or you are not the owner" });
      }
      return res.json({ message: "Room background updated", backgroundUrl });
    } catch (error) {
      if (savedPath) { try { require("fs").unlinkSync(savedPath); } catch (_) {} }
      console.error("Update room background:", error.message);
      return res.status(500).json({ error: "Could not update room background" });
    }
  });

  app.delete("/api/voice-rooms/:roomId/background", authenticateToken, async (req, res) => {
    try {
      const result = await db.query(
        "UPDATE voice_rooms SET room_background_url = NULL WHERE id = $1 AND owner_id = $2 AND status = 'active' RETURNING id",
        [req.params.roomId, req.userId]
      );
      if (!result.rows.length) {
        return res.status(404).json({ error: "Room not found or you are not the owner" });
      }
      return res.json({ message: "Room background removed" });
    } catch (error) {
      console.error("Remove room background:", error.message);
      return res.status(500).json({ error: "Could not remove room background" });
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
      const result = await db.query(
        `DELETE FROM room_members
         WHERE room_id = $1 AND user_id = $2
         RETURNING room_id`,
        [req.params.roomId, req.userId]
      );

      if (!result.rows.length) {
        return res.status(404).json({ error: "Membership not found" });
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
      const result = await db.query(
        `UPDATE voice_rooms
         SET status = 'closed'
         WHERE id = $1 AND owner_id = $2
         RETURNING id`,
        [req.params.roomId, req.userId]
      );

      if (!result.rows.length) {
        return res.status(404).json({
          error: "Room not found or you are not the owner"
        });
      }

      res.json({ message: "Room closed successfully" });

    } catch (error) {
      console.error("Close room:", error.message);
      res.status(500).json({ error: "Could not close room" });
    }
  });

};