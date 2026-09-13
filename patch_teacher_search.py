import re

with open('app/src/main/java/com/shuaib/classmate/fragments/FriendsFragment.kt', 'r') as f:
    content = f.read()

bad = """                user.studentId.contains(trimmedQuery, ignoreCase = true) ||
                user.bloodGroup.contains(trimmedQuery, ignoreCase = true) ||
                user.homeDistrict.contains(trimmedQuery, ignoreCase = true) ||
                user.address.contains(trimmedQuery, ignoreCase = true) ||
                user.phone.contains(trimmedQuery)"""

good = """                user.studentId.contains(trimmedQuery, ignoreCase = true) ||
                user.bloodGroup.contains(trimmedQuery, ignoreCase = true) ||
                user.homeDistrict.contains(trimmedQuery, ignoreCase = true) ||
                user.department.contains(trimmedQuery, ignoreCase = true) ||
                user.address.contains(trimmedQuery, ignoreCase = true) ||
                user.phone.contains(trimmedQuery)"""

content = content.replace(bad, good)

with open('app/src/main/java/com/shuaib/classmate/fragments/FriendsFragment.kt', 'w') as f:
    f.write(content)
