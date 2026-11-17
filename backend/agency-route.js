const crypto = require("crypto");

const MAX_IMAGE_BYTES = 1536 * 1024;
const IMAGE_KEY_ENV = "AGENCY_DOC_ENCRYPTION_KEY";

function decodeImage(value, label) {
  if (typeof value !== "string" || value.length < 100 || value.length > 2200000 ||
      !/^[A-Za-z0-9+/]+={0,2}$/.test(value)) {
    throw new Error(`صورة ${label} غير صالحة`);
  }
  const bytes = Buffer.from(value, "base64");
  const jpeg = bytes.length > 4 && bytes[0] === 0xff && bytes[1] === 0xd8 &&
    bytes[bytes.length - 2] === 0xff && bytes[bytes.length - 1] === 0xd9;
  const png = bytes.length > 8 && bytes.subarray(0, 8).equals(Buffer.from([137,80,78,71,13,10,26,10]));
  const webp = bytes.length > 12 && bytes.toString("ascii", 0, 4) === "RIFF" &&
    bytes.toString("ascii", 8, 12) === "WEBP";
  if (bytes.length > MAX_IMAGE_BYTES || (!jpeg && !png && !webp)) {
    throw new Error(`صورة ${label} يجب أن تكون JPEG أو PNG أو WEBP وبحجم لا يتجاوز 1.5MB`);
  }
  return bytes;
}

function encryptionKey() {
  const raw = process.env[IMAGE_KEY_ENV] || "";
  if (!/^[a-f0-9]{64}$/i.test(raw)) throw new Error("Agency document encryption key is not configured");
  return Buffer.from(raw, "hex");
}

function encryptImage(bytes, key) {
  const iv = crypto.randomBytes(12);
  const cipher = crypto.createCipheriv("aes-256-gcm", key, iv);
  const encrypted = Buffer.concat([cipher.update(bytes), cipher.final()]);
  return Buffer.concat([iv, cipher.getAuthTag(), encrypted]);
}

module.exports = function registerAgencyRoutes(app, db, authenticateToken) {
  app.get("/api/agency/application", authenticateToken, async (req, res) => {
    try {
      const result = await db.query(
        `SELECT id, agency_name, legal_name, country_code, status, review_note,
                created_at, updated_at
         FROM agency_applications
         WHERE user_id = $1
         ORDER BY created_at DESC LIMIT 1`,
        [req.userId]
      );
      return res.json({ application: result.rows[0] || null });
    } catch (error) {
      console.error("Agency application status:", error.message);
      return res.status(500).json({ error: "تعذر تحميل حالة طلب الوكالة" });
    }
  });

  app.post("/api/agency/application", authenticateToken, async (req, res) => {
    const body = req.body || {};
    const agencyName = String(body.agency_name || "").trim();
    const legalName = String(body.legal_name || "").trim();
    const countryCode = String(body.country_code || "").trim().toUpperCase();

    if (agencyName.length < 2 || agencyName.length > 80 ||
        legalName.length < 3 || legalName.length > 120 ||
        !/^[A-Z]{2}$/.test(countryCode) || body.consent !== true) {
      return res.status(400).json({ error: "أكمل اسم الوكالة والاسم القانوني والدولة والموافقة على معالجة الوثائق" });
    }

    let front, back, selfie, key;
    try {
      front = decodeImage(body.id_front_base64, "الهوية الأمامي");
      back = decodeImage(body.id_back_base64, "الهوية الخلفي");
      selfie = decodeImage(body.selfie_base64, "الصورة الشخصية");
      key = encryptionKey();
    } catch (error) {
      const status = error.message.includes("encryption key") ? 503 : 400;
      if (status === 503) console.error(error.message);
      return res.status(status).json({ error: status === 503 ? "خدمة توثيق الوكالات غير مهيأة حالياً" : error.message });
    }

    const client = await db.connect();
    try {
      await client.query("BEGIN");
      const existing = await client.query(
        `SELECT id, status FROM agency_applications
         WHERE user_id=$1 AND status IN ('pending','approved')
         FOR UPDATE`,
        [req.userId]
      );
      if (existing.rows.length) {
        await client.query("ROLLBACK");
        return res.status(409).json({ error: "لديك طلب وكالة قيد المراجعة أو وكالة مفعّلة بالفعل" });
      }

      const result = await client.query(
        `INSERT INTO agency_applications
          (user_id, agency_name, legal_name, country_code,
           id_front_encrypted, id_back_encrypted, selfie_encrypted,
           consent_at, status)
         VALUES ($1,$2,$3,$4,$5,$6,$7,NOW(),'pending')
         RETURNING id, agency_name, legal_name, country_code, status, created_at`,
        [req.userId, agencyName, legalName, countryCode,
         encryptImage(front, key), encryptImage(back, key), encryptImage(selfie, key)]
      );
      await client.query("COMMIT");
      return res.status(201).json({
        message: "تم استلام طلب الوكالة وسيتم مراجعته من الإدارة",
        application: result.rows[0]
      });
    } catch (error) {
      await client.query("ROLLBACK");
      console.error("Create agency application:", error.message);
      return res.status(500).json({ error: "تعذر إرسال طلب الوكالة" });
    } finally {
      client.release();
    }
  });
};
