with open("app/src/main/java/com/shuaib/classmate/chat/DmListFragment.kt", "r") as f:
    content = f.read()

content = content.replace("private enum class FilterType { ALL, ONLINE, ADMINS, STUDENTS }",
                          "private enum class FilterType { ALL, ONLINE, TEACHERS, ADMINS, STUDENTS }")

old_chips = """        val chips = mapOf(
            FilterType.ALL to binding.chipAll,
            FilterType.ONLINE to binding.chipOnline,
            FilterType.ADMINS to binding.chipAdmins,
            FilterType.STUDENTS to binding.chipStudents
        )"""
new_chips = """        val chips = mapOf(
            FilterType.ALL to binding.chipAll,
            FilterType.ONLINE to binding.chipOnline,
            FilterType.TEACHERS to binding.chipTeachers,
            FilterType.ADMINS to binding.chipAdmins,
            FilterType.STUDENTS to binding.chipStudents
        )"""
content = content.replace(old_chips, new_chips)

old_filter = """        when (currentFilter) {
            FilterType.ONLINE -> filtered = filtered.filter { it.isOnline }
            FilterType.ADMINS -> filtered = filtered.filter { it.role == "admin" || it.role == "superadmin" }
            FilterType.STUDENTS -> filtered = filtered.filter { it.role == "student" || it.role.isBlank() }
            FilterType.ALL -> {}
        }"""
new_filter = """        when (currentFilter) {
            FilterType.ONLINE -> filtered = filtered.filter { it.isOnline }
            FilterType.TEACHERS -> filtered = filtered.filter { it.role == "teacher" }
            FilterType.ADMINS -> filtered = filtered.filter { it.role == "admin" || it.role == "superadmin" }
            FilterType.STUDENTS -> filtered = filtered.filter { it.role == "student" || it.role.isBlank() }
            FilterType.ALL -> {}
        }"""
content = content.replace(old_filter, new_filter)

with open("app/src/main/java/com/shuaib/classmate/chat/DmListFragment.kt", "w") as f:
    f.write(content)
