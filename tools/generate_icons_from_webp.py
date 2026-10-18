"""
Generate all Android launcher icon PNGs from assets/gstu_go_logo.webp.
Source WebP is NOT modified. PNGs are written to mipmap-* directories.
Adaptive icon foreground is centred in safe-zone (74 % of canvas) per Android spec.
"""
from PIL import Image
from pathlib import Path
import sys

ROOT   = Path(__file__).resolve().parents[1]
SRC    = ROOT / "app" / "src" / "main" / "assets" / "gstu_go_logo.webp"
RES    = ROOT / "app" / "src" / "main" / "res"

if not SRC.exists():
    sys.exit(f"ERROR: source not found at {SRC}")

img = Image.open(SRC).convert("RGBA")
print(f"Source : {SRC.name}  {img.width}x{img.height}  mode={img.mode}")

DENSITIES = {
    "mdpi"   : 1.0,
    "hdpi"   : 1.5,
    "xhdpi"  : 2.0,
    "xxhdpi" : 3.0,
    "xxxhdpi": 4.0,
}

# ── Launcher icons  (48 dp base) ────────────────────────────────────────────
LAUNCHER_DP = 48
print("\nLauncher icons (ic_launcher / ic_launcher_round):")
for density, factor in DENSITIES.items():
    sz  = round(LAUNCHER_DP * factor)
    out = RES / f"mipmap-{density}"
    out.mkdir(parents=True, exist_ok=True)
    icon = img.resize((sz, sz), Image.Resampling.LANCZOS)
    icon.save(out / "ic_launcher.png",       "PNG", optimize=True)
    icon.save(out / "ic_launcher_round.png", "PNG", optimize=True)
    print(f"  mipmap-{density:<8}  {sz}×{sz} px")

# ── Adaptive icon foregrounds  (108 dp canvas, logo 74 % safe zone) ─────────
ADAPTIVE_DP  = 108
LOGO_SCALE   = 0.74   # keeps logo comfortably inside the 72-dp safe zone
print("\nAdaptive icon foregrounds (ic_launcher_foreground):")
for density, factor in DENSITIES.items():
    canvas_sz = round(ADAPTIVE_DP * factor)
    logo_sz   = round(canvas_sz * LOGO_SCALE)
    canvas    = Image.new("RGBA", (canvas_sz, canvas_sz), (0, 0, 0, 0))
    logo      = img.resize((logo_sz, logo_sz), Image.Resampling.LANCZOS)
    offset    = (canvas_sz - logo_sz) // 2
    canvas.paste(logo, (offset, offset), logo)
    out = RES / f"mipmap-{density}"
    canvas.save(out / "ic_launcher_foreground.png", "PNG", optimize=True)
    print(f"  mipmap-{density:<8}  canvas {canvas_sz}×{canvas_sz}  logo {logo_sz}×{logo_sz}")

# mipmap-anydpi-v26 foreground (432 × 432 = 108 dp @ ×4 / xxxhdpi scale)
anydpi_out    = RES / "mipmap-anydpi-v26"
anydpi_canvas = 432
anydpi_logo   = round(anydpi_canvas * LOGO_SCALE)
canvas        = Image.new("RGBA", (anydpi_canvas, anydpi_canvas), (0, 0, 0, 0))
logo          = img.resize((anydpi_logo, anydpi_logo), Image.Resampling.LANCZOS)
offset        = (anydpi_canvas - anydpi_logo) // 2
canvas.paste(logo, (offset, offset), logo)
canvas.save(anydpi_out / "ic_launcher_foreground.png", "PNG", optimize=True)
print(f"  mipmap-anydpi-v26  canvas {anydpi_canvas}×{anydpi_canvas}  logo {anydpi_logo}×{anydpi_logo}")

print("\nAll icon sizes generated successfully. Original WebP unchanged.")
