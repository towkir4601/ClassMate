# Set Java 21 environment
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.12"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

Write-Host ">>> Checking connected device..." -ForegroundColor Cyan
adb devices

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
