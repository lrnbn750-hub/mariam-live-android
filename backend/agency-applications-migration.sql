CREATE TABLE IF NOT EXISTS agency_applications (
  id BIGSERIAL PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  agency_name VARCHAR(80) NOT NULL,
  legal_name VARCHAR(120) NOT NULL,
  country_code CHAR(2) NOT NULL,
  id_front_encrypted BYTEA NOT NULL,
  id_back_encrypted BYTEA NOT NULL,
  selfie_encrypted BYTEA NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'pending'
    CHECK (status IN ('pending','approved','rejected')),
  review_note VARCHAR(500),
  reviewed_by UUID REFERENCES users(id) ON DELETE SET NULL,
  reviewed_at TIMESTAMPTZ,
  consent_at TIMESTAMPTZ NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS agency_applications_one_active_per_user
ON agency_applications(user_id)
WHERE status IN ('pending','approved');

CREATE INDEX IF NOT EXISTS agency_applications_status_created
ON agency_applications(status, created_at DESC);
