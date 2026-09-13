with open("app/src/main/java/com/shuaib/classmate/chat/ChatRepository.kt", "r") as f:
    content = f.read()

# Fix parseMessage
old_parse = """            imageUrl = (map["imageUrl"] as? String)?.takeIf { it.isNotBlank() },
            reactions = emptyList(),
            seenBy = (map["seenBy"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),"""

new_parse = """            imageUrl = (map["imageUrl"] as? String)?.takeIf { it.isNotBlank() },
            reactions = (map["reactions"] as? Map<*, *>)?.mapNotNull { (k, v) ->
                val emoji = k as? String ?: return@mapNotNull null
                val userIds = (v as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                if (userIds.isEmpty()) null else com.shuaib.classmate.chat.model.MessageReaction(emoji, userIds)
            } ?: emptyList(),
            seenBy = (map["seenBy"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),"""

content = content.replace(old_parse, new_parse)

# Implement reactMessage
old_react = """    fun reactMessage(messageId: String, roomId: String, emoji: String) {}"""

new_react = """    fun reactMessage(messageId: String, roomId: String, emoji: String) {
        val docRef = firestore.collection("chat_rooms").document(roomId)
            .collection("messages").document(messageId)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            if (!snapshot.exists()) return@runTransaction null
            val currentReactions = snapshot.get("reactions") as? Map<*, *> ?: emptyMap<String, Any>()
            val newReactions = mutableMapOf<String, List<String>>()
            currentReactions.forEach { (k, v) ->
                val em = k as? String ?: return@forEach
                val list = (v as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                newReactions[em] = list
            }
            val usersForEmoji = newReactions[emoji]?.toMutableList() ?: mutableListOf()
            if (usersForEmoji.contains(userId)) {
                usersForEmoji.remove(userId)
                if (usersForEmoji.isEmpty()) newReactions.remove(emoji) else newReactions[emoji] = usersForEmoji
            } else {
                usersForEmoji.add(userId)
                newReactions[emoji] = usersForEmoji
            }
            transaction.update(docRef, "reactions", newReactions)
            null
        }
    }"""

content = content.replace(old_react, new_react)

with open("app/src/main/java/com/shuaib/classmate/chat/ChatRepository.kt", "w") as f:
    f.write(content)
