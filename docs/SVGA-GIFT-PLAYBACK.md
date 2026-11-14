# SVGA gift playback

Room gift events now recognize server-provided `.svga` animation URLs and render them in the room overlay using SVGAPlayer-Android. JSON animation URLs continue to use Lottie. The gift picker shows the existing artwork/icon for SVGA entries rather than trying to parse SVGA as Lottie.

## Asset rights
Only upload SVGA files for which BAK LIVE has documented permission for commercial redistribution and in-app display. The app does not bundle the previously extracted third-party `aladdin.svga` asset. Add licensed assets to the gift catalog through the backend `animation_url` field.

## Backend requirement
The `/api/voice-rooms/:roomId/gifts` event feed must return the selected gift's `animation_url` ending in `.svga` for clients to play it. Use HTTPS URLs accessible to signed-in app users.
