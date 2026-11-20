BEGIN;
CREATE TABLE IF NOT EXISTS public.user_follows (
  follower_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
  followed_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY (follower_id, followed_id),
  CONSTRAINT user_follows_no_self CHECK (follower_id <> followed_id)
);
CREATE INDEX IF NOT EXISTS user_follows_followed_created_idx
  ON public.user_follows (followed_id, created_at DESC);
CREATE INDEX IF NOT EXISTS user_follows_follower_created_idx
  ON public.user_follows (follower_id, created_at DESC);
GRANT SELECT, INSERT, DELETE ON public.user_follows TO :"app_role";
COMMIT;
