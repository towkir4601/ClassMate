import re

with open('app/src/main/java/com/shuaib/classmate/activities/PostNoticeActivity.kt', 'r') as f:
    content = f.read()

bad_fetch = """        // Check if teacher
        auth.currentUser?.uid?.let { uid ->
            db.collection("users").document(uid).get().addOnSuccessListener { doc ->
                if (doc.getString("role") == "teacher") {
                    binding.rbNormal.isVisible = false
                    binding.rbPoll.isVisible = false
                    binding.rbVacation.isVisible = false
                    binding.rgNoticeType.check(R.id.rbCancel)
                }
            }
        }"""
good_fetch = """        // Check if teacher
        auth.currentUser?.uid?.let { uid ->
            db.collection("users").document(uid).get().addOnSuccessListener { doc ->
                currentUserBatch = doc.getString("batch") ?: ""
                if (doc.getString("role") == "teacher") {
                    binding.rbNormal.isVisible = false
                    binding.rbPoll.isVisible = false
                    binding.rbVacation.isVisible = false
                    binding.rgNoticeType.check(R.id.rbCancel)
                }
            }
        }"""
content = content.replace(bad_fetch, good_fetch)

with open('app/src/main/java/com/shuaib/classmate/activities/PostNoticeActivity.kt', 'w') as f:
    f.write(content)
