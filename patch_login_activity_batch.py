import re

with open('app/src/main/java/com/shuaib/classmate/activities/LoginActivity.kt', 'r') as f:
    content = f.read()

# Fix identifyUserAndNavigate signature
bad_sig = "private fun identifyUserAndNavigate(uid: String, role: String) {"
good_sig = "private fun identifyUserAndNavigate(uid: String, role: String, batch: String = \"\") {"
content = content.replace(bad_sig, good_sig)

# Fix identifyUserAndNavigate body
bad_body = """            OneSignal.User.addTag("uid", uid)
            val batch = document.getString("batch") ?: ""
            if (batch.isNotBlank()) {"""
good_body = """            OneSignal.User.addTag("uid", uid)
            if (batch.isNotBlank()) {"""
content = content.replace(bad_body, good_body)

# Fix calls to identifyUserAndNavigate inside checkUserProfile
# Call 1
bad_call1 = """                        identifyUserAndNavigate(uid, "student")"""
good_call1 = """                        identifyUserAndNavigate(uid, "student", "")"""
content = content.replace(bad_call1, good_call1)

# Before Call 2 and 3, we need to extract batch from existingData
# Wait, let's just do a blanket replace for `identifyUserAndNavigate(uid, role)` -> `identifyUserAndNavigate(uid, role, existingData["batch"] as? String ?: "")`
# Wait, we need `existingData` to be defined!

with open('app/src/main/java/com/shuaib/classmate/activities/LoginActivity.kt', 'w') as f:
    f.write(content)
