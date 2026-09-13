import re

with open('app/src/main/java/com/shuaib/classmate/fragments/FriendsFragment.kt', 'r') as f:
    content = f.read()

content = content.replace('private var currentUserDistrict = ""', 'private var currentUserDistrict = ""\n    private var currentUserBatch = ""')

with open('app/src/main/java/com/shuaib/classmate/fragments/FriendsFragment.kt', 'w') as f:
    f.write(content)
