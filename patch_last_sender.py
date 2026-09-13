with open("app/src/main/java/com/shuaib/classmate/chat/model/ChatRoom.kt", "r") as f:
    content = f.read()

if "val lastSenderId: String = \"\"" not in content:
    content = content.replace("val lastMessageTime: Long = 0L,", "val lastMessageTime: Long = 0L,\n    val lastSenderId: String = \"\",")

with open("app/src/main/java/com/shuaib/classmate/chat/model/ChatRoom.kt", "w") as f:
    f.write(content)

with open("app/src/main/java/com/shuaib/classmate/chat/ChatRepository.kt", "r") as f:
    content = f.read()

# Update parseRoom
if 'lastSenderId = map["lastSenderId"] as? String ?: "",' not in content:
    content = content.replace("lastMessageTime = (map[\"lastTimestamp\"] as? Number)?.toLong() ?: 0L",
                              "lastMessageTime = (map[\"lastTimestamp\"] as? Number)?.toLong() ?: 0L,\n            lastSenderId = map[\"lastSenderId\"] as? String ?: \"\"")

# Update sendMessage
if '"lastSenderId" to userId' not in content:
    content = content.replace('"lastMessage" to text,', '"lastMessage" to text,\n            "lastSenderId" to userId,')
    content = content.replace('"lastMessage" to if (caption.isNotBlank()) caption else "Sent an image",',
                              '"lastMessage" to if (caption.isNotBlank()) caption else "Sent an image",\n            "lastSenderId" to userId,')
    content = content.replace('"lastMessage" to "Sent a file",',
                              '"lastMessage" to "Sent a file",\n            "lastSenderId" to userId,')
    content = content.replace('"lastMessage" to "Sent an audio message",',
                              '"lastMessage" to "Sent an audio message",\n            "lastSenderId" to userId,')
    content = content.replace('"lastMessage" to "Sent a video",',
                              '"lastMessage" to "Sent a video",\n            "lastSenderId" to userId,')

with open("app/src/main/java/com/shuaib/classmate/chat/ChatRepository.kt", "w") as f:
    f.write(content)

# Update ChatUnreadManager
with open("app/src/main/java/com/shuaib/classmate/chat/ChatUnreadManager.kt", "r") as f:
    content = f.read()

if "if (room.lastSenderId" not in content:
    old_getTotal = """    fun getTotalUnread(rooms: List<com.shuaib.classmate.chat.model.ChatRoom>): Int {
        var count = 0
        for (room in rooms) {
            if (getUnreadCount(room.id, room.lastMessageTime) > 0) {
                count++
            }
        }
        return count
    }"""
    new_getTotal = """    fun getTotalUnread(rooms: List<com.shuaib.classmate.chat.model.ChatRoom>, currentUserId: String): Int {
        var count = 0
        for (room in rooms) {
            if (room.lastSenderId == currentUserId) continue
            if (getUnreadCount(room.id, room.lastMessageTime) > 0) {
                count++
            }
        }
        return count
    }"""
    content = content.replace(old_getTotal, new_getTotal)

with open("app/src/main/java/com/shuaib/classmate/chat/ChatUnreadManager.kt", "w") as f:
    f.write(content)

# Update MainActivity.kt
with open("app/src/main/java/com/shuaib/classmate/activities/MainActivity.kt", "r") as f:
    content = f.read()

if "getTotalUnread(rooms, auth.currentUser?.uid" not in content:
    content = content.replace("getTotalUnread(rooms)", "getTotalUnread(rooms, auth.currentUser?.uid ?: \"\")")

with open("app/src/main/java/com/shuaib/classmate/activities/MainActivity.kt", "w") as f:
    f.write(content)
