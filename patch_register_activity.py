import re

with open('app/src/main/java/com/shuaib/classmate/activities/RegisterActivity.kt', 'r') as f:
    content = f.read()

bad1 = "private fun identifyUserInOneSignal(uid: String) {"
good1 = "private fun identifyUserInOneSignal(uid: String, batch: String) {"
content = content.replace(bad1, good1)

bad2 = """            OneSignal.User.addTag("uid", uid)
            val batch = binding.dropdownBatch.text.toString()
            if (batch.isNotBlank()) {"""
good2 = """            OneSignal.User.addTag("uid", uid)
            if (batch.isNotBlank()) {"""
content = content.replace(bad2, good2)

bad3 = "identifyUserInOneSignal(uid)"
good3 = "identifyUserInOneSignal(uid, binding.etBatch.text.toString().trim())"
content = content.replace(bad3, good3)

with open('app/src/main/java/com/shuaib/classmate/activities/RegisterActivity.kt', 'w') as f:
    f.write(content)
