module.exports = function registerAgencyMembershipRoutes(app, db, authenticateToken) {
  app.post("/api/agency/invitations", authenticateToken, async (req, res) => {
    const publicId = String(req.body?.public_id || "").trim();
    if (!publicId || publicId.length > 40) return res.status(400).json({ error: "أدخل ID صحيح" });
    const client = await db.connect();
    try {
      await client.query("BEGIN");
      const agency = await client.query("SELECT id,agency_name FROM agency_applications WHERE user_id=$1 AND status='approved' ORDER BY created_at DESC LIMIT 1 FOR UPDATE", [req.userId]);
      if (!agency.rows.length) { await client.query("ROLLBACK"); return res.status(403).json({ error: "لا توجد وكالة معتمدة لهذا الحساب" }); }
      const creator = await client.query("SELECT id,public_id FROM users WHERE public_id=$1 AND status='active' LIMIT 1", [publicId]);
      if (!creator.rows.length) { await client.query("ROLLBACK"); return res.status(404).json({ error: "المستخدم غير موجود" }); }
      const creatorId = creator.rows[0].id;
      if (String(creatorId) === String(req.userId)) { await client.query("ROLLBACK"); return res.status(400).json({ error: "لا يمكنك دعوة نفسك" }); }
      const existingMember = await client.query("SELECT 1 FROM agency_members WHERE creator_user_id=$1", [creatorId]);
      if (existingMember.rows.length) { await client.query("ROLLBACK"); return res.status(409).json({ error: "هذا المستخدم مرتبط بوكالة بالفعل" }); }
      const pending = await client.query("SELECT id FROM agency_invitations WHERE creator_user_id=$1 AND status='pending'", [creatorId]);
      if (pending.rows.length) { await client.query("ROLLBACK"); return res.status(409).json({ error: "لدى المستخدم دعوة قيد الانتظار بالفعل" }); }
      const inserted = await client.query("INSERT INTO agency_invitations(agency_application_id,creator_user_id,invited_by) VALUES($1,$2,$3) RETURNING id,status,created_at", [agency.rows[0].id, creatorId, req.userId]);
      await client.query("COMMIT");
      return res.status(201).json({ invitation: inserted.rows[0], creator_public_id: creator.rows[0].public_id });
    } catch (error) {
      await client.query("ROLLBACK");
      console.error("Agency invitation:", error.message);
      return res.status(500).json({ error: "تعذر إرسال الدعوة" });
    } finally { client.release(); }
  });

  app.get("/api/agency/invitations/incoming", authenticateToken, async (req, res) => {
    try {
      const rows = await db.query(
        "SELECT i.id,i.status,i.created_at,a.agency_name,a.id AS agency_application_id FROM agency_invitations i JOIN agency_applications a ON a.id=i.agency_application_id WHERE i.creator_user_id=$1 AND i.status='pending' ORDER BY i.created_at DESC LIMIT 100",
        [req.userId]
      );
      return res.json({ invitations: rows.rows });
    } catch (error) {
      console.error("Agency incoming invitations:", error.message);
      return res.status(500).json({ error: "تعذر تحميل دعوات الوكالة" });
    }
  });

  app.get("/api/agency/invitations", authenticateToken, async (req, res) => {
    try {
      const agency = await db.query("SELECT id FROM agency_applications WHERE user_id=$1 AND status='approved' ORDER BY created_at DESC LIMIT 1", [req.userId]);
      if (!agency.rows.length) return res.status(403).json({ error: "لا توجد وكالة معتمدة" });
      const rows = await db.query("SELECT i.id,i.status,i.created_at,u.public_id,p.display_name,p.avatar_url FROM agency_invitations i JOIN users u ON u.id=i.creator_user_id LEFT JOIN user_profiles p ON p.user_id=u.id WHERE i.agency_application_id=$1 ORDER BY i.created_at DESC LIMIT 200", [agency.rows[0].id]);
      return res.json({ invitations: rows.rows });
    } catch (error) {
      console.error("Agency invitations list:", error.message);
      return res.status(500).json({ error: "تعذر تحميل الدعوات" });
    }
  });

  app.get("/api/agency/members", authenticateToken, async (req, res) => {
    try {
      const agency = await db.query("SELECT id,agency_name FROM agency_applications WHERE user_id=$1 AND status='approved' ORDER BY created_at DESC LIMIT 1", [req.userId]);
      if (!agency.rows.length) return res.status(403).json({ error: "لا توجد وكالة معتمدة" });
      const rows = await db.query("SELECT m.joined_at,u.public_id,p.display_name,p.avatar_url FROM agency_members m JOIN users u ON u.id=m.creator_user_id LEFT JOIN user_profiles p ON p.user_id=u.id WHERE m.agency_application_id=$1 ORDER BY m.joined_at DESC LIMIT 500", [agency.rows[0].id]);
      return res.json({ agency: agency.rows[0], members: rows.rows });
    } catch (error) {
      console.error("Agency members:", error.message);
      return res.status(500).json({ error: "تعذر تحميل أعضاء الوكالة" });
    }
  });

  app.post("/api/agency/invitations/:id/respond", authenticateToken, async (req, res) => {
    const invitationId = Number(req.params.id);
    const action = String(req.body?.action || "");
    if (!Number.isSafeInteger(invitationId) || invitationId < 1 || !["accept","reject"].includes(action)) return res.status(400).json({ error: "طلب غير صالح" });
    const client = await db.connect();
    try {
      await client.query("BEGIN");
      const invite = await client.query("SELECT * FROM agency_invitations WHERE id=$1 AND creator_user_id=$2 AND status='pending' FOR UPDATE", [invitationId, req.userId]);
      if (!invite.rows.length) { await client.query("ROLLBACK"); return res.status(404).json({ error: "الدعوة غير موجودة أو انتهت" }); }
      if (action === "accept") {
        const member = await client.query("SELECT 1 FROM agency_members WHERE creator_user_id=$1 FOR UPDATE", [req.userId]);
        if (member.rows.length) { await client.query("ROLLBACK"); return res.status(409).json({ error: "أنت مرتبط بوكالة أخرى" }); }
        await client.query("INSERT INTO agency_members(agency_application_id,creator_user_id,invitation_id) VALUES($1,$2,$3)", [invite.rows[0].agency_application_id, req.userId, invitationId]);
      }
      await client.query("UPDATE agency_invitations SET status=$2,responded_at=NOW() WHERE id=$1", [invitationId, action === "accept" ? "accepted" : "rejected"]);
      await client.query("COMMIT");
      return res.json({ success: true, status: action === "accept" ? "accepted" : "rejected" });
    } catch (error) {
      await client.query("ROLLBACK");
      console.error("Agency invitation response:", error.message);
      return res.status(500).json({ error: "تعذر معالجة الدعوة" });
    } finally { client.release(); }
  });
};
