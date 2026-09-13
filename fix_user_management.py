import re

with open('app/src/main/java/com/shuaib/classmate/activities/UserManagementActivity.kt', 'r') as f:
    content = f.read()

# Fix approveUser
bad_approve = """                val idx = userList.indexOfFirst { it.uid == targetUser.uid }
                if (idx != -1) {
                    userList[idx] = userList[idx].copy(approved = approve)
                    userAdapter.notifyItemChanged(idx)
                }"""
good_approve = ""
content = content.replace(bad_approve, good_approve)

# Fix updateUserRole
bad_role = """                val idx = userList.indexOfFirst { it.uid == targetUser.uid }
                if (idx != -1) {
                    userList[idx] = userList[idx].copy(role = newRole, permissions = newPermissions)
                    userAdapter.notifyItemChanged(idx)
                }"""
good_role = ""
content = content.replace(bad_role, good_role)

# Fix deleteUser
bad_delete = """                val idx = userList.indexOfFirst { it.uid == targetUser.uid }
                if (idx != -1) {
                    userList.removeAt(idx)
                    userAdapter.notifyItemRemoved(idx)
                }"""
good_delete = ""
content = content.replace(bad_delete, good_delete)

with open('app/src/main/java/com/shuaib/classmate/activities/UserManagementActivity.kt', 'w') as f:
    f.write(content)
