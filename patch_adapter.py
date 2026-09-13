with open("app/src/main/java/com/shuaib/classmate/chat/DmUsersAdapter.kt", "r") as f:
    content = f.read()

old_bind = """            userName.text = user.name
            AvatarUtils.bind(avatar, avatarLetter, user.id, user.name, user.avatarUrl)
            val online = user.id in onlineIdsProvider() || user.isOnline"""
new_bind = """            val roleSuffix = when (user.role) {
                "teacher" -> " (Teacher)"
                "admin", "superadmin" -> " (Admin)"
                else -> ""
            }
            val displayName = user.name + roleSuffix
            userName.text = displayName
            
            AvatarUtils.bind(avatar, avatarLetter, user.id, user.name, user.avatarUrl)
            val online = user.id in onlineIdsProvider() || user.isOnline"""

content = content.replace(old_bind, new_bind)

with open("app/src/main/java/com/shuaib/classmate/chat/DmUsersAdapter.kt", "w") as f:
    f.write(content)
