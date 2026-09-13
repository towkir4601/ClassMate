with open("app/src/main/java/com/shuaib/classmate/chat/ChatRepository.kt", "r") as f:
    content = f.read()

old_filter = """                    val allRooms = snap.documents.mapNotNull { parseRoom(it.data) }
                    val userRooms = allRooms.filter { 
                        it.type == "group" || it.member1Id == userId || it.member2Id == userId 
                    }.sortedByDescending { it.lastMessageTime }"""

new_filter = """                    val allRooms = snap.documents.mapNotNull { parseRoom(it.data) }
                    val currentUserRole = _users.value.find { it.id == userId }?.role ?: "student"
                    val isTeacher = currentUserRole == "teacher"
                    val userRooms = allRooms.filter { 
                        (it.type == "group" && !isTeacher) || it.member1Id == userId || it.member2Id == userId 
                    }.sortedByDescending { it.lastMessageTime }"""

content = content.replace(old_filter, new_filter)

with open("app/src/main/java/com/shuaib/classmate/chat/ChatRepository.kt", "w") as f:
    f.write(content)
