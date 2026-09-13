import re

with open('app/src/main/java/com/shuaib/classmate/fragments/NoticeFragment.kt', 'r') as f:
    content = f.read()

pattern = r'isAdmin = \(role == "superadmin" \|\| role == "admin" \|\| permissions\["canPostNotices"\] == true\)'
replacement = 'isAdmin = (role == "superadmin" || role == "admin" || role == "teacher" || permissions["canPostNotices"] == true)'
content = re.sub(pattern, replacement, content)

with open('app/src/main/java/com/shuaib/classmate/fragments/NoticeFragment.kt', 'w') as f:
    f.write(content)
