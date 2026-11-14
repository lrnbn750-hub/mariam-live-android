-- BAK LIVE gifts: additive migration; does not alter existing balances or settings.
CREATE TABLE IF NOT EXISTS room_gifts (
  gift_id varchar(64) PRIMARY KEY,
  name varchar(100) NOT NULL,
  animation_url text NOT NULL,
  price_coins bigint NOT NULL CHECK (price_coins > 0),
  gift_units integer NOT NULL DEFAULT 1 CHECK (gift_units > 0),
  active boolean NOT NULL DEFAULT true,
  sort_order integer NOT NULL DEFAULT 0,
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS voice_room_gift_events (
  id bigserial PRIMARY KEY,
  room_id uuid NOT NULL REFERENCES voice_rooms(id) ON DELETE CASCADE,
  sender_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  recipient_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  gift_id varchar(64) NOT NULL REFERENCES room_gifts(gift_id),
  quantity integer NOT NULL CHECK (quantity BETWEEN 1 AND 99),
  total_coins bigint NOT NULL CHECK (total_coins > 0),
  diamonds_awarded bigint NOT NULL CHECK (diamonds_awarded > 0),
  creator_earnings_usd_cents bigint NOT NULL CHECK (creator_earnings_usd_cents >= 0),
  platform_share_usd_cents bigint NOT NULL DEFAULT 0 CHECK (platform_share_usd_cents >= 0),
  event_key varchar(180) NOT NULL UNIQUE,
  created_at timestamptz NOT NULL DEFAULT now()
);
ALTER TABLE voice_room_gift_events
  ADD COLUMN IF NOT EXISTS platform_share_usd_cents bigint NOT NULL DEFAULT 0
  CHECK (platform_share_usd_cents >= 0);

CREATE INDEX IF NOT EXISTS voice_room_gift_events_room_id_id_idx
  ON voice_room_gift_events(room_id, id);

-- Starter catalog entries are placeholders until licensed animation assets are uploaded.
INSERT INTO room_gifts(gift_id,name,animation_url,price_coins,gift_units,sort_order)
VALUES
 ('rose','وردة','',500,1,10),
 ('car','سيارة','',2000,4,20),
 ('lion','أسد','',10000,20,30),
 ('plane','طائرة','',20000,40,40),
 ('castle','قصر','',50000,100,50)
ON CONFLICT (gift_id) DO NOTHING;


-- Aladdin animated gift. The animation is served from this repository's raw asset URL.
-- Re-running the migration updates the catalog row without changing wallet balances.
INSERT INTO room_gifts(gift_id,name,animation_url,price_coins,gift_units,active,sort_order)
VALUES (
  'aladdin',
  '🧞‍♂️ علاء الدين',
  'https://raw.githubusercontent.com/lrnbn750-hub/mariam-live-android/feature/bak-live-svga-gifts-from-working-43581935436/app/src/main/res/raw/aladdin.svga',
  1000,
  1,
  TRUE,
  25
)
ON CONFLICT (gift_id) DO UPDATE SET
  name=EXCLUDED.name,
  animation_url=EXCLUDED.animation_url,
  price_coins=EXCLUDED.price_coins,
  gift_units=EXCLUDED.gift_units,
  active=EXCLUDED.active,
  sort_order=EXCLUDED.sort_order;
