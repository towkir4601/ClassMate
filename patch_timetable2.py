import re

with open('app/src/main/java/com/shuaib/classmate/activities/TimetableManagementActivity.kt', 'r') as f:
    content = f.read()

setup_dialog = """        dialogBinding.dropdownSubject.setAdapter(adapter)
        
        if (period != null) {
            dialogBinding.layoutDaysSelection.visibility = View.GONE
            dialogBinding.dropdownSubject.setText("${period.subject}", false)
            dialogBinding.etTeacher.setText(period.teacher)
            dialogBinding.etRoom.setText(period.room)
            dialogBinding.etStartTime.setText(period.startTime)
            dialogBinding.etEndTime.setText(period.endTime)
            dialogBinding.cbTemporary.isChecked = period.isTemporary
        } else {
            dialogBinding.layoutDaysSelection.visibility = View.VISIBLE
            val chipId = when (currentDay) {
                "saturday" -> R.id.chipSat
                "sunday" -> R.id.chipSun
                "monday" -> R.id.chipMon
                "tuesday" -> R.id.chipTue
                "wednesday" -> R.id.chipWed
                "thursday" -> R.id.chipThu
                "friday" -> R.id.chipFri
                else -> R.id.chipSat
            }
            dialogBinding.chipGroupDays.check(chipId)
            
            dialogBinding.cbTemporary.setOnCheckedChangeListener { _, isChecked ->
                dialogBinding.layoutDaysSelection.visibility = if (isChecked) View.GONE else View.VISIBLE
            }
        }"""

pattern = re.compile(r'dialogBinding\.dropdownSubject\.setAdapter\(adapter\).*?dialogBinding\.chipGroupDays\.check\(chipId\)\n        \}', re.DOTALL)
content = pattern.sub(setup_dialog, content)

# Also fix the updatedPeriod mapping in savePeriodsForMultipleDays call
# Because my previous script passed newPeriod which had temporaryDate of currentDay.
# We need to map it inside savePeriodsForMultipleDays to calculate the proper date for EACH day if it is temporary.
# Actually, if layoutDaysSelection is hidden when cbTemporary is checked, selectedDays will be empty!
# If selectedDays is empty, it falls back to:
# if (selectedDays.isEmpty()) selectedDays.add(currentDay)
# Which means it perfectly saves only for currentDay, and the date matches currentDay! This is brilliant!

with open('app/src/main/java/com/shuaib/classmate/activities/TimetableManagementActivity.kt', 'w') as f:
    f.write(content)

