import re

with open('app/src/main/java/com/shuaib/classmate/chat/ChatRepository.kt', 'r') as f:
    content = f.read()

bad_send = """        firestore.collection("chat_rooms").document(roomId).update(
            "lastMessage", text,
            "lastTimestamp", System.currentTimeMillis()
        )
    }"""
good_send = """        firestore.collection("chat_rooms").document(roomId).update(
            "lastMessage", text,
            "lastTimestamp", System.currentTimeMillis()
        )
        
        // Send Notification
        val targetUserId = if (roomId.startsWith("dm_")) roomId.removePrefix("dm_").split("_").firstOrNull { it != userId } else null
        com.shuaib.classmate.utils.NotificationSender.sendChatMessageAlert(
            roomId = roomId,
            senderId = userId,
            senderName = userName,
            messageText = text,
            targetUserId = targetUserId
        )
    }"""

content = content.replace(bad_send, good_send)

bad_image = """        firestore.collection("chat_rooms").document(roomId).update(
            "lastMessage", "Photo",
            "lastTimestamp", System.currentTimeMillis()
        )
    }"""
good_image = """        firestore.collection("chat_rooms").document(roomId).update(
            "lastMessage", "Photo",
            "lastTimestamp", System.currentTimeMillis()
        )
        
        // Send Notification
        val targetUserId = if (roomId.startsWith("dm_")) roomId.removePrefix("dm_").split("_").firstOrNull { it != userId } else null
        val bodyText = if (caption.isNotBlank()) "Photo: $caption" else "Photo"
        com.shuaib.classmate.utils.NotificationSender.sendChatMessageAlert(
            roomId = roomId,
            senderId = userId,
            senderName = userName,
            messageText = bodyText,
            targetUserId = targetUserId
        )
    }"""

content = content.replace(bad_image, good_image)

with open('app/src/main/java/com/shuaib/classmate/chat/ChatRepository.kt', 'w') as f:
    f.write(content)
