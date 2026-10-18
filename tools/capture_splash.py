import subprocess
import time
from PIL import Image

def capture_splash():
    print("Launching SplashActivity and taking screencap at 1.8 seconds...")
    # Start SplashActivity
    subprocess.run(["adb", "shell", "am", "start", "-S", "-n", "com.shuaib.classmate/.activities.SplashActivity"], check=True)
    # Wait 1.8 seconds (cold start takes ~0.5s, so this is ~1.3s into animation)
    time.sleep(1.8)
    # Take screenshot directly to a fresh file
    target_remote = f"/sdcard/splash_{int(time.time())}.png"
    subprocess.run(["adb", "shell", "screencap", "-p", target_remote], check=True)
    local_file = "actual_splash_capture.png"
    subprocess.run(["adb", "pull", target_remote, local_file], check=True)
    subprocess.run(["adb", "shell", "rm", "-f", target_remote], check=True)
    
    im = Image.open(local_file)
    print(f"Captured {local_file}: size={im.size}, mode={im.mode}")
    colors = im.getcolors(maxcolors=2000000)
    print(f"Unique colors: {len(colors)}")
    
    # Check top colors
    from collections import Counter
    c = Counter(im.getdata())
    print("Top 10 colors:")
    for col, count in c.most_common(10):
        print(f"  {col}: {count} pixels")

if __name__ == '__main__':
    capture_splash()
