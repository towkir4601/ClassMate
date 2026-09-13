import re

with open('app/src/main/java/com/shuaib/classmate/utils/NotificationSender.kt', 'r') as f:
    content = f.read()

bad = """    fun sendChatMessageAlert(
        roomId: String,
        senderId: String,
        senderName: String,
        messageText: String,
        targetUserId: String? = null,
        onFailure: (String) -> Unit = {}
    ) {
        if (roomId in ChatRepository.activeRooms) return"""
good = """    fun sendChatMessageAlert(
        roomId: String,
        senderId: String,
        senderName: String,
        messageText: String,
        targetUserId: String? = null,
        onFailure: (String) -> Unit = {}
    ) {"""
content = content.replace(bad, good)

# also check if there is any other block doing similar
with open('app/src/main/java/com/shuaib/classmate/utils/NotificationSender.kt', 'w') as f:
    f.write(content)
