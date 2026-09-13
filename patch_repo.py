import re

with open("app/src/main/java/com/shuaib/classmate/chat/ChatRepository.kt", "r") as f:
    content = f.read()

# 1. Initialize in init()
content = content.replace("fun init(context: Context, userId: String, userName: String?, avatarUrl: String?) {", 
                          "fun init(context: Context, userId: String, userName: String?, avatarUrl: String?) {\n        ChatUnreadManager.init(context)")

# 2. Update parseRoom to include unreadCount
old_parse = """            otherUserAvatar = oAvatar,
            lastMessage = map["lastMessage"] as? String ?: "",
            lastMessageTime = (map["lastTimestamp"] as? Number)?.toLong() ?: 0L
        )
    }"""
new_parse = """            otherUserAvatar = oAvatar,
            lastMessage = map["lastMessage"] as? String ?: "",
            lastMessageTime = (map["lastTimestamp"] as? Number)?.toLong() ?: 0L
        ).apply {
            // Can't mutate data class val easily, but wait, let's change unreadCount in the data class instantiation
        }
    }"""
    
# Wait, let's just do it directly in parseRoom:
old_parse_full = """            otherUserId = oId,
            otherUserName = oName,
            otherUserAvatar = oAvatar,
            lastMessage = map["lastMessage"] as? String ?: "",
            lastMessageTime = (map["lastTimestamp"] as? Number)?.toLong() ?: 0L
        )
    }"""

new_parse_full = """            otherUserId = oId,
            otherUserName = oName,
            otherUserAvatar = oAvatar,
            lastMessage = map["lastMessage"] as? String ?: "",
            lastMessageTime = (map["lastTimestamp"] as? Number)?.toLong() ?: 0L
        )
        val finalLastTime = room.lastMessageTime
        return room.copy(unreadCount = ChatUnreadManager.getUnreadCount(room.id, finalLastTime))
    }"""

# But wait! I can just put `unreadCount = ChatUnreadManager.getUnreadCount(map["id"] as? String ?: "", (map["lastTimestamp"] as? Number)?.toLong() ?: 0L)` in ChatRoom construction!
old_construct = """            lastMessage = map["lastMessage"] as? String ?: "",
            lastMessageTime = (map["lastTimestamp"] as? Number)?.toLong() ?: 0L
        )"""
new_construct = """            lastMessage = map["lastMessage"] as? String ?: "",
            lastMessageTime = (map["lastTimestamp"] as? Number)?.toLong() ?: 0L,
            unreadCount = ChatUnreadManager.getUnreadCount(map["id"] as? String ?: "", (map["lastTimestamp"] as? Number)?.toLong() ?: 0L)
        )"""

content = content.replace(old_construct, new_construct)

# 3. Enter room marks as seen
old_enter = "fun enterRoom(roomId: String) {"
new_enter = "fun enterRoom(roomId: String) {\n        ChatUnreadManager.markRoomSeen(roomId)\n        getRooms() // refresh rooms to update unread counts"

content = content.replace(old_enter, new_enter)

# 4. Message seen tracking
old_get_rooms = """                    _rooms.value = userRooms
                    scope.launch { _roomsLoaded.emit(Unit) }"""
new_get_rooms = """                    val totalUnread = ChatUnreadManager.getTotalUnread(userRooms)
                    _rooms.value = userRooms
                    // We can emit the total unread somewhere or MainActivity can observe _rooms
                    scope.launch { _roomsLoaded.emit(Unit) }"""
content = content.replace(old_get_rooms, new_get_rooms)


with open("app/src/main/java/com/shuaib/classmate/chat/ChatRepository.kt", "w") as f:
    f.write(content)
