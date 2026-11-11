BEGIN;
CREATE TABLE IF NOT EXISTS public.user_levels (
  user_id UUID PRIMARY KEY REFERENCES public.users(id) ON DELETE CASCADE,
  total_xp BIGINT NOT NULL DEFAULT 0 CHECK (total_xp >= 0),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TABLE IF NOT EXISTS public.user_xp_events (
  id BIGSERIAL PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
  event_key VARCHAR(180) NOT NULL,
  xp_amount INTEGER NOT NULL CHECK (xp_amount BETWEEN 1 AND 10000),
  reason VARCHAR(60) NOT NULL DEFAULT 'activity',
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (user_id, event_key)
);
CREATE INDEX IF NOT EXISTS user_xp_events_user_created_idx
  ON public.user_xp_events(user_id, created_at DESC);
GRANT SELECT, INSERT, UPDATE, DELETE ON public.user_levels, public.user_xp_events TO :"app_role";
GRANT USAGE, SELECT ON SEQUENCE public.user_xp_events_id_seq TO :"app_role";
COMMIT;
