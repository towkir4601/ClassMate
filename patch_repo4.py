with open("app/src/main/java/com/shuaib/classmate/chat/ChatRepository.kt", "r") as f:
    content = f.read()

new_fun = """
    fun isTeacher(): Boolean {
        val currentUserRole = _users.value.find { it.id == userId }?.role ?: "student"
        return currentUserRole == "teacher"
    }
"""

if "fun isTeacher(): Boolean" not in content:
    content = content.replace("object ChatRepository {", "object ChatRepository {" + new_fun)

with open("app/src/main/java/com/shuaib/classmate/chat/ChatRepository.kt", "w") as f:
    f.write(content)
