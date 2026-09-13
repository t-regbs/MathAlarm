# MathAlarm icon

`mathalarm-mark.svg` is the editable vector master, reconstructed from the old pixelated icon. It preserves the alarm clock, plus/multiply/equals/minus layout, green (#34B840), and dark background (#1A1A1A). Curves and symbol edges are reconstructed approximations, not recovered original design files.

Generated assets:
- `mathalarm-icon.svg`: square icon with an opaque dark background.
- `mathalarm-adaptive-foreground.svg`: transparent foreground for the Android adaptive canvas.
- `mathalarm-icon-1024.png`: high-resolution export.
- Android: vector adaptive foreground, Android 13 themed icon layer, and standard/round legacy PNGs at 36, 48, 72, 96, 144, and 192 pixels. Debug retains its white background; release retains dark.
- iOS: all declared AppIcon catalog slots, including the opaque 1024px marketing icon, and existing launch-screen sizes.
- Shared UI, notification bitmap, and 512px Play/Fastlane artwork.

Regenerate from the repository root:

```sh
python3 -m pip install cairosvg pillow
python3 scripts/generate_icons.py
```

Edit paths in the master SVG (filled paths only); the generator derives Android VectorDrawable paths and rasterizes exports directly from the master, without upscaling the previous bitmap. Background settings and adaptive sizing live in the generator.

Adaptive artwork is centered and scaled inside the 66dp circular safe zone on a 108dp canvas, following [Android icon guidance](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive). The Android foreground is referenced from `drawable-v24`; obsolete raster foregrounds are removed during generation.

Android splash screens use a separate generated `drawable/ic_splash.xml`, centered at 80% scale on a 288dp canvas so the bells and feet fit comfortably inside the 192dp circular mask. Both light and dark themes reference this asset.
