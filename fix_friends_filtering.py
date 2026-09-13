import re

with open('app/src/main/java/com/shuaib/classmate/fragments/FriendsFragment.kt', 'r') as f:
    content = f.read()

bad = """                            val userRole = doc.getString("role") ?: "student"

                            
                            User("""
good = """                            val userRole = doc.getString("role") ?: "student"

                            if (isTeacherMode && userRole != "teacher") {
                                return@mapNotNull null
                            }
                            if (!isTeacherMode && userRole == "teacher") {
                                return@mapNotNull null
                            }
                            
                            User("""
content = content.replace(bad, good)

with open('app/src/main/java/com/shuaib/classmate/fragments/FriendsFragment.kt', 'w') as f:
    f.write(content)
