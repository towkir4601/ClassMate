# Set Java 21 environment
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.12"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

Write-Host ">>> Checking connected device..." -ForegroundColor Cyan
$deviceOutput = adb devices
Write-Host $deviceOutput

$devices = ($deviceOutput -split "`r?`n" | Select-String -Pattern "\bdevice\b" | Where-Object { $_ -notmatch "List of devices attached" })

if (-not $devices) {
    Write-Host "`n[ERROR] No active Android device detected!" -ForegroundColor Red
    Write-Host "Please check the following on your phone:" -ForegroundColor Yellow
    Write-Host " 1. Re-plug the USB cable (ensure it's a data cable, not charge-only)." -ForegroundColor Yellow
    Write-Host " 2. Enable 'USB Debugging' under Developer Options." -ForegroundColor Yellow
    Write-Host " 3. Unlock your phone and accept 'Allow USB debugging' prompt if shown." -ForegroundColor Yellow
    Write-Host " 4. Or connect via Wi-Fi: adb connect <phone-ip>:<port>`n" -ForegroundColor Yellow
    exit 1
}

Write-Host "`n>>> Building & installing to connected device..." -ForegroundColor Cyan
& ".\gradlew.bat" :app:installDebug

if ($LASTEXITCODE -ne 0) {
    Write-Host "`n[ERROR] Build/Installation failed!" -ForegroundColor Red
    exit $LASTEXITCODE
}

Write-Host "`n>>> Launching ClassMate..." -ForegroundColor Green
adb shell am start -n com.shuaib.classmate/.activities.SplashActivity

Write-Host "`n>>> Streaming app logs (Ctrl+C to stop)..." -ForegroundColor Yellow
# Clear old logs and filter for this app's logs & errors
adb logcat -c
adb logcat -v time | Select-String "com.shuaib.classmate|AndroidRuntime|FATAL"
