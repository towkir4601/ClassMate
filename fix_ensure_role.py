import re

with open('app/src/main/java/com/shuaib/classmate/activities/RegisterActivity.kt', 'r') as f:
    content = f.read()

ensure = """
    private fun ensureRoleDefaults(uid: String) {
        val userRef = FirebaseFirestore.getInstance().document("users/$uid")
        userRef.get()
            .addOnSuccessListener { doc ->
                if (!doc.exists() || !doc.contains("role")) {
                    val roleDefaults = hashMapOf<String, Any>(
                        "role" to "student",
                        "permissions" to com.shuaib.classmate.models.User.DEFAULT_PERMISSIONS,
                        "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )
                    userRef.set(roleDefaults, com.google.firebase.firestore.SetOptions.merge())
                }
            }
    }

    private fun validateProfileStep(): Boolean {"""

content = content.replace("    private fun validateProfileStep(): Boolean {", ensure)

with open('app/src/main/java/com/shuaib/classmate/activities/RegisterActivity.kt', 'w') as f:
    f.write(content)
