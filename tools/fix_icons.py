"""
Fix ClassMate app icons:
1. Regenerate ic_splash_logo.png (windowSplashScreenAnimatedIcon) from ic_app_logo.png
   - Properly sized to fill the Android splash screen icon area without trimming
2. Regenerate ic_launcher_foreground.png for adaptive icons
3. Regenerate density-specific ic_classmate_app_icon.png (used in login page)

Android splash screen spec (API 31+):
  - Icon drawable area: 240x240dp
  - Icon is placed in a 288x288dp container with 24dp padding on each side
  - Icon is NOT clipped to circle if using windowSplashScreenAnimatedIcon with adaptive icon behavior
  - For a simple PNG (non-adaptive), the whole 240x240dp area is used, then masked as circle
  - To avoid clipping, the logo must be well within the inner safe area (~60-70% of 240dp)
"""

import os
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "app" / "src" / "main" / "res"
DRAWABLE = RES / "drawable"

SOURCE = DRAWABLE / "ic_app_logo.png"


def load_base_logo():
    """Load ic_app_logo.png and crop to its actual content bounding box."""
    img = Image.open(SOURCE).convert("RGBA")
    w, h = img.size

    # Find tight bounding box of non-transparent pixels
    bbox_left = w; bbox_top = h; bbox_right = 0; bbox_bottom = 0
    for x in range(w):
        for y in range(h):
            if img.getpixel((x, y))[3] > 10:
                if x < bbox_left: bbox_left = x
                if x > bbox_right: bbox_right = x
                if y < bbox_top: bbox_top = y
                if y > bbox_bottom: bbox_bottom = y

    # Add small padding (2px on each side)
    pad = 2
    bbox_left = max(0, bbox_left - pad)
    bbox_top = max(0, bbox_top - pad)
    bbox_right = min(w, bbox_right + pad)
    bbox_bottom = min(h, bbox_bottom + pad)

    # Crop to bounding box and make square
    content = img.crop((bbox_left, bbox_top, bbox_right, bbox_bottom))
    cw, ch = content.size
    side = max(cw, ch)
    
    # Center on square canvas
    square = Image.new("RGBA", (side, side), (0, 0, 0, 0))
    px = (side - cw) // 2
    py = (side - ch) // 2
    square.paste(content, (px, py), content)
    
    print(f"Base logo loaded: original={w}x{h}, content bbox=({bbox_left},{bbox_top},{bbox_right},{bbox_bottom}), square={side}x{side}")
    return square


def regenerate_splash_logo(base_logo):
    """
    Regenerate ic_splash_logo.png for windowSplashScreenAnimatedIcon.
    
    Android 12+ splash screen: the icon is 240x240dp.
    If the icon is a simple PNG (non-adaptive), Android masks it to a circle.
    To prevent the logo from being cut off, center it at ~60% of the canvas.
    
    We use a 512x512 canvas (xxxhdpi equivalent) for maximum quality.
    Logo scaled to 65% of canvas = ~333px to fit comfortably within circle.
    """
    canvas_size = 512
    logo_scale = 0.60  # 60% - safely inside the inscribed circle
    logo_size = int(canvas_size * logo_scale)
    
    canvas = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
    resized = base_logo.resize((logo_size, logo_size), Image.Resampling.LANCZOS)
    offset = (canvas_size - logo_size) // 2
    canvas.paste(resized, (offset, offset), resized)
    
    out_path = DRAWABLE / "ic_splash_logo.png"
    canvas.save(out_path, "PNG", optimize=True)
    print(f"Regenerated ic_splash_logo.png: canvas={canvas_size}x{canvas_size}, logo={logo_size}x{logo_size}")


