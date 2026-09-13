with open("app/src/main/java/com/shuaib/classmate/activities/UserDetailActivity.kt", "r") as f:
    content = f.read()

old_block = """    private fun selectedRole(): String {
        return when (binding.radioGroupRole.checkedRadioButtonId) {
            R.id.radioSuperadmin -> "superadmin"
            R.id.radioAdmin -> "admin"
            else -> "student"
        }
    }"""

new_block = """    private fun selectedRole(): String {
        return when (binding.radioGroupRole.checkedRadioButtonId) {
            R.id.radioSuperadmin -> "superadmin"
            R.id.radioAdmin -> "admin"
            R.id.radioTeacher -> "teacher"
            else -> "student"
        }
    }"""

content = content.replace(old_block, new_block)

with open("app/src/main/java/com/shuaib/classmate/activities/UserDetailActivity.kt", "w") as f:
    f.write(content)
