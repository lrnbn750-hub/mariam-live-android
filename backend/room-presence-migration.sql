-- BAK LIVE room presence heartbeats.
-- A client sends a heartbeat every 15 seconds while connected. Seats are
-- reclaimed by the API after 90 seconds without a heartbeat (3 minutes for
-- legacy seat rows created before this table existed).

CREATE TABLE IF NOT EXISTS public.voice_room_presence (
  room_id uuid NOT NULL REFERENCES public.voice_rooms(id) ON DELETE CASCADE,
  user_id uuid NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
  last_seen_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (room_id, user_id)
);

CREATE INDEX IF NOT EXISTS voice_room_presence_last_seen_idx
  ON public.voice_room_presence(last_seen_at);

GRANT SELECT, INSERT, UPDATE, DELETE
  ON public.voice_room_presence TO :"app_role";