def regenerate_mipmap_icons(base_logo):
    """Regenerate adaptive launcher foreground and legacy launcher icons."""
    densities = {
        "mdpi":     1.0,
        "hdpi":     1.5,
        "xhdpi":    2.0,
        "xxhdpi":   3.0,
        "xxxhdpi":  4.0,
    }

    # --- Adaptive foreground (ic_launcher_foreground.png) ---
    # 108dp canvas at each density; logo scaled to 74% to fit in 72dp safe zone
    adaptive_dp = 108
    logo_scale = 0.74

    print("\nRegenerating ic_launcher_foreground.png (adaptive icon foreground)...")
    for density, factor in densities.items():
        canvas_size = int(adaptive_dp * factor)
        logo_size = int(canvas_size * logo_scale)

        canvas = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
        resized = base_logo.resize((logo_size, logo_size), Image.Resampling.LANCZOS)
        offset = (canvas_size - logo_size) // 2
        canvas.paste(resized, (offset, offset), resized)

        out = RES / f"mipmap-{density}" / "ic_launcher_foreground.png"
        out.parent.mkdir(parents=True, exist_ok=True)
        canvas.save(out, "PNG", optimize=True)
        print(f"  {density}: {canvas_size}x{canvas_size} (logo {logo_size}x{logo_size})")

    # --- Legacy launcher icon (ic_launcher.png) ---
    # 48dp icon: the full logo fills the icon (no padding for these)
    launcher_dp = 48
    print("\nRegenerating ic_launcher.png (legacy launcher icon)...")
    for density, factor in densities.items():
        size = int(launcher_dp * factor)
        resized = base_logo.resize((size, size), Image.Resampling.LANCZOS)
        out = RES / f"mipmap-{density}" / "ic_launcher.png"
        out.parent.mkdir(parents=True, exist_ok=True)
        resized.save(out, "PNG", optimize=True)
        print(f"  {density}: {size}x{size}")

    # --- Round launcher icon (ic_launcher_round.png) ---
    print("\nRegenerating ic_launcher_round.png...")
    for density, factor in densities.items():
        size = int(launcher_dp * factor)
        resized = base_logo.resize((size, size), Image.Resampling.LANCZOS)

        # Create circular mask
        from PIL import ImageDraw
        mask = Image.new("L", (size, size), 0)
        draw = ImageDraw.Draw(mask)
        draw.ellipse((0, 0, size - 1, size - 1), fill=255)

        # Apply mask
        result = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        result.paste(resized, (0, 0), mask)

        out = RES / f"mipmap-{density}" / "ic_launcher_round.png"
        out.parent.mkdir(parents=True, exist_ok=True)
        result.save(out, "PNG", optimize=True)
        print(f"  {density}: {size}x{size}")


def regenerate_drawable_icons(base_logo):
    """Regenerate density-specific drawable assets."""
    densities = {
        "mdpi":     1.0,
        "hdpi":     1.5,
        "xhdpi":    2.0,
        "xxhdpi":   3.0,
        "xxxhdpi":  4.0,
    }

    # ic_classmate_app_icon.png - used in login page (96dp)
    app_icon_dp = 96
    print("\nRegenerating density-specific ic_classmate_app_icon.png...")
    for density, factor in densities.items():
        size = int(app_icon_dp * factor)
        resized = base_logo.resize((size, size), Image.Resampling.LANCZOS)
        out = RES / f"drawable-{density}" / "ic_classmate_app_icon.png"
        out.parent.mkdir(parents=True, exist_ok=True)
        resized.save(out, "PNG", optimize=True)
        print(f"  {density}: {size}x{size}")

    # Also save a base drawable version (for devices without density qualifier)
    base_size = 192  # xxhdpi equivalent for base drawable
    resized_base = base_logo.resize((base_size, base_size), Image.Resampling.LANCZOS)
    out_base = DRAWABLE / "ic_classmate_app_icon.png"
    resized_base.save(out_base, "PNG", optimize=True)
    print(f"  base (no density): {base_size}x{base_size}")


def main():
    print("=== ClassMate Icon Fixer ===\n")

    if not SOURCE.exists():
        raise FileNotFoundError(f"Source not found: {SOURCE}")

    base_logo = load_base_logo()

    print("\n1. Fixing splash logo (windowSplashScreenAnimatedIcon)...")
    regenerate_splash_logo(base_logo)

    print("\n2. Fixing mipmap launcher icons...")
    regenerate_mipmap_icons(base_logo)

    print("\n3. Fixing drawable app icon assets...")
    regenerate_drawable_icons(base_logo)

    print("\n=== Done! All icons regenerated successfully. ===")
    print("Next: rebuild and install the app to verify icon appearance.")


if __name__ == "__main__":
    main()
