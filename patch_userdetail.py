with open("app/src/main/java/com/shuaib/classmate/activities/UserDetailActivity.kt", "r") as f:
    content = f.read()

old_block = """            val alignedPermissions = when (role) {
                "superadmin" -> User.DEFAULT_PERMISSIONS.mapValues { true }
                "admin" -> User.DEFAULT_PERMISSIONS.mapValues { it.key != "canManageUsers" && it.key != "canManageAdmins" }
                else -> User.DEFAULT_PERMISSIONS.mapValues { false }
            }
            updates["permissions"] = alignedPermissions"""

new_block = """            val alignedPermissions = when (role) {
                "superadmin" -> User.DEFAULT_PERMISSIONS.mapValues { true }
                "admin" -> newPermissions.apply { 
                    put("canManageUsers", false)
                    put("canManageAdmins", false)
                }
                else -> newPermissions
            }
            updates["permissions"] = alignedPermissions"""

content = content.replace(old_block, new_block)

with open("app/src/main/java/com/shuaib/classmate/activities/UserDetailActivity.kt", "w") as f:
    f.write(content)
