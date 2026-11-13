# BAK LIVE gifts backend deployment

This repository contains the additive migration and route module. The production backend currently lives at `/opt/mariam-live/backend`, so do not run this against production until the migration and route have been reviewed and backed up.

## Files
- `gifts-migration.sql`: creates the gift catalog and room event feed.
- `gifts-route.js`: authenticated catalog, send, and room event-feed endpoints.

## Install on the backend host
1. Back up the database and `src/server.js`.
2. Copy `gifts-migration.sql` to the backend host and apply it using the same PostgreSQL connection settings as the app.
3. Copy `gifts-route.js` to `src/routes/gifts.js`.
4. Register the route beside the existing wallet and voice-room routes:
   `require("./routes/gifts")(app, db, authenticateToken);`
5. Run a JavaScript syntax check and test with two test accounts in a private test room before restarting the production PM2 process.

## Gift economy
The route reads `gift_earnings` and `platform_share` from `wallet_settings`. With the currently approved settings, one gift unit costs 500 recharge coins (based on 200 coins/USD), awards 1,000 diamonds, and credits the recipient with 125 USD cents in creator earnings. The platform share is the remaining 125 USD cents.

## Important
The starter catalog currently has empty `animation_url` values. Upload licensed, tested animation assets and update those URLs before enabling the gift UI. The room feed is cursor-based polling; a LiveKit data broadcast can replace it later if desired. No Android APK build or production deployment is included in this commit.
