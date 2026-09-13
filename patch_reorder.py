import re

with open('app/src/main/java/com/shuaib/classmate/adapters/PeriodAdapter.kt', 'r') as f:
    content = f.read()

old_when = """                period.isSubstitute -> {
                    bindStatusColor(context, b, ThemeColors.warning(context))
                    b.cardRoot.setBackgroundResource(R.drawable.bg_card_notice_sub)
                    b.tvSubstituteMsg.isVisible = true
                    b.tvSubstituteMsg.text = "Substitute: ${period.substituteTeacher}"
                    b.tvTypeBadge.text = "SUBSTITUTE"
                    b.tvTypeBadge.setBackgroundResource(R.drawable.bg_badge_purple)
                    b.tvTypeBadge.setTextColor(ThemeColors.warning(context))
                }

                isCompleted -> {"""

new_when = """                isCompleted -> {
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

                period.isSubstitute -> {
                    bindStatusColor(context, b, ThemeColors.warning(context))
                    b.cardRoot.setBackgroundResource(R.drawable.bg_card_notice_sub)
                    b.tvSubstituteMsg.isVisible = true
                    b.tvSubstituteMsg.text = "Substitute: ${period.substituteTeacher}"
                    b.tvTypeBadge.text = "SUBSTITUTE"
                    b.tvTypeBadge.setBackgroundResource(R.drawable.bg_badge_purple)
                    b.tvTypeBadge.setTextColor(ThemeColors.warning(context))
                }

                _dummy_to_be_replaced_ -> {"""

content = content.replace(old_when, new_when)

# Now remove the old isCompleted block
to_remove = """                isCompleted -> {
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
                }"""
content = content.replace(to_remove, "", 1) # only replace the second occurrence

content = content.replace("                _dummy_to_be_replaced_ -> {", "")

with open('app/src/main/java/com/shuaib/classmate/adapters/PeriodAdapter.kt', 'w') as f:
    f.write(content)
