module.exports = function registerSvipRoutes(app, db, authenticateToken) {
  // SVIP pricing is editable only by explicitly allowlisted administrator UUIDs.
  const requireSvipAdmin = (req, res, next) => {
    const allowed = String(process.env.SVIP_ADMIN_USER_IDS || "")
      .split(",").map(value => value.trim()).filter(Boolean);
    if (!allowed.includes(String(req.userId || ""))) {
      return res.status(403).json({ success: false, error: "غير مصرح بإدارة أسعار SVIP" });
    }
    next();
  };

  app.get("/api/admin/svip/packages", authenticateToken, requireSvipAdmin, async (_req, res) => {
    try {
      const result = await db.query(
        `SELECT level, name, price_diamonds, duration_days, active, updated_at
         FROM public.svip_packages ORDER BY level ASC`
      );
      res.json({ success: true, packages: result.rows });
    } catch (error) {
      console.error("Admin SVIP packages:", error.message);
      res.status(500).json({ success: false, error: "تعذر تحميل أسعار SVIP" });
    }
  });

  app.put("/api/admin/svip/packages/:level", authenticateToken, requireSvipAdmin, async (req, res) => {
    const level = Number(req.params.level);
    const price = Number(req.body.price_diamonds);
    const active = req.body.active === true;
    if (!Number.isInteger(level) || level < 1 || level > 7 ||
        !Number.isSafeInteger(price) || price < 0 || (active && price <= 0)) {
      return res.status(400).json({ success: false, error: "السعر أو المستوى غير صالح. يجب أن يكون السعر أكبر من صفر لتفعيل الباقة." });
    }
    try {
      const result = await db.query(
        `UPDATE public.svip_packages
         SET price_diamonds=$2, active=$3, updated_at=NOW()
         WHERE level=$1
         RETURNING level, name, price_diamonds, duration_days, active, updated_at`,
        [level, price, active]
      );
      if (!result.rows.length) return res.status(404).json({ success: false, error: "المستوى غير موجود" });
      res.json({ success: true, package: result.rows[0] });
    } catch (error) {
      console.error("Update SVIP package:", error.message);
      res.status(500).json({ success: false, error: "تعذر حفظ إعدادات SVIP" });
    }
  });

  app.get("/api/svip/levels", authenticateToken, async (_req, res) => {
    try {
      const result = await db.query(
        `SELECT level, name, price_diamonds AS price_coins,
                duration_days, active
         FROM public.svip_packages
         ORDER BY level ASC`
      );
      res.json({ success: true, levels: result.rows });
    } catch (error) {
      console.error("SVIP levels:", error.message);
      res.status(500).json({ success: false, error: "تعذر تحميل مستويات SVIP" });
    }
  });

  app.get("/api/svip/me", authenticateToken, async (req, res) => {
    try {
      const result = await db.query(
        `SELECT level, starts_at, expires_at,
                (expires_at > NOW()) AS active
         FROM public.user_svip_memberships
         WHERE user_id = $1 AND expires_at > NOW()`,
        [req.userId]
      );
      res.json({ success: true, subscription: result.rows[0] || null });
    } catch (error) {
      console.error("SVIP membership:", error.message);
      res.status(500).json({ success: false, error: "تعذر تحميل عضوية SVIP" });
    }
  });

  app.post("/api/svip/purchase", authenticateToken, async (req, res) => {
    const level = Number(req.body.level);
    if (!Number.isInteger(level) || level < 1 || level > 7) {
      return res.status(400).json({ success: false, error: "مستوى SVIP غير صالح" });
    }

    const client = await db.connect();
    try {
      await client.query("BEGIN");

      const planResult = await client.query(
        `SELECT level, name, price_diamonds, duration_days
         FROM public.svip_packages
         WHERE level = $1 AND active = TRUE
         FOR SHARE`,
        [level]
      );
      if (!planResult.rows.length || Number(planResult.rows[0].price_diamonds) <= 0) {
        await client.query("ROLLBACK");
        return res.status(409).json({ success: false, error: "هذه الباقة غير متاحة حالياً" });
      }

      const plan = planResult.rows[0];
      await client.query(
        "INSERT INTO public.wallet_accounts(user_id) VALUES($1) ON CONFLICT(user_id) DO NOTHING",
        [req.userId]
      );
      const walletResult = await client.query(
        "SELECT diamonds_balance FROM public.wallet_accounts WHERE user_id=$1 FOR UPDATE",
        [req.userId]
      );
      const balance = Number(walletResult.rows[0]?.diamonds_balance || 0);
      const price = Number(plan.price_diamonds);
      if (!Number.isSafeInteger(price) || balance < price) {
        await client.query("ROLLBACK");
        return res.status(400).json({ success: false, error: "رصيد الماسات غير كافٍ" });
      }

      await client.query(
        "UPDATE public.wallet_accounts SET diamonds_balance=diamonds_balance-$2, updated_at=NOW() WHERE user_id=$1",
        [req.userId, price]
      );
      const membership = await client.query(
        `INSERT INTO public.user_svip_memberships(user_id,level,starts_at,expires_at,updated_at)
         VALUES($1,$2,NOW(),NOW()+($3 * INTERVAL '1 day'),NOW())
         ON CONFLICT(user_id) DO UPDATE SET
           level=EXCLUDED.level,
           starts_at=NOW(),
           expires_at=NOW()+(EXCLUDED.expires_at-EXCLUDED.starts_at),
           updated_at=NOW()
         RETURNING level, starts_at, expires_at`,
        [req.userId, level, Number(plan.duration_days)]
      );
      await client.query(
        `INSERT INTO public.wallet_transactions
          (user_id,transaction_type,currency,amount,status,description,reference_id)
         VALUES($1,'adjustment','diamonds',$2,'completed',$3,$4)`,
        [req.userId, price, "شراء " + plan.name + " لمدة 30 يوماً", "svip:" + level + ":" + Date.now()]
      );

      const updatedWallet = await client.query(
        "SELECT diamonds_balance FROM public.wallet_accounts WHERE user_id=$1",
        [req.userId]
      );
      await client.query("COMMIT");
      res.json({
        success: true,
        message: "تم تفعيل " + plan.name + " بنجاح",
        subscription: membership.rows[0],
        wallet: { diamonds_balance: Number(updatedWallet.rows[0].diamonds_balance) }
      });
    } catch (error) {
      await client.query("ROLLBACK").catch(() => {});
      console.error("SVIP purchase:", error.message);
      res.status(500).json({ success: false, error: "تعذر إتمام شراء SVIP" });
    } finally {
      client.release();
    }
  });
};
