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
