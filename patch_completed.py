import re

with open('app/src/main/java/com/shuaib/classmate/adapters/PeriodAdapter.kt', 'r') as f:
    content = f.read()

# Add checkIsCompleted
check_live_method = """    private fun checkIsLive(period: Period): Boolean {
        return try {
            val now = LocalTime.now()
            val start = LocalTime.parse(period.startTime)
            val end = LocalTime.parse(period.endTime)
            !now.isBefore(start) && now.isBefore(end)
        } catch (e: Exception) {
            false
        }
    }"""

check_completed_method = check_live_method + """

    private fun checkIsCompleted(period: Period): Boolean {
        return try {
            val now = LocalTime.now()
            val end = LocalTime.parse(period.endTime)
            now.isAfter(end) || now == end
        } catch (e: Exception) {
            false
        }
    }"""

content = content.replace(check_live_method, check_completed_method)

# Add isCompleted flag
bind_is_live = "val isLive = isViewingToday && !isPausedByCalendarException && !period.isCancelled && checkIsLive(period)"
bind_is_completed = bind_is_live + "\n        val isCompleted = isViewingToday && !isPausedByCalendarException && !period.isCancelled && !isLive && checkIsCompleted(period)"
content = content.replace(bind_is_live, bind_is_completed)

# Add isCompleted to when block
when_else = """                else -> {
                    bindStatusColor(context, b, accent)
                    b.cardRoot.setBackgroundResource(R.drawable.bg_card_primary)
                    if (isLabSession) {
                        b.tvTypeBadge.text = "LAB"
                        b.tvTypeBadge.setBackgroundResource(R.drawable.bg_badge_green)
                        b.tvTypeBadge.setTextColor(ContextCompat.getColor(context, R.color.cm_file_lab_text))
                    } else {
                        b.tvTypeBadge.text = "CLASS"
                        b.tvTypeBadge.setBackgroundResource(R.drawable.bg_badge_blue)
                        b.tvTypeBadge.setTextColor(ThemeColors.primary(context))
                    }
                }"""

when_completed = """                isCompleted -> {
                    val mutedColor = ThemeColors.textMuted(context)
                    bindStatusColor(context, b, mutedColor)
                    b.cardRoot.setBackgroundResource(R.drawable.bg_card_primary)
                    
                    if (period.isSubstitute) {
                        b.tvSubstituteMsg.isVisible = true
                        b.tvSubstituteMsg.text = "Substitute: ${period.substituteTeacher}"
                    }
                    
                    b.tvTypeBadge.text = "COMPLETED"
                    b.tvTypeBadge.setBackgroundResource(R.drawable.bg_badge_paused)
                    b.tvTypeBadge.setTextColor(mutedColor)
                }

""" + when_else

content = content.replace(when_else, when_completed)

with open('app/src/main/java/com/shuaib/classmate/adapters/PeriodAdapter.kt', 'w') as f:
    f.write(content)
