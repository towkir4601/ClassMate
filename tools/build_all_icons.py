"""
build_all_icons.py
Generates all Android launcher icon assets (adaptive foreground, legacy square, legacy round, splash logo, in-app logo)
from app/src/main/assets/gstu_go_logo.webp.
Ensures transparent antialiased edges, no black corner artifacts, and full compliance with
Android adaptive icon and SplashScreen guidelines.
"""
from PIL import Image, ImageDraw
from pathlib import Path
import os

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "app" / "src" / "main" / "assets" / "gstu_go_logo.webp"
RES = ROOT / "app" / "src" / "main" / "res"

print(f"Loading source logo: {SRC}")
src_img = Image.open(SRC).convert("RGBA")
w, h = src_img.size

# Step 1: Create clean anti-aliased squircle emblem
# Super-sampled (4x) rounded rectangle mask matching the 120px corner radius
scale_factor = 4
mask_hi = Image.new("L", (w * scale_factor, h * scale_factor), 0)
draw = ImageDraw.Draw(mask_hi)
draw.rounded_rectangle(
    [13 * scale_factor, 15 * scale_factor, 498 * scale_factor, 494 * scale_factor],
    radius=120 * scale_factor,
    fill=255,
)
mask_squircle = mask_hi.resize((w, h), Image.Resampling.LANCZOS)

clean_squircle = Image.new("RGBA", (w, h), (0, 0, 0, 0))
clean_squircle.paste(src_img, (0, 0), mask_squircle)

# Save full-res in-app logo to res/drawable/ic_app_logo.png (for LoginActivity, etc.)
app_logo_path = RES / "drawable" / "ic_app_logo.png"
clean_squircle.save(app_logo_path, "PNG", optimize=True)
print(f"Saved in-app logo: {app_logo_path}")

# Step 2: Create splash screen logo with safe-zone padding
# Android's SplashScreen API masks the windowSplashScreenAnimatedIcon within a central
# circle (diameter ~66% of canvas). By sizing the squircle to 307px (60% scale) on a
# 512x512 transparent canvas, the entire GSTU GO logo and text remain 100% visible
# and are NEVER trimmed by the circular splash mask.
splash_canvas = Image.new("RGBA", (w, h), (0, 0, 0, 0))
splash_sz = 307
logo_splash = clean_squircle.resize((splash_sz, splash_sz), Image.Resampling.LANCZOS)
off_s = (w - splash_sz) // 2
splash_canvas.paste(logo_splash, (off_s, off_s), logo_splash)
splash_logo_path = RES / "drawable" / "ic_splash_logo.png"
splash_canvas.save(splash_logo_path, "PNG", optimize=True)
print(f"Saved splash logo: {splash_logo_path}")

# Step 3: Clean legacy round emblem
circle_hi = Image.new("RGBA", (w * scale_factor, h * scale_factor), (0, 0, 0, 0))
d_c = ImageDraw.Draw(circle_hi)
cx, cy, r = w * scale_factor // 2, h * scale_factor // 2, (w * scale_factor // 2) - 4
d_c.ellipse([cx - r, cy - r, cx + r, cy + r], fill=(255, 255, 255, 255))
clean_round_bg = circle_hi.resize((w, h), Image.Resampling.LANCZOS)

squircle_in_round = clean_squircle.resize((round(w * 0.76), round(h * 0.76)), Image.Resampling.LANCZOS)
off_r = (w - squircle_in_round.width) // 2
clean_round = clean_round_bg.copy()
clean_round.paste(squircle_in_round, (off_r, off_r), squircle_in_round)

DENSITIES = {
    "mdpi": 1.0,
    "hdpi": 1.5,
    "xhdpi": 2.0,
    "xxhdpi": 3.0,
    "xxxhdpi": 4.0,
}

# Legacy launcher icons (48dp base)
LAUNCHER_DP = 48
print("\nGenerating legacy launcher icons (48dp base):")
for density, factor in DENSITIES.items():
    sz = round(LAUNCHER_DP * factor)
    out_dir = RES / f"mipmap-{density}"
    out_dir.mkdir(parents=True, exist_ok=True)
    icon_sq = clean_squircle.resize((sz, sz), Image.Resampling.LANCZOS)
    icon_sq.save(out_dir / "ic_launcher.png", "PNG", optimize=True)
    icon_rd = clean_round.resize((sz, sz), Image.Resampling.LANCZOS)
    icon_rd.save(out_dir / "ic_launcher_round.png", "PNG", optimize=True)
    print(f"  mipmap-{density:<8}: {sz}x{sz} px")

# Adaptive icon foregrounds (108dp canvas, safe zone 72dp -> 68% scale fits all launcher shapes)
ADAPTIVE_DP = 108
LOGO_SCALE = 0.68
print("\nGenerating adaptive icon foregrounds (108dp canvas):")
for density, factor in DENSITIES.items():
    canvas_sz = round(ADAPTIVE_DP * factor)
    logo_sz = round(canvas_sz * LOGO_SCALE)
    canvas = Image.new("RGBA", (canvas_sz, canvas_sz), (0, 0, 0, 0))
    logo_resized = clean_squircle.resize((logo_sz, logo_sz), Image.Resampling.LANCZOS)
    offset = (canvas_sz - logo_sz) // 2
    canvas.paste(logo_resized, (offset, offset), logo_resized)
    out_dir = RES / f"mipmap-{density}"
    canvas.save(out_dir / "ic_launcher_foreground.png", "PNG", optimize=True)
    print(f"  mipmap-{density:<8}: canvas={canvas_sz}x{canvas_sz}, logo={logo_sz}x{logo_sz}")

# Remove invalid PNGs from mipmap-anydpi / mipmap-anydpi-v26 if present
for d in ["mipmap-anydpi", "mipmap-anydpi-v26"]:
    png_path = RES / d / "ic_launcher_foreground.png"
    if png_path.exists():
        png_path.unlink()
        print(f"Removed invalid PNG: {png_path}")

print("\nAll launcher icon assets generated successfully.")
