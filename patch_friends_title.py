import re

with open('app/src/main/java/com/shuaib/classmate/fragments/FriendsFragment.kt', 'r') as f:
    content = f.read()

bad = """        isTeacherMode = arguments?.getBoolean("isTeacherMode") ?: false
        if (isTeacherMode) {"""
good = """        isTeacherMode = arguments?.getBoolean("isTeacherMode") ?: false
        if (isTeacherMode) {
            binding.tvHeaderTitle.text = "Teacher Directory"
            binding.chipBatch?.visibility = android.view.View.GONE
            binding.chipMyMatches.visibility = android.view.View.GONE
            binding.tilSearch.hint = "Search name or department..."
        } else {
            binding.tvHeaderTitle.text = "Student Directory"
        }"""
content = content.replace(bad, good)

with open('app/src/main/java/com/shuaib/classmate/fragments/FriendsFragment.kt', 'w') as f:
    f.write(content)
