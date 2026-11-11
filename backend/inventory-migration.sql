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

GRANT SELECT, INSERT, UPDATE, DELETE ON public.store_items, public.user_inventory, public.user_equipped_items TO :"app_role";
COMMIT;
