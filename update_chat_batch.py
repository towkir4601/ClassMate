import re

with open('app/src/main/java/com/shuaib/classmate/chat/ChatRepository.kt', 'r') as f:
    content = f.read()

bad_init = """    fun init(context: Context, userId: String, userName: String?, avatarUrl: String?) {
        if (userId.isBlank()) return
        this.context = context.applicationContext
        this.userId = userId
        this.userName = userName?.takeIf { it.isNotBlank() } ?: "User"
        this.avatarUrl = avatarUrl.orEmpty()
        
        _connectionState.value = ConnectionState.CONNECTED
        
        ensureMainGroupExists()
        getRooms()
        getUsers()
    }"""
good_init = """    var userBatch: String = ""
    val groupRoomId: String
        get() = if (userBatch.isBlank()) "group_main" else "group_batch_$userBatch"

    fun init(context: Context, userId: String, userName: String?, avatarUrl: String?) {
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
    }"""

content = content.replace(bad_init, good_init)

bad_ensure = """    private fun ensureMainGroupExists() {
        scope.launch {
            try {
                val mainGroupRef = firestore.collection("chat_rooms").document("group_main")
                val snap = mainGroupRef.get().await()
                if (!snap.exists()) {
                    mainGroupRef.set(mapOf(
                        "id" to "group_main",
                        "type" to "group",
                        "name" to "Class Group",
                        "lastMessage" to "",
                        "lastTimestamp" to System.currentTimeMillis()
                    ), SetOptions.merge())
                }
            } catch (e: Exception) {
                Log.e("ChatRepo", "Failed to init group_main", e)
            }
        }
    }"""
good_ensure = """    private fun ensureMainGroupExists() {
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
    }"""
content = content.replace(bad_ensure, good_ensure)

with open('app/src/main/java/com/shuaib/classmate/chat/ChatRepository.kt', 'w') as f:
    f.write(content)
