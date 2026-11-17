-- Agency lifecycle controls for future admin panel.
-- Existing applications and encrypted identity documents are preserved.

ALTER TABLE agency_applications
  DROP CONSTRAINT IF EXISTS agency_applications_status_check;

ALTER TABLE agency_applications
  ADD CONSTRAINT agency_applications_status_check
  CHECK (status IN ('pending','approved','rejected','blocked','revoked'));

ALTER TABLE agency_applications
  ADD COLUMN IF NOT EXISTS last_activity_at TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS blocked_at TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS blocked_by UUID REFERENCES users(id) ON DELETE SET NULL,
  ADD COLUMN IF NOT EXISTS block_reason VARCHAR(500),
  ADD COLUMN IF NOT EXISTS revoked_at TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS revoked_by UUID REFERENCES users(id) ON DELETE SET NULL,
  ADD COLUMN IF NOT EXISTS revoke_reason VARCHAR(500),
  ADD COLUMN IF NOT EXISTS inactivity_warning_sent_at TIMESTAMPTZ;

UPDATE agency_applications
SET last_activity_at = COALESCE(last_activity_at, updated_at, created_at)
WHERE last_activity_at IS NULL;

ALTER TABLE agency_applications
  ALTER COLUMN last_activity_at SET DEFAULT NOW();

DROP INDEX IF EXISTS agency_applications_one_active_per_user;
CREATE UNIQUE INDEX IF NOT EXISTS agency_applications_one_active_per_user
ON agency_applications(user_id)
WHERE status IN ('pending','approved','blocked');

CREATE INDEX IF NOT EXISTS agency_applications_last_activity
ON agency_applications(status, last_activity_at)
WHERE status = 'approved';

CREATE TABLE IF NOT EXISTS agency_admin_audit (
  id BIGSERIAL PRIMARY KEY,
  application_id BIGINT NOT NULL REFERENCES agency_applications(id) ON DELETE RESTRICT,
  actor_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
  action VARCHAR(24) NOT NULL
    CHECK (action IN ('approved','rejected','blocked','unblocked','revoked','inactivity_warning','inactivity_revoked')),
  reason VARCHAR(500),
  previous_status VARCHAR(16),
  new_status VARCHAR(16),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS agency_admin_audit_application
ON agency_admin_audit(application_id, created_at DESC);

-- Admin-configurable inactivity policy; automation remains disabled until explicitly enabled.
CREATE TABLE IF NOT EXISTS agency_settings (
  setting_key VARCHAR(64) PRIMARY KEY,
  setting_value JSONB NOT NULL,
  updated_by UUID REFERENCES users(id) ON DELETE SET NULL,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO agency_settings(setting_key, setting_value)
VALUES
  ('inactive_after_days', '90'::jsonb),
  ('auto_revoke_inactive', 'false'::jsonb),
  ('inactivity_warning_days', '14'::jsonb)
ON CONFLICT (setting_key) DO NOTHING;
