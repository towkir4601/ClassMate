import re

with open('app/src/main/java/com/shuaib/classmate/activities/TimetableManagementActivity.kt', 'r') as f:
    content = f.read()

is_today = """                    val todayIndex = DateHelper.getTodayIndex()
                    val isToday = currentDay.lowercase() == days[todayIndex].lowercase()"""
is_past = """                    val todayIndex = DateHelper.getTodayIndex()
                    val isToday = currentDay.lowercase() == days[todayIndex].lowercase()
                    val isPastDay = days.indexOf(currentDay.lowercase()) < todayIndex"""
content = content.replace(is_today, is_past)

update_old = "periodAdapter.updateList(periodList, isToday)"
update_new = "periodAdapter.updateList(periodList, isToday, isPastDay)"
content = content.replace(update_old, update_new)

with open('app/src/main/java/com/shuaib/classmate/activities/TimetableManagementActivity.kt', 'w') as f:
    f.write(content)
