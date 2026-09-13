import re

with open('app/src/main/java/com/shuaib/classmate/activities/TimetableManagementActivity.kt', 'r') as f:
    content = f.read()

bad = "val isToday = day == getTodayName()"
good = """val isToday = day == getTodayName()
                    val todayIndex = days.indexOf(getTodayName())
                    val isPastDay = days.indexOf(day.lowercase()) < todayIndex"""

content = content.replace(bad, good)

with open('app/src/main/java/com/shuaib/classmate/activities/TimetableManagementActivity.kt', 'w') as f:
    f.write(content)

