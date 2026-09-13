import re

with open('app/src/main/java/com/shuaib/classmate/models/User.kt', 'r') as f:
    content = f.read()

bad_user = """    val oneSignalPlayerId: String = "",
    val favoriteSubjects: List<String> = emptyList(),
    val favoritePdfIds: List<String> = emptyList(),
    val permissions: Map<String, Boolean> = DEFAULT_PERMISSIONS
) {"""
good_user = """    val oneSignalPlayerId: String = "",
    val favoriteSubjects: List<String> = emptyList(),
    val favoritePdfIds: List<String> = emptyList(),
    val permissions: Map<String, Boolean> = DEFAULT_PERMISSIONS,
    val isOnline: Boolean = false
) {"""

content = content.replace(bad_user, good_user)

with open('app/src/main/java/com/shuaib/classmate/models/User.kt', 'w') as f:
    f.write(content)
