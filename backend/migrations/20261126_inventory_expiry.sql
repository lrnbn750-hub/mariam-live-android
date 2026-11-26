-- BAK LIVE cosmetic inventory expiry.
-- NULL expires_at means a permanent/admin-granted item.
ALTER TABLE user_inventory
  ADD COLUMN IF NOT EXISTS expires_at TIMESTAMPTZ NULL;

CREATE INDEX IF NOT EXISTS idx_user_inventory_active_expiry
  ON user_inventory (user_id, expires_at)
  WHERE expires_at IS NOT NULL;

-- Remove any already-expired temporary cosmetics from active equipment.
DELETE FROM user_equipped_items e
USING user_inventory ui
WHERE e.user_id=ui.user_id
  AND e.item_id=ui.item_id
  AND ui.expires_at IS NOT NULL
  AND ui.expires_at <= NOW();

-- Store purchases use diamonds, not withdrawable IQD earnings.
ALTER TABLE store_items
  ADD COLUMN IF NOT EXISTS price_diamonds BIGINT NOT NULL DEFAULT 0
  CHECK (price_diamonds >= 0);

-- Record diamond spending separately in the wallet ledger.
ALTER TABLE wallet_transactions
  DROP CONSTRAINT IF EXISTS wallet_transactions_transaction_type_check;
ALTER TABLE wallet_transactions
  ADD CONSTRAINT wallet_transactions_transaction_type_check
  CHECK (transaction_type IN ('topup','gift_sent','gift_received','cashout','adjustment','store_purchase'));


-- Standard BAK LIVE cosmetic prices, charged in diamonds.
UPDATE public.store_items
SET price_diamonds = CASE item_type
  WHEN 'frame' THEN 1000
  WHEN 'chat_bubble' THEN 500
  WHEN 'background' THEN 2000
  ELSE price_diamonds
END
WHERE item_type IN ('frame','chat_bubble','background');

-- Apply the standard price automatically when new catalog items are added.
CREATE OR REPLACE FUNCTION public.set_bak_store_item_diamond_price()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
  IF NEW.item_type = 'frame' THEN
    NEW.price_diamonds := 1000;
  ELSIF NEW.item_type = 'chat_bubble' THEN
    NEW.price_diamonds := 500;
  ELSIF NEW.item_type = 'background' THEN
    NEW.price_diamonds := 2000;
  END IF;
  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_set_bak_store_item_diamond_price ON public.store_items;
CREATE TRIGGER trg_set_bak_store_item_diamond_price
BEFORE INSERT OR UPDATE OF item_type ON public.store_items
FOR EACH ROW
EXECUTE FUNCTION public.set_bak_store_item_diamond_price();
