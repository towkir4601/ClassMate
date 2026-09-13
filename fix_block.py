with open('app/src/main/java/com/shuaib/classmate/adapters/PeriodAdapter.kt', 'r') as f:
    content = f.read()

bad_block = """                    if (period.isSubstitute) {
                        b.tvSubstituteMsg.isVisible = true
                        b.tvSubstituteMsg.text = "Substitute: ${period.substituteTeacher}"
                    }
                    
                    b.tvTypeBadge.text = "COMPLETED"
                    b.tvTypeBadge.setBackgroundResource(R.drawable.bg_badge_paused)
                    b.tvTypeBadge.setTextColor(mutedColor)
                }

                else -> {"""
                
content = content.replace(bad_block, """                else -> {""")

with open('app/src/main/java/com/shuaib/classmate/adapters/PeriodAdapter.kt', 'w') as f:
    f.write(content)
