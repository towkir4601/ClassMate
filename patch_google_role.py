import re

with open('app/src/main/java/com/shuaib/classmate/activities/RegisterActivity.kt', 'r') as f:
    content = f.read()

bad = """    private fun saveGoogleUserToFirestore(uid: String, name: String, email: String, photoUrl: String, isNewUser: Boolean) {
        val userRef = firestore.document("users/$uid")
        val finalName = name.ifBlank { email.substringBefore("@").ifBlank { "User" } }
        Log.d("AuthTrace", "9. users/{uid} profile create/update started")
        
        fun createGoogleProfile() {
            val userMap = hashMapOf<String, Any>(
                "uid" to uid,
                "name" to finalName,
                "fullName" to finalName,
                "email" to email,
                "studentId" to "",
                "department" to "CSE",
                "photoUrl" to photoUrl,
                "role" to "student",
                "approved" to false,"""

good = """    private fun saveGoogleUserToFirestore(uid: String, name: String, email: String, photoUrl: String, isNewUser: Boolean) {
        val userRef = firestore.document("users/$uid")
        val finalName = name.ifBlank { email.substringBefore("@").ifBlank { "User" } }
        Log.d("AuthTrace", "9. users/{uid} profile create/update started")
        
        fun createGoogleProfile() {
            val isTeacher = binding.rgRole.checkedRadioButtonId == R.id.rbTeacher
            val userMap = hashMapOf<String, Any>(
                "uid" to uid,
                "name" to finalName,
                "fullName" to finalName,
                "email" to email,
                "studentId" to "",
                "department" to "CSE",
                "photoUrl" to photoUrl,
                "role" to if (isTeacher) "teacher" else "student",
                "approved" to false,"""

content = content.replace(bad, good)

with open('app/src/main/java/com/shuaib/classmate/activities/RegisterActivity.kt', 'w') as f:
    f.write(content)
