with open("app/src/main/java/com/shuaib/classmate/chat/ChatRepository.kt", "r") as f:
    content = f.read()

old_users = """                    }
                    _users.value = chatUsers
                    scope.launch { _usersLoaded.emit(Unit) }"""
new_users = """                    }
                    _users.value = chatUsers
                    _onlineUsers.value = chatUsers.filter { it.isOnline }.map { it.id }
                    scope.launch { _usersLoaded.emit(Unit) }"""
content = content.replace(old_users, new_users)

with open("app/src/main/java/com/shuaib/classmate/chat/ChatRepository.kt", "w") as f:
    f.write(content)
