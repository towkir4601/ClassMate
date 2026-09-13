with open('app/src/main/java/com/shuaib/classmate/adapters/PeriodAdapter.kt', 'r') as f:
    content = f.read()

bad_str = """
                period.isSubstitute -> {
                    bindStatusColor(context, b, ThemeColors.warning(context))
                    b.cardRoot.setBackgroundResource(R.drawable.bg_card_notice_sub)
                    b.tvSubstituteMsg.isVisible = true
                    b.tvSubstituteMsg.text = "Substitute: ${period.substituteTeacher}"
                    b.tvTypeBadge.text = "SUBSTITUTE"
                    b.tvTypeBadge.setBackgroundResource(R.drawable.bg_badge_purple)
                    b.tvTypeBadge.setTextColor(ThemeColors.warning(context))
                }


                    val mutedColor = ThemeColors.textMuted(context)
                    bindStatusColor(context, b, mutedColor)
                    b.cardRoot.setBackgroundResource(R.drawable.bg_card_primary)"""

good_str = """
                isCompleted -> {
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
                }"""

content = content.replace(bad_str, good_str)
with open('app/src/main/java/com/shuaib/classmate/adapters/PeriodAdapter.kt', 'w') as f:
    f.write(content)
