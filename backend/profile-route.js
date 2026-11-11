const fs = require("fs/promises");
const path = require("path");
const { randomUUID } = require("crypto");

module.exports = function(app, db, authenticateToken) {


  // Level progress is server-owned. Client devices can read progress but cannot
  // grant XP. Trusted backend event handlers can call app.locals.awardMariamXp.
  app.get("/api/levels/me", authenticateToken, async (req, res) => {
    try {
      const result = await db.query(
        "SELECT total_xp FROM user_levels WHERE user_id = $1",
        [req.userId]
      );
      const totalXp = result.rows.length ? Number(result.rows[0].total_xp) : 0;
      let level = 1;
      let currentFloor = 0;
      let nextFloor = 100;
      while (level < 100 && totalXp >= nextFloor) {
        level += 1;
        currentFloor = nextFloor;
        nextFloor = level * level * 100;
      }
      if (level === 1) {
        currentFloor = 0;
        nextFloor = 100;
      }
      if (level === 100) {
        currentFloor = 990000;
        nextFloor = 990000;
      }
      return res.json({
        level,
        total_xp: totalXp,
        current_level_xp: Math.max(0, totalXp - currentFloor),
        next_level_xp: Math.max(0, nextFloor - currentFloor),
        progress: level === 100 ? 1 : Math.min(1, Math.max(0, (totalXp - currentFloor) / (nextFloor - currentFloor)))
      });
    } catch (error) {
      console.error("Load level progress:", error.message);
      return res.status(500).json({ error: "Could not load level progress" });
    }
  });

  // Idempotent server-side award helper. Event handlers must supply a unique
  // event key so retries cannot grant the same experience twice.
  app.locals.awardMariamXp = async (userId, amount, eventKey, reason) => {
    const xp = Math.floor(Number(amount));
    if (!userId || !Number.isSafeInteger(xp) || xp < 1 || xp > 10000 ||
        typeof eventKey !== "string" || !eventKey || eventKey.length > 180) {
      throw new Error("Invalid XP award");
    }
    const client = await db.connect();
    try {
      await client.query("BEGIN");
      const inserted = await client.query(
        `INSERT INTO user_xp_events (user_id, event_key, xp_amount, reason)
         VALUES ($1, $2, $3, $4)
         ON CONFLICT (user_id, event_key) DO NOTHING
         RETURNING xp_amount`,
        [userId, eventKey, xp, String(reason || "activity").slice(0, 60)]
      );
      if (inserted.rows.length) {
        await client.query(
          `INSERT INTO user_levels (user_id, total_xp, updated_at)
           VALUES ($1, $2, NOW())
           ON CONFLICT (user_id)
           DO UPDATE SET total_xp = user_levels.total_xp + EXCLUDED.total_xp,
                         updated_at = NOW()`,
          [userId, xp]
        );
      }
      const total = await client.query(
        "SELECT total_xp FROM user_levels WHERE user_id = $1",
        [userId]
      );
      await client.query("COMMIT");
      return Number(total.rows[0]?.total_xp || 0);
    } catch (error) {
      await client.query("ROLLBACK");
      throw error;
    } finally {
      client.release();
    }
  };

  // Public profile lookup by the internal UUID. The authenticated API returns
  // the separate public account ID used by the app's profile screen.
  app.get("/api/users/:id", async (req, res) => {
    try {
      const result = await db.query(`
        SELECT
          u.id,
          u.username,
          u.created_at,
          p.display_name,
          p.avatar_url,
          p.bio,
          p.country
        FROM users u
        LEFT JOIN user_profiles p ON p.user_id = u.id
        WHERE u.id = $1
          AND u.status = 'active'
      `, [req.params.id]);

      if (!result.rows.length) {
        return res.status(404).json({ error: "User not found" });
      }

      return res.json({ user: result.rows[0] });
    } catch (error) {
      console.error("Public profile error:", error.message);
      return res.status(500).json({ error: "Could not load user profile" });
    }
  });

  // Accept the JPEG created by ProfileActivity, persist it under /uploads,
  // and store its public HTTPS URL against the authenticated account.
  app.post("/api/profile/avatar", authenticateToken, async (req, res) => {
    let savedPath = null;
    try {
      const encoded = req.body && req.body.avatar_base64;
      if (typeof encoded !== "string" || !encoded || encoded.length > 2100000 ||
          !/^[A-Za-z0-9+/]+={0,2}$/.test(encoded)) {
        return res.status(400).json({ error: "Invalid avatar image data" });
      }

      const bytes = Buffer.from(encoded, "base64");
      const maxBytes = 1536 * 1024;
      if (bytes.length < 4 || bytes.length > maxBytes ||
          bytes[0] !== 0xff || bytes[1] !== 0xd8 ||
          bytes[bytes.length - 2] !== 0xff || bytes[bytes.length - 1] !== 0xd9) {
        return res.status(400).json({ error: "Avatar must be a valid JPEG under 1.5 MB" });
      }

      const uploadDir = "/opt/mariam-live/uploads/avatars";
      await fs.mkdir(uploadDir, { recursive: true, mode: 0o750 });

      const filename = `${randomUUID()}.jpg`;
      savedPath = path.join(uploadDir, filename);
      await fs.writeFile(savedPath, bytes, { flag: "wx", mode: 0o640 });

      const avatarUrl = `https://nokiraq.duckdns.org/uploads/avatars/${filename}`;
      const result = await db.query(`
        INSERT INTO user_profiles (user_id, avatar_url, updated_at)
        VALUES ($1, $2, NOW())
        ON CONFLICT (user_id)
        DO UPDATE SET
          avatar_url = EXCLUDED.avatar_url,
          updated_at = NOW()
        RETURNING avatar_url
      `, [req.userId, avatarUrl]);

      return res.status(201).json({
        message: "Avatar uploaded successfully",
        avatar_url: result.rows[0].avatar_url
      });
    } catch (error) {
      if (savedPath) {
        try { await fs.unlink(savedPath); } catch (_) {}
      }
      console.error("Avatar upload error:", error.message);
      return res.status(500).json({ error: "Could not upload avatar" });
    }
  });

  // Update the editable profile fields. An omitted/empty avatar URL must not
  // erase an avatar that was already uploaded successfully.
  app.put("/api/profile", authenticateToken, async (req, res) => {
    try {
      const { display_name, avatar_url, bio, country } = req.body || {};

      if (typeof display_name !== "string" ||
          display_name.trim().length < 1 ||
          display_name.trim().length > 60) {
        return res.status(400).json({
          error: "Display name is required (maximum 60 characters)"
        });
      }

      if (bio != null && (typeof bio !== "string" || bio.length > 300)) {
        return res.status(400).json({ error: "Bio maximum is 300 characters" });
      }

      if (country != null && (typeof country !== "string" || country.length > 80)) {
        return res.status(400).json({ error: "Invalid country" });
      }

      if (avatar_url != null &&
          (typeof avatar_url !== "string" || avatar_url.length > 500 ||
           (avatar_url !== "" && !/^https:\/\//i.test(avatar_url)))) {
        return res.status(400).json({ error: "Invalid avatar URL" });
      }

      const result = await db.query(`
        INSERT INTO user_profiles
          (user_id, display_name, avatar_url, bio, country, updated_at)
        VALUES ($1, $2, $3, $4, $5, NOW())
        ON CONFLICT (user_id)
        DO UPDATE SET
          display_name = EXCLUDED.display_name,
          avatar_url = COALESCE(EXCLUDED.avatar_url, user_profiles.avatar_url),
          bio = EXCLUDED.bio,
          country = EXCLUDED.country,
          updated_at = NOW()
        RETURNING *
      `, [
        req.userId,
        display_name.trim(),
        avatar_url || null,
        bio || null,
        country || null
      ]);

      return res.json({
        message: "Profile updated successfully",
        profile: result.rows[0]
      });
    } catch (error) {
      console.error("Update profile error:", error.message);
      return res.status(500).json({ error: "Could not update profile" });
    }
  });
};
