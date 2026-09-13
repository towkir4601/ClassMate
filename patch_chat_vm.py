import re

with open('app/src/main/java/com/shuaib/classmate/chat/ChatViewModel.kt', 'r') as f:
    content = f.read()

bad_preview = """        _rooms.value = if (updated.any { it.id == message.roomId }) {
            updated
        } else {
            current + ChatRoom(
                id = message.roomId,
                type = if (message.roomId == "group_main") "group" else "dm",
                name = if (message.roomId == "group_main") "Class Group" else "",
                member1Id = "",
                member2Id = "",
                lastMessage = preview,
                lastMessageTime = message.timestamp
            )
        }"""
good_preview = """        _rooms.value = if (updated.any { it.id == message.roomId }) {
            updated
        } else {
            val isGroup = message.roomId.startsWith("group_")
            current + ChatRoom(
                id = message.roomId,
                type = if (isGroup) "group" else "dm",
                name = if (isGroup) "Class Group" else "",
                member1Id = "",
                member2Id = "",
                lastMessage = preview,
                lastMessageTime = message.timestamp
            )
        }"""
content = content.replace(bad_preview, good_preview)

with open('app/src/main/java/com/shuaib/classmate/chat/ChatViewModel.kt', 'w') as f:
    f.write(content)
