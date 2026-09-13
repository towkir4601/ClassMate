import re

with open('app/src/main/java/com/shuaib/classmate/activities/RegisterActivity.kt', 'r') as f:
    content = f.read()

bad_batch = "val batch = if (isTeacher) \"\" else binding.etBatch.text.toString().trim()"
good_batch = """val batch = if (isTeacher) "" else {
            val prefix = studentId.take(2).toIntOrNull()
            if (prefix != null) (prefix - 10).toString() else ""
        }"""
content = content.replace(bad_batch, good_batch)

with open('app/src/main/java/com/shuaib/classmate/activities/RegisterActivity.kt', 'w') as f:
    f.write(content)
