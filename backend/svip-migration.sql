BEGIN;

CREATE TABLE IF NOT EXISTS public.svip_packages (
  level SMALLINT PRIMARY KEY CHECK (level BETWEEN 1 AND 7),
  name VARCHAR(32) NOT NULL,
  price_diamonds BIGINT NOT NULL DEFAULT 0 CHECK (price_diamonds >= 0),
  duration_days SMALLINT NOT NULL DEFAULT 30 CHECK (duration_days = 30),
  active BOOLEAN NOT NULL DEFAULT FALSE,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO public.svip_packages(level,name,price_diamonds,duration_days,active)
VALUES
  (1,'SVIP 1',15000,30,TRUE),
  (2,'SVIP 2',25000,30,TRUE),
  (3,'SVIP 3',40000,30,TRUE),
  (4,'SVIP 4',60000,30,TRUE),
  (5,'SVIP 5',80000,30,TRUE),
  (6,'SVIP 6',120000,30,TRUE),
  (7,'SVIP 7',160000,30,TRUE)
ON CONFLICT(level) DO UPDATE SET
  name=EXCLUDED.name,
  price_diamonds=EXCLUDED.price_diamonds,
  duration_days=EXCLUDED.duration_days,
  active=EXCLUDED.active,
  updated_at=NOW();

CREATE TABLE IF NOT EXISTS public.user_svip_memberships (
  user_id UUID PRIMARY KEY REFERENCES public.users(id) ON DELETE CASCADE,
  level SMALLINT NOT NULL REFERENCES public.svip_packages(level),
  starts_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  expires_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS user_svip_memberships_expiry_idx
  ON public.user_svip_memberships(expires_at);

GRANT SELECT ON public.svip_packages TO :"app_role";
GRANT SELECT, INSERT, UPDATE, DELETE ON public.user_svip_memberships TO :"app_role";
GRANT SELECT, INSERT, UPDATE ON public.wallet_accounts, public.wallet_transactions TO :"app_role";

COMMIT;
