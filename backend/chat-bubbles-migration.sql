BEGIN;

-- Extend the inventory type constraints without disturbing existing equipped items.
ALTER TABLE public.store_items DROP CONSTRAINT IF EXISTS store_items_item_type_check;
ALTER TABLE public.store_items
  ADD CONSTRAINT store_items_item_type_check
  CHECK (item_type IN ('frame','background','chat_bubble'));

ALTER TABLE public.user_equipped_items DROP CONSTRAINT IF EXISTS user_equipped_items_item_type_check;
ALTER TABLE public.user_equipped_items
  ADD CONSTRAINT user_equipped_items_item_type_check
  CHECK (item_type IN ('frame','background','chat_bubble'));

-- Seven original, native-rendered skins. The app draws these locally in 2D;
-- asset_url is a style key, not a remote image URL.
INSERT INTO public.store_items(item_id,name,item_type,asset_url,source_type,price_iqd,active)
VALUES
 ('chat-bubble-dragon','التنين الأحمر الملكي','chat_bubble','bakbubble://chat-bubble-dragon','offer',0,TRUE),
 ('chat-bubble-purple','البنفسجي الملكي','chat_bubble','bakbubble://chat-bubble-purple','offer',0,TRUE),
 ('chat-bubble-blue','الأزرق الكريستالي','chat_bubble','bakbubble://chat-bubble-blue','offer',0,TRUE),
 ('chat-bubble-green','الزمرد الأخضر','chat_bubble','bakbubble://chat-bubble-green','offer',0,TRUE),
 ('chat-bubble-pink','الوردي الرومانسي','chat_bubble','bakbubble://chat-bubble-pink','offer',0,TRUE),
 ('chat-bubble-gold','الذهبي VIP','chat_bubble','bakbubble://chat-bubble-gold','offer',0,TRUE),
 ('chat-bubble-obsidian','الأسود الفاخر','chat_bubble','bakbubble://chat-bubble-obsidian','offer',0,TRUE)
ON CONFLICT(item_id) DO UPDATE SET
 name=EXCLUDED.name,
 item_type=EXCLUDED.item_type,
 asset_url=EXCLUDED.asset_url,
 source_type='offer',
 price_iqd=0,
 active=TRUE;

-- Give the seven launch skins to all existing accounts; the API grants them
-- automatically to accounts created later.
INSERT INTO public.user_inventory(user_id,item_id,obtained_from)
SELECT u.id, s.item_id, 'offer'
FROM public.users u
CROSS JOIN public.store_items s
WHERE s.item_type='chat_bubble' AND s.active=TRUE
ON CONFLICT(user_id,item_id) DO NOTHING;

GRANT SELECT, INSERT, UPDATE, DELETE ON public.store_items, public.user_inventory, public.user_equipped_items TO :"app_role";
COMMIT;
