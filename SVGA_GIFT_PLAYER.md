# SVGA gift player integration

This branch adds a reusable Jetpack Compose wrapper around SVGAPlayer-Android.

## Add a licensed gift animation

1. Create: `app/src/main/assets/gifts/`
2. Add an SVGA file that BAK LIVE is licensed to distribute, for example `sample_gift.svga`.
3. Use the player in a Compose screen:

```kotlin
SvgaGiftPlayer(
    assetPath = "gifts/sample_gift.svga",
    modifier = Modifier.fillMaxSize(),
    loops = 1
)
```

The view uses a transparent background so the animation can be layered over the room UI.

## Dependency

The Android SVGA player is added from JitPack at version 2.6.1. Its upstream Android repository is archived, so validate compatibility with the project's current Android Gradle Plugin and device matrix before release.

Do not bundle SVGA files extracted from third-party apps unless you have explicit redistribution rights. The player library's license does not grant rights to animation assets.
