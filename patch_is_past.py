import re

with open('app/src/main/java/com/shuaib/classmate/fragments/TimetableFragment.kt', 'r') as f:
    content = f.read()

old_str = """        val isToday = day.lowercase() == days[getTodayIndex()].lowercase()
        val isPastDay = days.indexOf(day.lowercase()) < getTodayIndex()"""

new_str = """        val isToday = day.lowercase() == days[getTodayIndex()].lowercase()
        val isPastDay = if (targetDateString.isNotBlank()) {
            try {
                LocalDate.parse(targetDateString).isBefore(LocalDate.now())
            } catch (e: Exception) {
                days.indexOf(day.lowercase()) < getTodayIndex()
            }
        } else {
            days.indexOf(day.lowercase()) < getTodayIndex()
        }"""

content = content.replace(old_str, new_str)

with open('app/src/main/java/com/shuaib/classmate/fragments/TimetableFragment.kt', 'w') as f:
    f.write(content)
