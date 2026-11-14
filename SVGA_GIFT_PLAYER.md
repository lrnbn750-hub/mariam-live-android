# SVGA gift player integration

This branch adds a reusable Jetpack Compose wrapper for SVGA animations.

## Add a licensed gift animation

1. Create `app/src/main/assets/gifts/`.
2. Add an SVGA animation that BAK LIVE is licensed to distribute.
3. Render it in a Compose screen:

```kotlin
SvgaGiftPlayer(
    assetPath = "gifts/sample_gift.svga",
    modifier = Modifier.fillMaxSize(),
    loops = 1
)
```

The player view has a transparent background and supports completion/error callbacks.

## Notes

The SVGAPlayer Android library is added through JitPack. Validate dependency resolution and Android compatibility in CI before release. The player library's license does not grant redistribution rights to third-party animation files. Do not bundle extracted Xena Live animations without explicit permission.
