import re

with open('app/src/main/java/com/shuaib/classmate/activities/PostNoticeActivity.kt', 'r') as f:
    content = f.read()

# Add currentUserBatch variable
content = content.replace("private var isAiPostingMode = false", "private var isAiPostingMode = false\n    private var currentUserBatch: String = \"\"")

# Fetch in onCreate
bad_oncreate = """        setupAttachmentPicker()

        setupEditModeIfNeeded()"""
good_oncreate = """        setupAttachmentPicker()

        auth.currentUser?.uid?.let { uid ->
            db.collection("users").document(uid).get().addOnSuccessListener { doc ->
                currentUserBatch = doc.getString("batch") ?: ""
            }
        }

        setupEditModeIfNeeded()"""
content = content.replace(bad_oncreate, good_oncreate)

# Now, add "targetBatch" to all hashMapOf calls that build noticeData.
# We will use regex to find all hashMapOf( up to "timestamp" to FieldValue.serverTimestamp() ) 
# and append "targetBatch" to targetBatchVal

def replacer(match):
    return match.group(0) + """,
            "targetBatch" to if (binding.toggleTarget.checkedButtonId == R.id.btnTargetAll) "all" else currentUserBatch"""

# The match pattern: "timestamp" to FieldValue.serverTimestamp()
pattern = r'"timestamp" to FieldValue\.serverTimestamp\(\)'
content = re.sub(pattern, replacer, content)

with open('app/src/main/java/com/shuaib/classmate/activities/PostNoticeActivity.kt', 'w') as f:
    f.write(content)
