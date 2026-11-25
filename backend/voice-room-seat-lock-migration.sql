-- Run once against the BAK LIVE PostgreSQL database with a migration/admin role.
ALTER TABLE voice_room_seats
  ADD COLUMN IF NOT EXISTS is_locked BOOLEAN NOT NULL DEFAULT FALSE;

-- The host always owns seat 1; clear any accidental lock there.
UPDATE voice_room_seats
SET is_locked = FALSE
WHERE seat_number = 1;
