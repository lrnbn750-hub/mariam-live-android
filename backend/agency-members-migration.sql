-- Persistent agency invitations and creator membership.
CREATE TABLE IF NOT EXISTS agency_invitations (
  id BIGSERIAL PRIMARY KEY,
  agency_application_id BIGINT NOT NULL REFERENCES agency_applications(id) ON DELETE CASCADE,
  creator_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  invited_by UUID REFERENCES users(id) ON DELETE SET NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'pending'
    CHECK (status IN ('pending','accepted','rejected','cancelled')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  responded_at TIMESTAMPTZ
);
CREATE UNIQUE INDEX IF NOT EXISTS agency_invites_one_pending_creator
  ON agency_invitations(creator_user_id) WHERE status='pending';
CREATE INDEX IF NOT EXISTS agency_invites_agency_status
  ON agency_invitations(agency_application_id,status,created_at DESC);
CREATE TABLE IF NOT EXISTS agency_members (
  agency_application_id BIGINT NOT NULL REFERENCES agency_applications(id) ON DELETE CASCADE,
  creator_user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
  invitation_id BIGINT UNIQUE REFERENCES agency_invitations(id) ON DELETE SET NULL,
  joined_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY(agency_application_id,creator_user_id)
);
