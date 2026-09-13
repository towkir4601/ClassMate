import re

with open('app/src/main/java/com/shuaib/classmate/activities/LoginActivity.kt', 'r') as f:
    content = f.read()

bad = "identifyUserAndNavigate(uid, role)"
good = "identifyUserAndNavigate(uid, role, existingData[\"batch\"] as? String ?: \"\")"
content = content.replace(bad, good)
with open('app/src/main/java/com/shuaib/classmate/activities/LoginActivity.kt', 'w') as f:
    f.write(content)
