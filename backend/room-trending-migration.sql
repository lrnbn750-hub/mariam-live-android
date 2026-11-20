-- Daily BAK LIVE room trending scores. Scores reset automatically by Baghdad date.
CREATE TABLE IF NOT EXISTS public.voice_room_daily_trends (
  room_id uuid NOT NULL REFERENCES public.voice_rooms(id) ON DELETE CASCADE,
  trend_date date NOT NULL,
  points bigint NOT NULL DEFAULT 0 CHECK (points >= 0),
  gift_coins bigint NOT NULL DEFAULT 0 CHECK (gift_coins >= 0),
  message_count bigint NOT NULL DEFAULT 0 CHECK (message_count >= 0),
  seat_seconds bigint NOT NULL DEFAULT 0 CHECK (seat_seconds >= 0),
  updated_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY(room_id,trend_date)
);
CREATE INDEX IF NOT EXISTS voice_room_daily_trends_rank_idx
  ON public.voice_room_daily_trends(trend_date,points DESC);

CREATE TABLE IF NOT EXISTS public.voice_room_trend_message_minutes (
  room_id uuid NOT NULL REFERENCES public.voice_rooms(id) ON DELETE CASCADE,
  user_id uuid NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
  minute_bucket bigint NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY(room_id,user_id,minute_bucket)
);
CREATE TABLE IF NOT EXISTS public.voice_room_trend_seat_sessions (
  room_id uuid NOT NULL REFERENCES public.voice_rooms(id) ON DELETE CASCADE,
  user_id uuid NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
  trend_date date NOT NULL,
  last_seen_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY(room_id,user_id,trend_date)
);

GRANT SELECT,INSERT,UPDATE ON public.voice_room_daily_trends TO :"app_role";
GRANT SELECT,INSERT,DELETE ON public.voice_room_trend_message_minutes TO :"app_role";
GRANT SELECT,INSERT,UPDATE ON public.voice_room_trend_seat_sessions TO :"app_role";
