import re

with open('app/src/main/java/com/shuaib/classmate/activities/CreatePollActivity.kt', 'r') as f:
    content = f.read()

# Add currentUserBatch variable
content = content.replace("private var currentUserName = \"User\"", "private var currentUserName = \"User\"\n    private var currentUserBatch = \"\"")

# Fetch batch
bad_oncreate = """        if (currentUserId != null) {
            db.collection("users").document(currentUserId).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        currentUserName = doc.getString("name") ?: "User"
                    }
                }
        }"""
good_oncreate = """        if (currentUserId != null) {
            db.collection("users").document(currentUserId).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        currentUserName = doc.getString("name") ?: "User"
                        currentUserBatch = doc.getString("batch") ?: ""
                    }
                }
        }"""
content = content.replace(bad_oncreate, good_oncreate)

# Add targetBatch to pollData
bad_pollData = """        val pollData = hashMapOf(
            "question" to question,
            "options" to validOptions,
            "createdBy" to currentUserName,
            "expiresAt" to expiresAt,
            "isActive" to true,
            "allowMultipleAnswers" to binding.switchMultipleAnswers.isChecked
        )"""
good_pollData = """        val pollData = hashMapOf<String, Any>(
            "question" to question,
            "options" to validOptions,
            "createdBy" to currentUserName,
            "expiresAt" to expiresAt,
            "isActive" to true,
            "allowMultipleAnswers" to binding.switchMultipleAnswers.isChecked,
            "targetBatch" to if (binding.toggleTarget.checkedButtonId == R.id.btnTargetAll) "all" else currentUserBatch
        )"""
content = content.replace(bad_pollData, good_pollData)

with open('app/src/main/java/com/shuaib/classmate/activities/CreatePollActivity.kt', 'w') as f:
    f.write(content)
