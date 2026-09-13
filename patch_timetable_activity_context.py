import re

with open('app/src/main/java/com/shuaib/classmate/activities/TimetableManagementActivity.kt', 'r') as f:
    content = f.read()

bad = "Context.MODE_PRIVATE"
good = "android.content.Context.MODE_PRIVATE"
content = content.replace(bad, good)
with open('app/src/main/java/com/shuaib/classmate/activities/TimetableManagementActivity.kt', 'w') as f:
    f.write(content)
