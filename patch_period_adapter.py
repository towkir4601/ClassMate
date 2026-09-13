import re

with open('app/src/main/java/com/shuaib/classmate/adapters/PeriodAdapter.kt', 'r') as f:
    content = f.read()

bad = """                isCompleted -> {
                    val mutedColor = ThemeColors.textMuted(context)
                    bindStatusColor(context, b, mutedColor)
                    b.cardRoot.setBackgroundResource(R.drawable.bg_card_paused)
                    
                    if (period.isSubstitute) {
                        b.tvSubstituteMsg.isVisible = true
                        b.tvSubstituteMsg.text = "Substitute: ${period.substituteTeacher}"
                    }
                    
                    b.tvTypeBadge.text = "COMPLETED"
                    b.tvTypeBadge.setBackgroundResource(R.drawable.bg_badge_paused)
                    b.tvTypeBadge.setTextColor(mutedColor)
                }"""
good = """                isCompleted -> {
                    val mutedColor = ThemeColors.textMuted(context)
                    bindStatusColor(context, b, mutedColor)
                    b.cardRoot.setBackgroundResource(R.drawable.bg_card_paused)
                    
                    if (period.isSubstitute) {
                        b.tvSubstituteMsg.isVisible = true
                        b.tvSubstituteMsg.text = "Substitute: ${period.substituteTeacher}"
                    }
                    
                    b.tvTypeBadge.text = "COMPLETED"
                    b.tvTypeBadge.setBackgroundResource(R.drawable.bg_library_badge_green)
                    b.tvTypeBadge.setTextColor(ThemeColors.success(context))
                }"""
content = content.replace(bad, good)

bad2 = """        b.vCancelledDivider.isVisible = false"""
good2 = """        b.vCancelledDivider.isVisible = false
        
        if (period.batch.isNotBlank() && period.batch != "all") {
            b.tvBatchBadge.isVisible = true
            b.tvBatchBadge.text = "BATCH ${period.batch}"
        } else {
            b.tvBatchBadge.isVisible = false
        }"""
content = content.replace(bad2, good2)

with open('app/src/main/java/com/shuaib/classmate/adapters/PeriodAdapter.kt', 'w') as f:
    f.write(content)
