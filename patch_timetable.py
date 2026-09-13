import re

with open("app/src/main/java/com/shuaib/classmate/fragments/TimetableFragment.kt", "r") as f:
    content = f.read()

# Replace getFullDateForIndex
old_get_full_date = """    private fun getFullDateForIndex(index: Int): String {
        val today = LocalDate.now()
        val todayIndex = getTodayIndex()
        val targetDate = if (todayIndex == 5 || todayIndex == 6) {
            val diff = (index - todayIndex + 7) % 7
            today.plusDays(diff.toLong())
        } else {
            val saturday = today.minusDays(todayIndex.toLong())
            saturday.plusDays(index.toLong())
        }
        return targetDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    }"""

new_get_full_date = """    private fun getTargetDateForIndex(index: Int): LocalDate {
        val today = LocalDate.now()
        val todayIndex = getTodayIndex()
        return if (todayIndex == 5 || todayIndex == 6) {
            val diff = (index - todayIndex + 7) % 7
            today.plusDays(diff.toLong())
        } else {
            val saturday = today.minusDays(todayIndex.toLong())
            saturday.plusDays(index.toLong())
        }
    }

    private fun getFullDateForIndex(index: Int): String {
        return getTargetDateForIndex(index).format(DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US))
    }"""

content = content.replace(old_get_full_date, new_get_full_date)

# Replace isPastDay logic
old_is_past_day = """        val isPastDay = if (targetDateString.isNotBlank()) {
            try {
                LocalDate.parse(targetDateString).isBefore(LocalDate.now())
            } catch (e: Exception) {
                days.indexOf(day.lowercase()) < getTodayIndex()
            }
        } else {
            days.indexOf(day.lowercase()) < getTodayIndex()
        }"""

new_is_past_day = """        val isPastDay = if (dayIndex != -1) {
            getTargetDateForIndex(dayIndex).isBefore(LocalDate.now())
        } else {
            false
        }"""

content = content.replace(old_is_past_day, new_is_past_day)

with open("app/src/main/java/com/shuaib/classmate/fragments/TimetableFragment.kt", "w") as f:
    f.write(content)
