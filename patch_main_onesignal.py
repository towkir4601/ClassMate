import re

with open('app/src/main/java/com/shuaib/classmate/activities/MainActivity.kt', 'r') as f:
    content = f.read()

bad = """                    try {
                        val r = doc.getString("role") ?: "student"
                        com.onesignal.OneSignal.User.addTag("role", r)
                    } catch (e: Exception) {"""
good = """                    try {
                        val r = doc.getString("role") ?: "student"
                        com.onesignal.OneSignal.User.addTag("role", r)
                        val b = doc.getString("batch") ?: ""
                        if (b.isNotBlank()) com.onesignal.OneSignal.User.addTag("batch", b)
                    } catch (e: Exception) {"""
content = content.replace(bad, good)
with open('app/src/main/java/com/shuaib/classmate/activities/MainActivity.kt', 'w') as f:
    f.write(content)
