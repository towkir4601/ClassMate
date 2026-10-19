from PIL import Image
import numpy as np

im = Image.open('app/src/main/assets/gstu_go_logo.webp')
w, h = im.size
arr = np.array(im)

print(f"Image shape: {arr.shape}")

# Find mask of where it is not black
# Black is roughly r < 15, g < 15, b < 15
is_black = (arr[:, :, 0] < 20) & (arr[:, :, 1] < 20) & (arr[:, :, 2] < 20)
print(f"Black pixels: {is_black.sum()} out of {w*h} ({is_black.sum()/(w*h)*100:.1f}%)")

# Let's find the circle center and radius
y_idx, x_idx = np.where(~is_black)
cx = (x_idx.min() + x_idx.max()) / 2
cy = (y_idx.min() + y_idx.max()) / 2
rx = (x_idx.max() - x_idx.min()) / 2
ry = (y_idx.max() - y_idx.min()) / 2
print(f"Non-black bbox: X=[{x_idx.min()}, {x_idx.max()}], Y=[{y_idx.min()}, {y_idx.max()}]")
print(f"Estimated center: ({cx:.1f}, {cy:.1f}), radii: rx={rx:.1f}, ry={ry:.1f}")

# Check edge pixels of the circle
for angle_deg in [0, 45, 90, 135, 180, 225, 270, 315]:
    rad = np.radians(angle_deg)
    # Check at radius rx - 2 (just inside)
    x = int(cx + (rx - 2) * np.cos(rad))
    y = int(cy + (ry - 2) * np.sin(rad))
    print(f"Angle {angle_deg:3d} deg inside edge ({x},{y}): {arr[y, x]}")
