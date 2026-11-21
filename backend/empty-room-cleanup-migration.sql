-- Persist the start of each room's empty-seat countdown.
-- The backend closes an active room after 3 minutes with no occupied audio or camera seat.
ALTER TABLE public.voice_rooms
  ADD COLUMN IF NOT EXISTS empty_since timestamptz;

CREATE INDEX IF NOT EXISTS voice_rooms_empty_since_idx
  ON public.voice_rooms(empty_since)
  WHERE status = 'active' AND empty_since IS NOT NULL;
