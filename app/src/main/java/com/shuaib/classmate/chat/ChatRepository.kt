package com.shuaib.classmate.chat

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.shuaib.classmate.chat.model.ChatMessage
import com.shuaib.classmate.chat.model.ChatRoom
import com.shuaib.classmate.chat.model.ChatUser
import com.shuaib.classmate.models.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

object ChatRepository {
    fun isTeacher(): Boolean {
        val currentUserRole = _users.value.find { it.id == userId }?.role ?: "student"
        return currentUserRole == "teacher"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val firestore = FirebaseFirestore.getInstance()

    private var context: Context? = null
    private var userId: String = ""
    private var userName: String = "User"
    private var currentRoomId: String? = null
    private var avatarUrl: String = ""

    val activeRooms = mutableSetOf<String>()

    private val _incomingMessage = MutableStateFlow<ChatMessage?>(null)
    val incomingMessage: StateFlow<ChatMessage?> = _incomingMessage

    private val _typingEvent = MutableSharedFlow<TypingEvent>(extraBufferCapacity = 8)
    val typingEvent: SharedFlow<TypingEvent> = _typingEvent

    private val _dmCreated = MutableSharedFlow<ChatRoom>(extraBufferCapacity = 8)
    val dmCreated: SharedFlow<ChatRoom> = _dmCreated

    private val _historyMessages = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())
    val historyMessages: StateFlow<Map<String, List<ChatMessage>>> = _historyMessages

    private val _messageUpdate = MutableSharedFlow<ChatMessage>(extraBufferCapacity = 16)
    val messageUpdate: SharedFlow<ChatMessage> = _messageUpdate

    private val _searchResults = MutableStateFlow<List<ChatMessage>>(emptyList())
    val searchResults: StateFlow<List<ChatMessage>> = _searchResults

    private val _pinnedMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val pinnedMessages: StateFlow<List<ChatMessage>> = _pinnedMessages

    private val _onlineUsers = MutableStateFlow<List<String>>(emptyList())
    val onlineUsers: StateFlow<List<String>> = _onlineUsers

    private val _rooms = MutableStateFlow<List<ChatRoom>>(emptyList())
    val rooms: StateFlow<List<ChatRoom>> = _rooms

    private val _users = MutableStateFlow<List<ChatUser>>(emptyList())
    val users: StateFlow<List<ChatUser>> = _users

    private val _historyLoaded = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val historyLoaded: SharedFlow<String> = _historyLoaded

    private val _roomsLoaded = MutableSharedFlow<Unit>(extraBufferCapacity = 8)
    val roomsLoaded: SharedFlow<Unit> = _roomsLoaded

    private val _usersLoaded = MutableSharedFlow<Unit>(extraBufferCapacity = 8)
    val usersLoaded: SharedFlow<Unit> = _usersLoaded

    private val _connectionError = MutableSharedFlow<Unit>(extraBufferCapacity = 8)
    val connectionError: SharedFlow<Unit> = _connectionError

