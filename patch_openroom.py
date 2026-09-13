import re

with open('app/src/main/java/com/shuaib/classmate/chat/ChatViewModel.kt', 'r') as f:
    content = f.read()

bad_open = """    fun openRoom(roomId: String) {
        val changedRoom = currentRoomId != roomId
        currentRoomId = roomId
        if (changedRoom) {
            _messages.value = emptyList()
        }
        repository.enterRoom(roomId)
        repository.setRoom(roomId)
        repository.getHistory(roomId)
        repository.getPinned(roomId)
    }"""
good_open = """    fun openRoom(roomId: String) {
        val changedRoom = currentRoomId != roomId
        currentRoomId = roomId
        if (changedRoom) {
            _messages.value = repository.historyMessages.value[roomId] ?: emptyList()
        }
        repository.enterRoom(roomId)
        repository.setRoom(roomId)
        repository.getHistory(roomId)
        repository.getPinned(roomId)
    }"""
content = content.replace(bad_open, good_open)

with open('app/src/main/java/com/shuaib/classmate/chat/ChatViewModel.kt', 'w') as f:
    f.write(content)
