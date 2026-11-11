BEGIN;

CREATE TABLE IF NOT EXISTS public.store_items (
  item_id VARCHAR(80) PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  item_type VARCHAR(20) NOT NULL CHECK (item_type IN ('frame','background')),
  asset_url TEXT NOT NULL,
  source_type VARCHAR(20) NOT NULL DEFAULT 'admin' CHECK (source_type IN ('admin','purchase','offer')),
  price_iqd BIGINT NOT NULL DEFAULT 0 CHECK (price_iqd >= 0),
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.user_inventory (
  user_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
  item_id VARCHAR(80) NOT NULL REFERENCES public.store_items(item_id) ON DELETE CASCADE,
  obtained_from VARCHAR(20) NOT NULL CHECK (obtained_from IN ('admin','purchase','offer')),
  obtained_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY (user_id, item_id)
);

CREATE TABLE IF NOT EXISTS public.user_equipped_items (
  user_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
  item_type VARCHAR(20) NOT NULL CHECK (item_type IN ('frame','background')),
  item_id VARCHAR(80) NOT NULL REFERENCES public.store_items(item_id) ON DELETE CASCADE,
  equipped_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY (user_id, item_type)
);

INSERT INTO public.store_items(item_id,name,item_type,asset_url,source_type,price_iqd,active)
VALUES ('royal100-frame','الإطار الملكي LV 100','frame',
'https://raw.githubusercontent.com/lrnbn750-hub/mariam-live-android/feature/mariam-live-development/app/src/main/res/drawable-nodpi/royal100.gif',
'admin',0,TRUE)
ON CONFLICT(item_id) DO UPDATE SET
 name=EXCLUDED.name,item_type=EXCLUDED.item_type,asset_url=EXCLUDED.asset_url,active=TRUE;

GRANT SELECT, INSERT, UPDATE, DELETE ON public.store_items, public.user_inventory, public.user_equipped_items TO :"app_role";
COMMIT;
