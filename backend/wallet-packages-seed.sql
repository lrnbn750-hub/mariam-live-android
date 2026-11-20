-- BAK LIVE wallet packages
-- Pricing rule: 200 base coins per USD.
-- Bonus coins are promotional and do not change the base exchange rate.
BEGIN;

INSERT INTO public.wallet_packages
  (package_key, price_usd_cents, coins, bonus_coins, active)
VALUES
  ('starter_1_usd',    100,   200,    0, TRUE),
  ('starter_5_usd',    500,  1000,    0, TRUE),
  ('value_10_usd',    1000,  2000,  100, TRUE),
  ('popular_20_usd',  2000,  4000,  400, TRUE),
  ('pro_50_usd',      5000, 10000, 1500, TRUE),
  ('elite_100_usd',  10000, 20000, 4000, TRUE)
ON CONFLICT (package_key) DO UPDATE SET
  price_usd_cents = EXCLUDED.price_usd_cents,
  coins = EXCLUDED.coins,
  bonus_coins = EXCLUDED.bonus_coins,
  active = EXCLUDED.active;

COMMIT;
