import re

with open('app/src/main/java/com/shuaib/classmate/widget/WidgetTimetableService.kt', 'r') as f:
    content = f.read()

bad = """            val targetDateString = DateHelper.today()
            val prefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
            val currentUserBatch = prefs.getString("current_batch", "all") ?: "all"
            val currentUserRole = prefs.getString("user_role", "student") ?: "student"

            val filteredEntities = rawEntities.filter { entity ->
                !entity.isTemporary || entity.temporaryDate.isBlank() || entity.temporaryDate == targetDateString
            }.filter { entity ->
                entity.batch.isBlank() || entity.batch == "all" || entity.batch == currentUserBatch || currentUserRole == "superadmin" || currentUserRole == "admin"
            }"""
good = """            val targetDateString = DateHelper.today()

            val filteredEntities = rawEntities.filter { entity ->
                !entity.isTemporary || entity.temporaryDate.isBlank() || entity.temporaryDate == targetDateString
            }"""
content = content.replace(bad, good)
with open('app/src/main/java/com/shuaib/classmate/widget/WidgetTimetableService.kt', 'w') as f:
    f.write(content)
