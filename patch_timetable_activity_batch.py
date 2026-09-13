import re

with open('app/src/main/java/com/shuaib/classmate/activities/TimetableManagementActivity.kt', 'r') as f:
    content = f.read()

bad_edit = """            dialogBinding.etSubject.setText(period.subject)
            dialogBinding.etTeacher.setText(period.teacher)
            dialogBinding.etRoom.setText(period.room)
            dialogBinding.etStartTime.setText(period.startTime)
            dialogBinding.etEndTime.setText(period.endTime)"""
good_edit = """            dialogBinding.etSubject.setText(period.subject)
            dialogBinding.etTeacher.setText(period.teacher)
            dialogBinding.etRoom.setText(period.room)
            dialogBinding.etStartTime.setText(period.startTime)
            dialogBinding.etEndTime.setText(period.endTime)
            dialogBinding.etBatch.setText(period.batch, false)"""
content = content.replace(bad_edit, good_edit)

bad_dropdown = """        dialogBinding.etStartTime.setOnClickListener {"""
good_dropdown = """        val batches = mutableListOf("all")
        for (i in 64 downTo 10) batches.add(i.toString())
        val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, batches)
        dialogBinding.etBatch.setAdapter(adapter)
        if (period == null) {
            val currentUserBatch = getSharedPreferences("app_prefs", Context.MODE_PRIVATE).getString("current_batch", "all") ?: "all"
            dialogBinding.etBatch.setText(currentUserBatch, false)
        }
        dialogBinding.etStartTime.setOnClickListener {"""
content = content.replace(bad_dropdown, good_dropdown)

bad_save = """            val newPeriod = Period(
                id = period?.id ?: java.util.UUID.randomUUID().toString(),
                subject = subject,
                teacher = teacher,
                room = room,
                startTime = startTime,
                endTime = endTime,
                isTemporary = isTemporary,
                temporaryDate = if (isTemporary) targetDate else ""
            )"""
good_save = """            val batch = dialogBinding.etBatch.text.toString()
            val newPeriod = Period(
                id = period?.id ?: java.util.UUID.randomUUID().toString(),
                subject = subject,
                teacher = teacher,
                room = room,
                startTime = startTime,
                endTime = endTime,
                isTemporary = isTemporary,
                temporaryDate = if (isTemporary) targetDate else "",
                batch = batch
            )"""
content = content.replace(bad_save, good_save)

with open('app/src/main/java/com/shuaib/classmate/activities/TimetableManagementActivity.kt', 'w') as f:
    f.write(content)
