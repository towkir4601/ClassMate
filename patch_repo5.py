with open("app/src/main/java/com/shuaib/classmate/chat/ChatRepository.kt", "r") as f:
    content = f.read()

old_seen = """                    snap.documents.forEach { doc ->
                        if (doc.id == currentRoomId) {
                            ChatUnreadManager.markRoomSeen(doc.id)
                        }
                    }"""

new_seen = """                    snap.documents.forEach { doc ->
                        if (doc.id == currentRoomId) {
                            val lastTime = (doc.data?.get("lastTimestamp") as? Number)?.toLong() ?: System.currentTimeMillis()
                            ChatUnreadManager.markRoomSeen(doc.id, Math.max(System.currentTimeMillis(), lastTime))
                        }
                    }"""

content = content.replace(old_seen, new_seen)

old_enter = """    fun enterRoom(roomId: String) {
        currentRoomId = roomId
        ChatUnreadManager.markRoomSeen(roomId)
        getRooms() // refresh rooms to update unread counts
    }"""
    
new_enter = """    fun enterRoom(roomId: String) {
        currentRoomId = roomId
        // We will fetch the current lastTime if we can, but getRooms will be called anyway
        ChatUnreadManager.markRoomSeen(roomId, System.currentTimeMillis() + 10000) // Give it a buffer
        getRooms() // refresh rooms to update unread counts
    }"""
content = content.replace(old_enter, new_enter)

old_leave = """    fun leaveRoom(roomId: String) {
        if (currentRoomId == roomId) currentRoomId = null
        ChatUnreadManager.markRoomSeen(roomId)
        getRooms()
    }"""
new_leave = """    fun leaveRoom(roomId: String) {
        if (currentRoomId == roomId) currentRoomId = null
        ChatUnreadManager.markRoomSeen(roomId, System.currentTimeMillis() + 10000)
        getRooms()
    }"""
content = content.replace(old_leave, new_leave)

# Wait, adding 10000ms is a hack.
# Let's do it properly. I will just pass Long.MAX_VALUE when they are IN the room.
# No, if they are NOT in the room, it shouldn't be Long.MAX_VALUE.
