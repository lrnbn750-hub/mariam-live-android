BEGIN;

CREATE TABLE IF NOT EXISTS public.wallet_accounts (
  user_id UUID PRIMARY KEY REFERENCES public.users(id) ON DELETE CASCADE,
  diamonds_balance BIGINT NOT NULL DEFAULT 0 CHECK (diamonds_balance >= 0),
  earnings_iqd BIGINT NOT NULL DEFAULT 0 CHECK (earnings_iqd >= 0),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.wallet_transactions (
  id BIGSERIAL PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
  transaction_type VARCHAR(24) NOT NULL CHECK (transaction_type IN ('topup','gift_sent','gift_received','cashout','adjustment')),
  currency VARCHAR(16) NOT NULL CHECK (currency IN ('diamonds','iqd')),
  amount BIGINT NOT NULL CHECK (amount > 0),
  status VARCHAR(20) NOT NULL DEFAULT 'completed' CHECK (status IN ('pending','completed','rejected')),
  description VARCHAR(180) NOT NULL DEFAULT '',
  reference_id VARCHAR(100),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS wallet_transactions_user_created_idx
  ON public.wallet_transactions(user_id, created_at DESC);

CREATE TABLE IF NOT EXISTS public.wallet_topup_requests (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
  package_id VARCHAR(40) NOT NULL,
  amount_iqd BIGINT NOT NULL CHECK (amount_iqd > 0),
  diamonds BIGINT NOT NULL CHECK (diamonds > 0),
  payment_method VARCHAR(24) NOT NULL CHECK (payment_method IN ('zain_cash','asia_hawala','usdt','gateway')),
  payment_reference VARCHAR(120) NOT NULL DEFAULT '',
  status VARCHAR(20) NOT NULL DEFAULT 'pending' CHECK (status IN ('pending','approved','rejected')),
  reviewed_by UUID REFERENCES public.users(id),
  reviewed_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS wallet_topups_user_created_idx
  ON public.wallet_topup_requests(user_id, created_at DESC);

CREATE TABLE IF NOT EXISTS public.wallet_cashout_requests (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
  amount_iqd BIGINT NOT NULL CHECK (amount_iqd >= 25000),
  payout_method VARCHAR(24) NOT NULL CHECK (payout_method IN ('zain_cash','asia_hawala','bank')),
  payout_account VARCHAR(120) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'pending' CHECK (status IN ('pending','approved','rejected')),
  reviewed_by UUID REFERENCES public.users(id),
  reviewed_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

GRANT SELECT, INSERT, UPDATE ON public.wallet_accounts, public.wallet_transactions,
  public.wallet_topup_requests, public.wallet_cashout_requests TO :"app_role";
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO :"app_role";
COMMIT;
