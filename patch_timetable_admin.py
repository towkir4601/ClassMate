import re

with open("app/src/main/java/com/shuaib/classmate/activities/TimetableManagementActivity.kt", "r") as f:
    content = f.read()

# Fix getFullDateForIndex Locale bug
old_date = """        return targetDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))"""
new_date = """        return targetDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd", java.util.Locale.US))"""
content = content.replace(old_date, new_date)

# Fix Period creation and save multiple days logic
old_save = """                val selectedSubject = dialogBinding.dropdownSubject.text.toString().trim()
                val teacher = dialogBinding.etTeacher.text.toString().trim()
                val room = dialogBinding.etRoom.text.toString().trim()
                val start = dialogBinding.etStartTime.text.toString()
                val end = dialogBinding.etEndTime.text.toString()

                if (selectedSubject.isNotEmpty() && teacher.isNotEmpty()) {
                    val updatedPeriod = Period(
                        id = period?.id ?: "",
                        subject = selectedSubject,
                        teacher = teacher,
                        startTime = start,
                        endTime = end,
                        room = room
                    )
                    
                    com.shuaib.classmate.utils.NotificationSender.sendNoticeAlert(
                        title = "Routine Updated",
                        body = "The class routine has been updated."
                    )
                    
                    if (isEdit) {
                        savePeriod(updatedPeriod, true)
                    } else {
                        val selectedDays = mutableListOf<String>()
                        val checkedIds = dialogBinding.chipGroupDays.checkedChipIds
                        for (id in checkedIds) {
                            when (id) {
                                R.id.chipSat -> selectedDays.add("saturday")
                                R.id.chipSun -> selectedDays.add("sunday")
                                R.id.chipMon -> selectedDays.add("monday")
                                R.id.chipTue -> selectedDays.add("tuesday")
                                R.id.chipWed -> selectedDays.add("wednesday")
                                R.id.chipThu -> selectedDays.add("thursday")
                                R.id.chipFri -> selectedDays.add("friday")
                            }
                        }
                        if (selectedDays.isEmpty()) {
                            selectedDays.add(currentDay)
                        }
                        savePeriodsForMultipleDays(updatedPeriod, selectedDays)
                    }
                } else {"""

new_save = """                val selectedSubject = dialogBinding.dropdownSubject.text.toString().trim()
                val teacher = dialogBinding.etTeacher.text.toString().trim()
                val room = dialogBinding.etRoom.text.toString().trim()
                val start = dialogBinding.etStartTime.text.toString()
                val end = dialogBinding.etEndTime.text.toString()
                val batch = dialogBinding.etBatch.text.toString().trim()
                val isTemp = dialogBinding.cbTemporary.isChecked
                val tempDate = if (isTemp) getFullDateForIndex(days.indexOf(currentDay)) else ""

                if (selectedSubject.isNotEmpty() && teacher.isNotEmpty()) {
                    val updatedPeriod = period?.copy(
                        subject = selectedSubject,
                        teacher = teacher,
                        startTime = start,
                        endTime = end,
                        room = room,
                        batch = batch,
                        isTemporary = isTemp,
                        temporaryDate = tempDate
                    ) ?: Period(
                        id = "",
                        subject = selectedSubject,
                        teacher = teacher,
                        startTime = start,
                        endTime = end,
                        room = room,
                        batch = batch,
                        isTemporary = isTemp,
                        temporaryDate = tempDate
                    )
                    
                    com.shuaib.classmate.utils.NotificationSender.sendNoticeAlert(
                        title = "Routine Updated",
                        body = "The class routine has been updated."
                    )
                    
                    if (isEdit) {
                        savePeriod(updatedPeriod, true)
                    } else {
                        val selectedDays = mutableListOf<String>()
                        if (isTemp) {
                            selectedDays.add(currentDay)
                        } else {
                            val checkedIds = dialogBinding.chipGroupDays.checkedChipIds
                            for (id in checkedIds) {
                                when (id) {
                                    R.id.chipSat -> selectedDays.add("saturday")
                                    R.id.chipSun -> selectedDays.add("sunday")
                                    R.id.chipMon -> selectedDays.add("monday")
                                    R.id.chipTue -> selectedDays.add("tuesday")
                                    R.id.chipWed -> selectedDays.add("wednesday")
                                    R.id.chipThu -> selectedDays.add("thursday")
                                    R.id.chipFri -> selectedDays.add("friday")
                                }
                            }
                            if (selectedDays.isEmpty()) {
                                selectedDays.add(currentDay)
                            }
                        }
                        savePeriodsForMultipleDays(updatedPeriod, selectedDays)
                    }
                } else {"""

content = content.replace(old_save, new_save)

with open("app/src/main/java/com/shuaib/classmate/activities/TimetableManagementActivity.kt", "w") as f:
    f.write(content)
