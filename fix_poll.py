import re

with open('app/src/main/java/com/shuaib/classmate/activities/CreatePollActivity.kt', 'r') as f:
    content = f.read()

bad = """        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                currentUserName = doc.getString("name") ?: "Admin"
            }"""
good = """        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                currentUserName = doc.getString("name") ?: "Admin"
                currentUserBatch = doc.getString("batch") ?: ""
            }"""
content = content.replace(bad, good)

with open('app/src/main/java/com/shuaib/classmate/activities/CreatePollActivity.kt', 'w') as f:
    f.write(content)
