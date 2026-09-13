import re

with open('app/src/main/java/com/shuaib/classmate/activities/RegisterActivity.kt', 'r') as f:
    content = f.read()

bad = "identifyUserInOneSignal(uid, binding.etBatch.text.toString().trim())"
good = """val calculatedBatch = if (isTeacher) "" else {
            val p = binding.etStudentId.text.toString().take(2).toIntOrNull()
            if (p != null) (p - 10).toString() else ""
        }
        identifyUserInOneSignal(uid, calculatedBatch)"""
content = content.replace(bad, good)

with open('app/src/main/java/com/shuaib/classmate/activities/RegisterActivity.kt', 'w') as f:
    f.write(content)