    private val _connectionState = MutableStateFlow(ConnectionState.CONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private var roomsListener: ListenerRegistration? = null
    private var usersListener: ListenerRegistration? = null
    private val messagesListeners = mutableMapOf<String, ListenerRegistration>()

    var userBatch: String = ""
    val groupRoomId: String
        get() = if (userBatch.isBlank()) "group_main" else "group_batch_$userBatch"

    fun init(context: Context, userId: String, userName: String?, avatarUrl: String?) {
        ChatUnreadManager.init(context)
        if (userId.isBlank()) return
        this.context = context.applicationContext
        this.userId = userId
        this.userName = userName?.takeIf { it.isNotBlank() } ?: "User"
        this.avatarUrl = avatarUrl.orEmpty()
        
        _connectionState.value = ConnectionState.CONNECTED
        
        firestore.collection("users").document(userId).get().addOnSuccessListener { doc ->
            userBatch = doc.getString("batch") ?: ""
            ensureMainGroupExists()
            getRooms()
            getUsers()
        }
    }

    private fun ensureMainGroupExists() {
        scope.launch {
            try {
                val mainGroupRef = firestore.collection("chat_rooms").document(groupRoomId)
                val snap = mainGroupRef.get().await()
                if (!snap.exists()) {
                    mainGroupRef.set(mapOf(
                        "id" to groupRoomId,
                        "type" to "group",
                        "name" to "Class Group (${if(userBatch.isNotBlank()) "Batch $userBatch" else "All"})",
                        "lastMessage" to "",
                        "lastTimestamp" to System.currentTimeMillis()
                    ), SetOptions.merge())
                }
            } catch (e: Exception) {
                Log.e("ChatRepo", "Failed to init $groupRoomId", e)
            }
        }
    }

    fun close() {
        roomsListener?.remove()
        usersListener?.remove()
        messagesListeners.values.forEach { it.remove() }
        messagesListeners.clear()
        _connectionState.value = ConnectionState.DISCONNECTED
    }
    
    fun connect() {
        _connectionState.value = ConnectionState.CONNECTED
    }

    fun updateServerUrl(newUrl: String) {}

    fun sendMessage(
        roomId: String,
        text: String,
        replyToId: String? = null,
        replyToText: String? = null,
        replyToSender: String? = null,
        forwardedFrom: String? = null
    ) {
        val msgId = UUID.randomUUID().toString()
        val msg = mapOf(
            "id" to msgId,
            "roomId" to roomId,
            "senderId" to userId,
            "senderName" to userName,
            "senderAvatarUrl" to avatarUrl,
            "text" to text,
            "timestamp" to System.currentTimeMillis(),
            "isDeleted" to false,
            "replyToId" to replyToId,
            "replyToText" to replyToText,
            "replyToSender" to replyToSender,
            "forwardedFrom" to forwardedFrom
        )
        firestore.collection("chat_rooms").document(roomId).collection("messages").document(msgId).set(msg)
        firestore.collection("chat_rooms").document(roomId).update(
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
    }

    fun sendImage(roomId: String, imageUrl: String, caption: String) {
        val msgId = UUID.randomUUID().toString()
        val msg = mapOf(
            "id" to msgId,
            "roomId" to roomId,
            "senderId" to userId,
            "senderName" to userName,
            "senderAvatarUrl" to avatarUrl,
            "text" to caption,
            "imageUrl" to imageUrl,
            "timestamp" to System.currentTimeMillis(),
            "isDeleted" to false
        )
        firestore.collection("chat_rooms").document(roomId).collection("messages").document(msgId).set(msg)
        firestore.collection("chat_rooms").document(roomId).update(
            "lastMessage", if (caption.isNotBlank()) caption else "Sent an image",
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
    }

    fun enterRoom(roomId: String) {
        currentRoomId = roomId
        ChatUnreadManager.markRoomSeen(roomId)
        getRooms() // refresh rooms to update unread counts
        activeRooms.add(roomId)
    }
    
    fun leaveRoom(roomId: String) {
        if (currentRoomId == roomId) currentRoomId = null
        ChatUnreadManager.markRoomSeen(roomId)
        getRooms()
        activeRooms.remove(roomId)
    }
    fun setRoom(roomId: String) {}

    fun getHistory(roomId: String) {
        if (messagesListeners.containsKey(roomId)) return
        
        messagesListeners[roomId] = firestore.collection("chat_rooms").document(roomId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snap, _ ->
                if (snap != null) {
                    val msgs = snap.documents.mapNotNull { parseMessage(it.data) }
                    val newMap = _historyMessages.value.toMutableMap()
                    newMap[roomId] = msgs.reversed()
                    _historyMessages.value = newMap
                    
                    if (msgs.isNotEmpty()) {
                        _incomingMessage.value = msgs.first()
                    }

                    val batch = firestore.batch()
                    var batchCount = 0
                    msgs.forEach { msg ->
                        if (msg.senderId != userId && !msg.seenBy.contains(userId)) {
                            val ref = firestore.collection("chat_rooms").document(roomId).collection("messages").document(msg.id)
                            batch.update(ref, "seenBy", com.google.firebase.firestore.FieldValue.arrayUnion(userId))
                            batchCount++
                        }
                    }
                    if (batchCount > 0) batch.commit()

                    scope.launch { _historyLoaded.emit(roomId) }
                }
            }
    }

    fun getRooms() {
        roomsListener?.remove()
        roomsListener = firestore.collection("chat_rooms")
            .addSnapshotListener { snap, _ ->
                if (snap != null) {
                                        snap.documents.forEach { doc ->
                        if (doc.id == currentRoomId) {
                            ChatUnreadManager.markRoomSeen(doc.id)
                        }
                    }
                    val allRooms = snap.documents.mapNotNull { parseRoom(it.data) }
                    val currentUserRole = _users.value.find { it.id == userId }?.role ?: "student"
                    val isTeacher = currentUserRole == "teacher"
                    val userRooms = allRooms.filter { 
                        (it.type == "group" && !isTeacher) || it.member1Id == userId || it.member2Id == userId 
                    }.sortedByDescending { it.lastMessageTime }
                    
                    val totalUnread = ChatUnreadManager.getTotalUnread(userRooms, userId)
                    _rooms.value = userRooms
                    // We can emit the total unread somewhere or MainActivity can observe _rooms
                    scope.launch { _roomsLoaded.emit(Unit) }
                }
            }
    }

    fun createDm(targetUserId: String, targetUserName: String) {
        val ids = listOf(userId, targetUserId).sorted()
        val roomId = "dm_${ids[0]}_${ids[1]}"
        
        val roomData = mapOf(
            "id" to roomId,
            "type" to "direct",
            "name" to "",
            "member1Id" to ids[0],
            "member2Id" to ids[1],
            "lastMessage" to "",
            "lastTimestamp" to System.currentTimeMillis()
        )
        
        firestore.collection("chat_rooms").document(roomId).set(roomData, SetOptions.merge())
            .addOnSuccessListener {
                val dmRoom = parseRoom(roomData)
                if (dmRoom != null) {
                    scope.launch { _dmCreated.emit(dmRoom) }
                }
            }
    }

    fun deleteMessage(messageId: String, roomId: String, isAdmin: Boolean) {
        firestore.collection("chat_rooms").document(roomId).collection("messages").document(messageId)
            .update(
                "isDeleted", true,
                "text", "",
                "imageUrl", ""
            ).addOnSuccessListener {
                firestore.collection("chat_rooms").document(roomId).collection("messages")
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(1)
                    .get()
                    .addOnSuccessListener { snap ->
                        val lastDoc = snap.documents.firstOrNull()
                        if (lastDoc != null) {
                            val isDeleted = lastDoc.getBoolean("isDeleted") ?: false
                            val text = if (isDeleted) "Message was deleted" else (lastDoc.getString("text") ?: "")
                            val caption = lastDoc.getString("imageUrl")
                            val finalText = if (text.isBlank() && !caption.isNullOrBlank()) "Sent an image" else text
                            firestore.collection("chat_rooms").document(roomId).update(
                                "lastMessage", finalText
                            )
                        }
                    }
            }
    }

    fun getUsers() {
        usersListener?.remove()
        usersListener = firestore.collection("users")
            .addSnapshotListener { snap, _ ->
                if (snap != null) {
                    val chatUsers = snap.documents.mapNotNull { doc ->
                        try {
                            val u = doc.toObject(User::class.java) ?: return@mapNotNull null
                            ChatUser(
                                id = u.uid,
                                name = u.name.ifBlank { u.fullName },
                                avatarUrl = u.photoUrl,
                                role = u.role,
                                isOnline = u.isOnline
                            )
                        } catch (e: Exception) {
                            Log.e("ChatRepo", "Error parsing user ${doc.id}", e)
                            null
                        }
                    }
                    _users.value = chatUsers
                    _onlineUsers.value = chatUsers.filter { it.isOnline }.map { it.id }
                    scope.launch { _usersLoaded.emit(Unit) }
                    
                    // Re-parse rooms to update avatars/names if they were missing
                    val currentRooms = _rooms.value
                    if (currentRooms.isNotEmpty()) {
                        val updatedRooms = currentRooms.map { room ->
                            if (room.type == "direct") {
                                val oId = if (room.member1Id == userId) room.member2Id else room.member1Id
                                val oUser = chatUsers.find { it.id == oId }
                                room.copy(
                                    otherUserName = oUser?.name ?: "User",
                                    otherUserAvatar = oUser?.avatarUrl ?: ""
                                )
                            } else room
                        }
                        _rooms.value = updatedRooms
                    }
                }
            }
    }

    fun sendTyping(roomId: String) {}
    
    fun editMessage(messageId: String, roomId: String, newText: String) {}
    fun reactMessage(messageId: String, roomId: String, emoji: String) {
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
    }
    fun seenMessage(messageId: String, roomId: String) {}
    fun forwardMessage(originalMessageId: String, targetRoomId: String) {}
    fun pinMessage(messageId: String, roomId: String, pin: Boolean) {}
    fun getPinned(roomId: String) {}
    fun searchMessages(roomId: String, query: String) {}
    
    fun dmRoomIdFor(targetUserId: String): String {
        val ids = listOf(userId, targetUserId).sorted()
        return "dm_${ids[0]}_${ids[1]}"
    }
    
    private fun parseMessage(map: Map<String, Any>?): ChatMessage? {
        if (map == null) return null
        return ChatMessage(
            id = map["id"] as? String ?: return null,
            roomId = map["roomId"] as? String ?: "",
            senderId = map["senderId"] as? String ?: "",
            senderName = map["senderName"] as? String ?: "User",
            senderAvatarUrl = map["senderAvatarUrl"] as? String ?: "",
            text = map["text"] as? String ?: "",
            timestamp = (map["timestamp"] as? Number)?.toLong() ?: 0L,
            isDeleted = map["isDeleted"] as? Boolean ?: false,
            replyToId = map["replyToId"] as? String,
            replyToText = map["replyToText"] as? String,
            replyToSender = map["replyToSender"] as? String,
            forwardedFrom = map["forwardedFrom"] as? String,
            imageUrl = (map["imageUrl"] as? String)?.takeIf { it.isNotBlank() },
            reactions = (map["reactions"] as? Map<*, *>)?.mapNotNull { (k, v) ->
                val emoji = k as? String ?: return@mapNotNull null
                val userIds = (v as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                if (userIds.isEmpty()) null else com.shuaib.classmate.chat.model.MessageReaction(emoji, userIds)
            } ?: emptyList(),
            seenBy = (map["seenBy"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            isPinned = false
        )
    }

    private fun parseRoom(map: Map<String, Any>?): ChatRoom? {
        if (map == null) return null
        val type = map["type"] as? String ?: "direct"
        val m1 = map["member1Id"] as? String ?: ""
        val m2 = map["member2Id"] as? String ?: ""
        
        var oId = ""
        var oName = ""
        var oAvatar = ""
        
        if (type == "direct") {
            oId = if (m1 == userId) m2 else m1
            val oUser = _users.value.find { it.id == oId }
            oName = oUser?.name ?: "User"
            oAvatar = oUser?.avatarUrl ?: ""
        }
        
        return ChatRoom(
            id = map["id"] as? String ?: return null,
            type = type,
            name = map["name"] as? String ?: "",
            member1Id = m1,
            member2Id = m2,
            otherUserId = oId,
            otherUserName = oName,
            otherUserAvatar = oAvatar,
            lastMessage = map["lastMessage"] as? String ?: "",
            lastMessageTime = (map["lastTimestamp"] as? Number)?.toLong() ?: 0L,
            lastSenderId = map["lastSenderId"] as? String ?: "",
            unreadCount = ChatUnreadManager.getUnreadCount(map["id"] as? String ?: "", (map["lastTimestamp"] as? Number)?.toLong() ?: 0L)
        )
    }
}
