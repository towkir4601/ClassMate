import re

with open('app/src/main/java/com/shuaib/classmate/activities/RegisterActivity.kt', 'r') as f:
    content = f.read()

bad = """    private fun finishAuthFlow(uid: String) {
        Log.d("AuthTrace", "8. Navigation started")
        ensureRoleDefaults(uid)
        val calculatedBatch = if (isTeacher) "" else {"""

good = """    private fun finishAuthFlow(uid: String) {
        Log.d("AuthTrace", "8. Navigation started")
        ensureRoleDefaults(uid)
        val isTeacher = binding.rgRole.checkedRadioButtonId == R.id.rbTeacher
        val calculatedBatch = if (isTeacher) "" else {"""

content = content.replace(bad, good)

with open('app/src/main/java/com/shuaib/classmate/activities/RegisterActivity.kt', 'w') as f:
    f.write(content)
