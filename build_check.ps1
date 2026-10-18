$env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.12"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
& ".\gradlew.bat" compileDebugKotlin 2>&1 | Out-File -Encoding utf8 build_out.txt
