import re

with open("app/src/main/java/com/shuaib/classmate/chat/ChatRepository.kt", "r") as f:
    content = f.read()

# Add currentRoomId
if "private var currentRoomId: String? = null" not in content:
    content = content.replace("private var userName: String = \"User\"", 
                              "private var userName: String = \"User\"\n    private var currentRoomId: String? = null")

# Update enterRoom and leaveRoom
old_enter = """fun enterRoom(roomId: String) {
        ChatUnreadManager.markRoomSeen(roomId)
        getRooms() // refresh rooms to update unread counts"""
new_enter = """fun enterRoom(roomId: String) {
        currentRoomId = roomId
        ChatUnreadManager.markRoomSeen(roomId)
        getRooms() // refresh rooms to update unread counts"""
content = content.replace(old_enter, new_enter)

old_leave = """fun leaveRoom(roomId: String) {"""
new_leave = """fun leaveRoom(roomId: String) {
        if (currentRoomId == roomId) currentRoomId = null
        ChatUnreadManager.markRoomSeen(roomId)
        getRooms()"""
content = content.replace(old_leave, new_leave)

# In getRooms, if room.id == currentRoomId, mark it seen!
old_get = """                    val allRooms = snap.documents.mapNotNull { parseRoom(it.data) }"""
new_get = """                    val allRooms = snap.documents.mapNotNull { parseRoom(it.data) }
                    currentRoomId?.let { cid -> 
                        ChatUnreadManager.markRoomSeen(cid) 
                        // Update the current room's unread count to 0 in memory since we just marked it seen
                        allRooms.find { it.id == cid }?.let { it -> 
                            // It's a data class, we'd need to copy, but it's simpler to just map it
                        }
                    }"""
# Let's just modify the list in place
new_get2 = """                    snap.documents.forEach { doc ->
                        if (doc.id == currentRoomId) {
                            ChatUnreadManager.markRoomSeen(doc.id)
                        }
                    }
                    val allRooms = snap.documents.mapNotNull { parseRoom(it.data) }"""
content = content.replace("val allRooms = snap.documents.mapNotNull { parseRoom(it.data) }", new_get2)

with open("app/src/main/java/com/shuaib/classmate/chat/ChatRepository.kt", "w") as f:
    f.write(content)
