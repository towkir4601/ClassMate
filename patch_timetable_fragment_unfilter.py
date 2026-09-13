import re

with open('app/src/main/java/com/shuaib/classmate/fragments/TimetableFragment.kt', 'r') as f:
    content = f.read()

bad = """        val prefs = requireContext().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        val currentUserBatch = prefs.getString("current_batch", "all") ?: "all"
        val currentUserRole = prefs.getString("user_role", "student") ?: "student"
        
        val periods = rawPeriods.filter { period ->
            !period.isTemporary || period.temporaryDate.isBlank() || period.temporaryDate == targetDateString
        }.filter { period ->
            period.batch.isBlank() || period.batch == "all" || period.batch == currentUserBatch || currentUserRole == "superadmin" || currentUserRole == "admin"
        }.map { period ->"""
good = """        val periods = rawPeriods.filter { period ->
            !period.isTemporary || period.temporaryDate.isBlank() || period.temporaryDate == targetDateString
        }.map { period ->"""
content = content.replace(bad, good)
with open('app/src/main/java/com/shuaib/classmate/fragments/TimetableFragment.kt', 'w') as f:
    f.write(content)
