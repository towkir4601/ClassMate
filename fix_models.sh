#!/bin/bash
# Update build.gradle.kts
sed -i '' 's/"gemini-2.5-flash"/"gemini-3.6-flash"/g' app/build.gradle.kts

# Update AiSettingsActivity to include 3.6-flash
sed -i '' 's/"gemini-2.5-flash", "gemini-3.5-flash"/"gemini-3.6-flash", "gemini-2.5-flash", "gemini-3.5-flash"/g' app/src/main/java/com/shuaib/classmate/activities/AiSettingsActivity.kt
