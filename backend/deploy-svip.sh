#!/usr/bin/env bash
set -euo pipefail

cd /opt/mariam-live/backend
test -f .env || { echo "Missing backend .env"; exit 1; }
test -f src/server.js || { echo "src/server.js not found"; exit 1; }

BASE="https://raw.githubusercontent.com/lrnbn750-hub/mariam-live-android/feature/svip-perks-latest-audio-8-15/backend"
curl -fsSL "$BASE/svip-route.js" -o src/routes/svip.js
curl -fsSL "$BASE/svip-migration.sql" -o svip-migration.sql

set -a
. ./.env
set +a
: "${DB_HOST:?DB_HOST missing}"
: "${DB_NAME:?DB_NAME missing}"
: "${DB_USER:?DB_USER missing}"
: "${DB_PASSWORD:?DB_PASSWORD missing}"

export PGPASSWORD="$DB_PASSWORD"
psql -h "$DB_HOST" -p "${DB_PORT:-5432}" -U "$DB_USER" -d "$DB_NAME" \
  -v app_role="$DB_USER" -v ON_ERROR_STOP=1 -f svip-migration.sql
unset PGPASSWORD

cp src/server.js "src/server.js.before-svip-$(date +%Y%m%d-%H%M%S)"
node <<'NODE'
const fs = require("fs");
const p = "src/server.js";
let s = fs.readFileSync(p, "utf8");
const line = 'require("./routes/svip")(app, db, authenticateToken);';
if (!s.includes(line)) {
  const marker = /app\.listen\s*\(/;
  if (!marker.test(s)) throw new Error("Could not find app.listen() in src/server.js; no route was mounted");
  s = s.replace(marker, line + "\n\napp.listen(");
  fs.writeFileSync(p, s);
}
NODE

node --check src/routes/svip.js
node --check src/server.js
pm2 restart mariam-live-api --update-env
sleep 2
curl -fsS https://nokiraq.duckdns.org/api/health
echo
echo "SVIP routes and database migration installed."
echo "SVIP prices are intentionally inactive until configured in public.svip_packages."
