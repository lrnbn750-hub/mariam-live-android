CREATE TABLE IF NOT EXISTS public.voice_room_messages (
  id BIGSERIAL PRIMARY KEY,
  room_id UUID NOT NULL REFERENCES public.voice_rooms(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
  message VARCHAR(500) NOT NULL CHECK (char_length(message) BETWEEN 1 AND 500),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS voice_room_messages_room_id_id_idx
  ON public.voice_room_messages(room_id, id DESC);
GRANT SELECT, INSERT ON public.voice_room_messages TO :"app_role";
GRANT USAGE, SELECT ON SEQUENCE public.voice_room_messages_id_seq TO :"app_role";
