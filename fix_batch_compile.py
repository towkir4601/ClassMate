import re

# Fix CreatePollActivity
with open('app/src/main/java/com/shuaib/classmate/activities/CreatePollActivity.kt', 'r') as f:
    content = f.read()

bad_vars = """    private var currentUserName = ""
    private var pollId: String? = null"""
good_vars = """    private var currentUserName = ""
    private var currentUserBatch = ""
    private var pollId: String? = null"""
content = content.replace(bad_vars, good_vars)

bad_fetch = """        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            currentUserName = doc.getString("name") ?: ""
        }"""
good_fetch = """        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            currentUserName = doc.getString("name") ?: ""
            currentUserBatch = doc.getString("batch") ?: ""
        }"""
content = content.replace(bad_fetch, good_fetch)

# Just in case my previous patch failed and there's another fetch block:
bad_fetch2 = """        if (currentUserId != null) {
            db.collection("users").document(currentUserId).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        currentUserName = doc.getString("name") ?: "User"
                    }
                }
        }"""
content = content.replace(bad_fetch2, good_fetch)

with open('app/src/main/java/com/shuaib/classmate/activities/CreatePollActivity.kt', 'w') as f:
    f.write(content)

# Fix PostNoticeActivity
with open('app/src/main/java/com/shuaib/classmate/activities/PostNoticeActivity.kt', 'r') as f:
    content2 = f.read()

bad_vars2 = """    private var isAiPostingMode = false"""
good_vars2 = """    private var isAiPostingMode = false
    private var currentUserBatch = \"\""""
if "private var currentUserBatch" not in content2:
    content2 = content2.replace(bad_vars2, good_vars2)

with open('app/src/main/java/com/shuaib/classmate/activities/PostNoticeActivity.kt', 'w') as f:
    f.write(content2)
