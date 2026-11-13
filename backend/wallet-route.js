const crypto = require("crypto");

const PACKAGES = [
  { id: "iqd5000", amount_iqd: 5000, diamonds: 500, bonus: 0 },
  { id: "iqd10000", amount_iqd: 10000, diamonds: 1050, bonus: 50 },
  { id: "iqd25000", amount_iqd: 25000, diamonds: 2700, bonus: 200 },
  { id: "iqd50000", amount_iqd: 50000, diamonds: 5600, bonus: 600 },
  { id: "iqd100000", amount_iqd: 100000, diamonds: 11500, bonus: 1500 }
];
const METHODS = ["zain_cash", "asia_hawala", "usdt"];

module.exports = function registerWalletRoutes(app, db, authenticateToken) {
  app.get("/api/wallet", authenticateToken, async (req, res) => {
    try {
      await db.query(
        "INSERT INTO wallet_accounts(user_id) VALUES($1) ON CONFLICT(user_id) DO NOTHING",
        [req.userId]
      );
      const result = await db.query(
        "SELECT diamonds_balance, earnings_iqd, updated_at FROM wallet_accounts WHERE user_id=$1",
        [req.userId]
      );
      res.json({
        wallet: result.rows[0],
        packages: PACKAGES,
        payment_methods: [
          { id: "zain_cash", name: "زين كاش", kind: "manual" },
          { id: "asia_hawala", name: "آسيا حوالة", kind: "manual" },
          { id: "usdt", name: "USDT", kind: "manual" },
          { id: "gateway", name: "دفع إلكتروني", kind: "gateway", enabled: Boolean(process.env.PAYMENT_GATEWAY_URL) }
        ],
        cashout_min_iqd: 25000
      });
    } catch (e) {
      console.error("Wallet load:", e.message);
      res.status(500).json({ error: "تعذر تحميل المحفظة" });
    }
  });

  app.get("/api/wallet/transactions", authenticateToken, async (req, res) => {
    try {
      const limit = Math.min(100, Math.max(1, Number(req.query.limit) || 50));
      const result = await db.query(
        `SELECT id, transaction_type, currency, amount, status, description, created_at
         FROM wallet_transactions WHERE user_id=$1
         ORDER BY created_at DESC, id DESC LIMIT $2`,
        [req.userId, limit]
      );
      res.json({ transactions: result.rows });
    } catch (e) {
      console.error("Wallet transactions:", e.message);
      res.status(500).json({ error: "تعذر تحميل سجل العمليات" });
    }
  });

  app.post("/api/wallet/topups", authenticateToken, async (req, res) => {
    const packageId = String(req.body.package_id || "");
    const method = String(req.body.payment_method || "");
    const reference = String(req.body.payment_reference || "").trim().slice(0, 120);
    const pack = PACKAGES.find(p => p.id === packageId);
    if (!pack) return res.status(400).json({ error: "باقة الشحن غير صالحة" });
    if (method === "gateway") {
      if (!process.env.PAYMENT_GATEWAY_URL || !process.env.PAYMENT_GATEWAY_SECRET) {
        return res.status(503).json({ error: "الدفع الإلكتروني التلقائي غير مفعّل بعد. استخدم التحويل اليدوي حالياً." });
      }
      return res.status(503).json({ error: "بوابة الدفع تحتاج ربط حساب التاجر والتحقق من إشعار الدفع قبل تفعيلها." });
    }
    if (!METHODS.includes(method)) return res.status(400).json({ error: "طريقة الدفع غير مدعومة" });
    if (reference.length < 4) return res.status(400).json({ error: "أدخل رقم الحوالة أو مرجع التحويل" });
    try {
      const result = await db.query(
        `INSERT INTO wallet_topup_requests
          (user_id, package_id, amount_iqd, diamonds, payment_method, payment_reference)
         VALUES ($1,$2,$3,$4,$5,$6)
         RETURNING id, amount_iqd, diamonds, payment_method, status, created_at`,
        [req.userId, pack.id, pack.amount_iqd, pack.diamonds, method, reference]
      );
      res.status(201).json({ request: result.rows[0], message: "تم استلام طلب الشحن، وسيضاف الرصيد بعد مراجعة التحويل." });
    } catch (e) {
      console.error("Create topup:", e.message);
      res.status(500).json({ error: "تعذر إرسال طلب الشحن" });
    }
  });

  app.post("/api/wallet/cashouts", authenticateToken, async (req, res) => {
    const amount = Math.floor(Number(req.body.amount_iqd));
    const method = String(req.body.payout_method || "");
    const account = String(req.body.payout_account || "").trim().slice(0, 120);
    if (!Number.isSafeInteger(amount) || amount < 25000) return res.status(400).json({ error: "الحد الأدنى للسحب 25,000 دينار" });
    if (!["zain_cash", "asia_hawala", "bank"].includes(method) || account.length < 5) {
      return res.status(400).json({ error: "بيانات السحب غير مكتملة" });
    }
    const client = await db.connect();
    try {
      await client.query("BEGIN");
      const w = await client.query("SELECT earnings_iqd FROM wallet_accounts WHERE user_id=$1 FOR UPDATE", [req.userId]);
      const balance = Number(w.rows[0]?.earnings_iqd || 0);
      if (balance < amount) {
        await client.query("ROLLBACK");
        return res.status(400).json({ error: "رصيد الأرباح غير كافٍ" });
      }
      await client.query("UPDATE wallet_accounts SET earnings_iqd=earnings_iqd-$2, updated_at=NOW() WHERE user_id=$1", [req.userId, amount]);
      const created = await client.query(
        `INSERT INTO wallet_cashout_requests(user_id,amount_iqd,payout_method,payout_account)
         VALUES($1,$2,$3,$4) RETURNING id,status,amount_iqd,created_at`,
        [req.userId, amount, method, account]
      );
      await client.query(
        `INSERT INTO wallet_transactions(user_id,transaction_type,currency,amount,status,description,reference_id)
         VALUES($1,'cashout','iqd',$2,'pending','طلب سحب أرباح',$3)`,
        [req.userId, amount, String(created.rows[0].id)]
      );
      await client.query("COMMIT");
      res.status(201).json({ request: created.rows[0], message: "تم تسجيل طلب السحب للمراجعة" });
    } catch (e) {
      await client.query("ROLLBACK").catch(() => {});
      console.error("Wallet cashout:", e.message);
      res.status(500).json({ error: "تعذر تسجيل طلب السحب" });
    } finally {
      client.release();
    }
  });

  app.post("/api/admin/wallet/cashouts/:id/:decision", authenticateToken, async (req, res) => {
    const admins = String(process.env.WALLET_ADMIN_USER_IDS || "").split(",").map(v => v.trim()).filter(Boolean);
    if (!admins.includes(String(req.userId))) return res.status(403).json({ error: "غير مصرح" });
    const decision = String(req.params.decision);
    if (!["approve", "reject"].includes(decision)) return res.status(400).json({ error: "قرار غير صالح" });
    const client = await db.connect();
    try {
      await client.query("BEGIN");
      const q = await client.query("SELECT * FROM wallet_cashout_requests WHERE id=$1 FOR UPDATE", [req.params.id]);
      if (!q.rows.length || q.rows[0].status !== "pending") {
        await client.query("ROLLBACK");
        return res.status(409).json({ error: "الطلب غير موجود أو تمت معالجته" });
      }
      const request = q.rows[0];
      if (decision === "reject") {
        await client.query("UPDATE wallet_accounts SET earnings_iqd=earnings_iqd+$2,updated_at=NOW() WHERE user_id=$1", [request.user_id,request.amount_iqd]);
        await client.query("UPDATE wallet_cashout_requests SET status='rejected',reviewed_by=$2,reviewed_at=NOW() WHERE id=$1", [request.id,req.userId]);
        await client.query("UPDATE wallet_transactions SET status='rejected',description='تم رفض طلب السحب وإعادة الرصيد' WHERE reference_id=$1 AND transaction_type='cashout'", [String(request.id)]);
      } else {
        await client.query("UPDATE wallet_cashout_requests SET status='approved',reviewed_by=$2,reviewed_at=NOW() WHERE id=$1", [request.id,req.userId]);
        await client.query("UPDATE wallet_transactions SET status='completed' WHERE reference_id=$1 AND transaction_type='cashout'", [String(request.id)]);
      }
      await client.query("COMMIT");
      res.json({ processed: true, status: decision === "approve" ? "approved" : "rejected" });
    } catch (e) {
      await client.query("ROLLBACK").catch(() => {});
      console.error("Review wallet cashout:", e.message);
      res.status(500).json({ error: "تعذر معالجة طلب السحب" });
    } finally {
      client.release();
    }
  });

  // Admin-only approval: set WALLET_ADMIN_USER_IDS to comma-separated internal UUIDs.
  app.post("/api/admin/wallet/topups/:id/approve", authenticateToken, async (req, res) => {
    const admins = String(process.env.WALLET_ADMIN_USER_IDS || "").split(",").map(v => v.trim()).filter(Boolean);
    if (!admins.includes(String(req.userId))) return res.status(403).json({ error: "غير مصرح" });
    const client = await db.connect();
    try {
      await client.query("BEGIN");
      const q = await client.query("SELECT * FROM wallet_topup_requests WHERE id=$1 FOR UPDATE", [req.params.id]);
      if (!q.rows.length || q.rows[0].status !== "pending") {
        await client.query("ROLLBACK");
        return res.status(409).json({ error: "الطلب غير موجود أو تمت معالجته" });
      }
      const t = q.rows[0];
      await client.query("INSERT INTO wallet_accounts(user_id) VALUES($1) ON CONFLICT(user_id) DO NOTHING", [t.user_id]);
      await client.query("UPDATE wallet_accounts SET diamonds_balance=diamonds_balance+$2,updated_at=NOW() WHERE user_id=$1", [t.user_id,t.diamonds]);
      await client.query("UPDATE wallet_topup_requests SET status='approved',reviewed_by=$2,reviewed_at=NOW() WHERE id=$1", [t.id,req.userId]);
      await client.query(
        `INSERT INTO wallet_transactions(user_id,transaction_type,currency,amount,status,description,reference_id)
         VALUES($1,'topup','diamonds',$2,'completed','شحن رصيد ماسات',$3)`,
        [t.user_id,t.diamonds,String(t.id)]
      );
      await client.query("COMMIT");
      res.json({ approved: true, diamonds_added: Number(t.diamonds) });
    } catch (e) {
      await client.query("ROLLBACK").catch(() => {});
      console.error("Approve wallet topup:", e.message);
      res.status(500).json({ error: "تعذر اعتماد الشحن" });
    } finally {
      client.release();
    }
  });
};
