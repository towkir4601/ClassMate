import re

with open('app/src/main/java/com/shuaib/classmate/fragments/FriendsFragment.kt', 'r') as f:
    content = f.read()

bad = """                    allUsersList = snapshot.documents.mapNotNull { doc ->
                        try {
                            User(
                                uid = doc.id,"""
good = """                    allUsersList = snapshot.documents.mapNotNull { doc ->
                        try {
                            val userBatch = doc.getString("batch").orEmpty()
                            val userRole = doc.getString("role") ?: "student"
                            if (currentUserRole != "superadmin" && currentUserBatch.isNotEmpty() && userBatch.isNotEmpty() && userBatch != currentUserBatch) {
                                return@mapNotNull null // Skip user if they are in a different batch and we are not superadmin
                            }
                            
                            User(
                                uid = doc.id,
                                batch = userBatch,"""

content = content.replace(bad, good)

with open('app/src/main/java/com/shuaib/classmate/fragments/FriendsFragment.kt', 'w') as f:
    f.write(content)
