-- Additive BAK LIVE wallet compatibility migration.
-- The app's top-up balance (diamonds_balance) is the single spendable gift currency.
-- Existing balances are preserved; payout conversion remains disabled until configured.
BEGIN;

ALTER TABLE public.wallet_accounts
  ADD COLUMN IF NOT EXISTS earned_diamonds BIGINT NOT NULL DEFAULT 0 CHECK (earned_diamonds >= 0),
  ADD COLUMN IF NOT EXISTS withdrawable_usd_cents BIGINT NOT NULL DEFAULT 0 CHECK (withdrawable_usd_cents >= 0),
  ADD COLUMN IF NOT EXISTS lifetime_earned_usd_cents BIGINT NOT NULL DEFAULT 0 CHECK (lifetime_earned_usd_cents >= 0);

ALTER TABLE public.wallet_transactions
  ADD COLUMN IF NOT EXISTS event_key VARCHAR(180),
  ADD COLUMN IF NOT EXISTS related_user_id UUID REFERENCES public.users(id) ON DELETE SET NULL,
  ADD COLUMN IF NOT EXISTS metadata JSONB NOT NULL DEFAULT '{}'::jsonb;

CREATE UNIQUE INDEX IF NOT EXISTS wallet_transactions_event_key_uidx
  ON public.wallet_transactions(event_key)
  WHERE event_key IS NOT NULL;

CREATE TABLE IF NOT EXISTS public.wallet_settings (
  setting_key VARCHAR(64) PRIMARY KEY,
  setting_value JSONB NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

GRANT SELECT, INSERT, UPDATE ON public.wallet_accounts, public.wallet_transactions,
  public.wallet_settings TO :"app_role";

COMMIT;

-- Revenue split explicitly approved for BAK LIVE:
-- 50% creator/host, 50% platform. USD conversion is intentionally not seeded
-- until the product owner confirms the value per gift unit.
INSERT INTO public.wallet_settings(setting_key, setting_value, updated_at)
VALUES (
  'platform_share',
  '{"creator_percent":50,"platform_percent":50}'::jsonb,
  NOW()
)
ON CONFLICT (setting_key) DO UPDATE
SET setting_value = EXCLUDED.setting_value,
    updated_at = NOW();
