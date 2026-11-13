const crypto = require("crypto");

module.exports = function registerGiftRoutes(app, db, authenticateToken) {
  const validId = value => /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(String(value || ""));

  app.get("/api/gifts", authenticateToken, async (_req, res) => {
    try {
      const result = await db.query(
        "SELECT gift_id, name, animation_url, price_coins, gift_units FROM room_gifts WHERE active=TRUE ORDER BY sort_order, price_coins"
      );
      return res.json({ gifts: result.rows });
    } catch (error) {
      console.error("Gift catalog:", error.message);
      return res.status(500).json({ error: "تعذر تحميل الهدايا" });
    }
  });

  // Cursor-based room feed. Clients poll with after_id; every event is committed before it is visible.
  app.get("/api/voice-rooms/:roomId/gifts", authenticateToken, async (req, res) => {
    const roomId = req.params.roomId;
    const afterId = Math.max(0, Number(req.query.after_id) || 0);
    if (!validId(roomId)) return res.status(400).json({ error: "معرّف الغرفة غير صالح" });
    try {
      const member = await db.query(
        "SELECT 1 FROM room_members WHERE room_id=$1 AND user_id=$2 UNION SELECT 1 FROM voice_room_seats WHERE room_id=$1 AND user_id=$2",
        [roomId, req.userId]
      );
      if (!member.rows.length) return res.status(403).json({ error: "يجب الانضمام إلى الغرفة أولاً" });
      const result = await db.query(`
        SELECT e.id, e.room_id, e.sender_id, e.recipient_id, e.gift_id, e.quantity,
               e.total_coins, e.created_at, g.name, g.animation_url,
               COALESCE(sp.display_name, su.username) AS sender_name,
               COALESCE(rp.display_name, ru.username) AS recipient_name
        FROM voice_room_gift_events e
        JOIN room_gifts g ON g.gift_id=e.gift_id
        JOIN users su ON su.id=e.sender_id
        JOIN users ru ON ru.id=e.recipient_id
        LEFT JOIN user_profiles sp ON sp.user_id=su.id
        LEFT JOIN user_profiles rp ON rp.user_id=ru.id
        WHERE e.room_id=$1 AND e.id>$2
        ORDER BY e.id ASC LIMIT 30
      `, [roomId, afterId]);
      return res.json({ events: result.rows });
    } catch (error) {
      console.error("Gift feed:", error.message);
      return res.status(500).json({ error: "تعذر تحميل هدايا الغرفة" });
    }
  });

  app.post("/api/voice-rooms/:roomId/gifts", authenticateToken, async (req, res) => {
    const roomId = req.params.roomId;
    const recipientId = String(req.body && req.body.recipient_id || "");
    const giftId = String(req.body && req.body.gift_id || "");
    const quantity = Number(req.body && req.body.quantity);
    const requestKey = String(req.body && req.body.request_id || "");
    if (!validId(roomId) || !validId(recipientId)) return res.status(400).json({ error: "بيانات الغرفة أو المستلم غير صالحة" });
    if (!/^[a-zA-Z0-9_-]{8,100}$/.test(requestKey)) return res.status(400).json({ error: "معرّف الطلب غير صالح" });
    if (!Number.isInteger(quantity) || quantity < 1 || quantity > 99) return res.status(400).json({ error: "الكمية يجب أن تكون بين 1 و99" });
    if (!/^[a-z0-9_-]{2,64}$/.test(giftId)) return res.status(400).json({ error: "الهدية غير صالحة" });

    const client = await db.connect();
    try {
      await client.query("BEGIN");
      const prior = await client.query("SELECT id FROM voice_room_gift_events WHERE event_key=$1", ["gift:" + String(req.userId) + ":" + requestKey]);
      if (prior.rows.length) {
        await client.query("ROLLBACK");
        return res.status(409).json({ error: "تمت معالجة هذا الطلب مسبقاً" });
      }
      const room = await client.query("SELECT id FROM voice_rooms WHERE id=$1 AND status='active' FOR UPDATE", [roomId]);
      if (!room.rows.length) { await client.query("ROLLBACK"); return res.status(404).json({ error: "الغرفة غير نشطة" }); }
      const members = await client.query(
        "SELECT user_id FROM room_members WHERE room_id=$1 AND user_id = ANY($2::uuid[]) UNION SELECT user_id FROM voice_room_seats WHERE room_id=$1 AND user_id = ANY($2::uuid[])",
        [roomId, [req.userId, recipientId]]
      );
      const memberIds = new Set(members.rows.map(row => String(row.user_id)));
      if (!memberIds.has(String(req.userId)) || !memberIds.has(String(recipientId))) {
        await client.query("ROLLBACK");
        return res.status(403).json({ error: "المرسل والمستلم يجب أن يكونا داخل الغرفة" });
      }
      if (String(req.userId) === recipientId) { await client.query("ROLLBACK"); return res.status(400).json({ error: "لا يمكن إرسال هدية لنفسك" }); }

      const giftResult = await client.query(
        "SELECT gift_id,name,price_coins,gift_units FROM room_gifts WHERE gift_id=$1 AND active=TRUE FOR SHARE",
        [giftId]
      );
      if (!giftResult.rows.length) { await client.query("ROLLBACK"); return res.status(404).json({ error: "الهدية غير متاحة" }); }
      const gift = giftResult.rows[0];
      const settings = await client.query(
        "SELECT setting_key,setting_value FROM wallet_settings WHERE setting_key IN ('gift_earnings','platform_share')"
      );
      const config = Object.fromEntries(settings.rows.map(row => [row.setting_key, row.setting_value]));
      const unitsPerGift = Number(config.gift_earnings && config.gift_earnings.diamonds_per_gift_unit);
      const usdCentsPerUnit = Number(config.gift_earnings && config.gift_earnings.usd_cents_per_gift_unit);
      const creatorPercent = Number(config.platform_share && config.platform_share.creator_percent);
      const platformPercent = Number(config.platform_share && config.platform_share.platform_percent);
      if (![unitsPerGift, usdCentsPerUnit, creatorPercent, platformPercent].every(Number.isFinite) ||
          unitsPerGift <= 0 || usdCentsPerUnit <= 0 || creatorPercent < 0 || platformPercent < 0 ||
          creatorPercent + platformPercent !== 100) {
        throw new Error("Invalid gift earnings settings");
      }
      const totalCoins = BigInt(gift.price_coins) * BigInt(quantity);
      const units = BigInt(gift.gift_units) * BigInt(quantity);
      const diamonds = units * BigInt(unitsPerGift);
      const totalEarningsCents = units * BigInt(usdCentsPerUnit);
      const creatorCents = totalEarningsCents * BigInt(creatorPercent) / 100n;
      const platformCents = totalEarningsCents - creatorCents;
      if (totalCoins > BigInt(Number.MAX_SAFE_INTEGER) || diamonds > BigInt(Number.MAX_SAFE_INTEGER) ||
          creatorCents > BigInt(Number.MAX_SAFE_INTEGER)) throw new Error("Gift amount overflow");

      await client.query(
        "INSERT INTO wallet_accounts(user_id) VALUES($1),($2) ON CONFLICT(user_id) DO NOTHING",
        [req.userId, recipientId]
      );
      const accounts = await client.query(
        "SELECT user_id,recharge_coins FROM wallet_accounts WHERE user_id=ANY($1::uuid[]) ORDER BY user_id FOR UPDATE",
        [[req.userId, recipientId]]
      );
      const sender = accounts.rows.find(row => String(row.user_id) === String(req.userId));
      if (!sender || BigInt(sender.recharge_coins) < totalCoins) {
        await client.query("ROLLBACK");
        return res.status(400).json({ error: "رصيد العملات غير كافٍ" });
      }
      await client.query(
        "UPDATE wallet_accounts SET recharge_coins=recharge_coins-$2,updated_at=NOW() WHERE user_id=$1",
        [req.userId, totalCoins.toString()]
      );
      await client.query(
        `UPDATE wallet_accounts SET earned_diamonds=earned_diamonds+$2,
           withdrawable_usd_cents=withdrawable_usd_cents+$3,
           lifetime_earned_usd_cents=lifetime_earned_usd_cents+$3,updated_at=NOW()
         WHERE user_id=$1`,
        [recipientId, diamonds.toString(), creatorCents.toString()]
      );
      const eventKey = "gift:" + String(req.userId) + ":" + requestKey;
      const event = await client.query(
        `INSERT INTO voice_room_gift_events
          (room_id,sender_id,recipient_id,gift_id,quantity,total_coins,diamonds_awarded,creator_earnings_usd_cents,platform_share_usd_cents,event_key)
         VALUES($1,$2,$3,$4,$5,$6,$7,$8,$9,$10)
         RETURNING id,room_id,sender_id,recipient_id,gift_id,quantity,total_coins,created_at`,
        [roomId, req.userId, recipientId, giftId, quantity, totalCoins.toString(), diamonds.toString(), creatorCents.toString(), platformCents.toString(), eventKey]
      );
      await client.query(
        `INSERT INTO wallet_transactions(user_id,transaction_type,amount,currency,status,event_key,related_user_id,metadata)
         VALUES($1,'gift_sent',$2,'COIN','completed',$3,$4,$5)`,
        [req.userId,totalCoins.toString(),eventKey+":sender",recipientId,JSON.stringify({room_id:roomId,gift_id:giftId,quantity})]
      );
      await client.query(
        `INSERT INTO wallet_transactions(user_id,transaction_type,amount,currency,status,event_key,related_user_id,metadata)
         VALUES($1,'gift_received',$2,'DIAMOND','completed',$3,$4,$5)`,
        [recipientId,diamonds.toString(),eventKey+":recipient",req.userId,JSON.stringify({room_id:roomId,gift_id:giftId,quantity,creator_earnings_usd_cents:creatorCents.toString(),platform_share_usd_cents:platformCents.toString()})]
      );
      await client.query("COMMIT");
      return res.status(201).json({ message: "تم إرسال الهدية", event: event.rows[0], total_coins: totalCoins.toString(), diamonds_awarded: diamonds.toString() });
    } catch (error) {
      await client.query("ROLLBACK").catch(() => {});
      if (error.code === "23505") return res.status(409).json({ error: "تمت معالجة هذا الطلب مسبقاً" });
      console.error("Send gift:", error.message);
      return res.status(500).json({ error: "تعذر إرسال الهدية" });
    } finally { client.release(); }
  });
};
