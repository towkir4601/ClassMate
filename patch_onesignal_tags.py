import re

# LoginActivity
with open('app/src/main/java/com/shuaib/classmate/activities/LoginActivity.kt', 'r') as f:
    content = f.read()

bad = """            OneSignal.User.addTag("role", role)
            OneSignal.User.addTag("uid", uid)"""
good = """            OneSignal.User.addTag("role", role)
            OneSignal.User.addTag("uid", uid)
            val batch = document.getString("batch") ?: ""
            if (batch.isNotBlank()) {
                OneSignal.User.addTag("batch", batch)
            }"""
content = content.replace(bad, good)
with open('app/src/main/java/com/shuaib/classmate/activities/LoginActivity.kt', 'w') as f:
    f.write(content)

# RegisterActivity
with open('app/src/main/java/com/shuaib/classmate/activities/RegisterActivity.kt', 'r') as f:
    content = f.read()

bad2 = """            OneSignal.User.addTag("role", "student")
            OneSignal.User.addTag("uid", uid)"""
good2 = """            OneSignal.User.addTag("role", "student")
            OneSignal.User.addTag("uid", uid)
            val batch = binding.dropdownBatch.text.toString()
            if (batch.isNotBlank()) {
                OneSignal.User.addTag("batch", batch)
            }"""
content = content.replace(bad2, good2)
with open('app/src/main/java/com/shuaib/classmate/activities/RegisterActivity.kt', 'w') as f:
    f.write(content)
