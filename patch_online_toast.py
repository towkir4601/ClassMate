with open("app/src/main/java/com/shuaib/classmate/activities/MainActivity.kt", "r") as f:
    content = f.read()

old_status = """    private fun setOnlineStatus(isOnline: Boolean) {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("users").document(uid)
            .update("isOnline", isOnline)
    }"""
new_status = """    private fun setOnlineStatus(isOnline: Boolean) {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("users").document(uid)
            .update("isOnline", isOnline)
            .addOnFailureListener { e ->
                android.util.Log.e("MainActivity", "Failed to update online status: ${e.message}")
            }
    }"""

content = content.replace(old_status, new_status)

with open("app/src/main/java/com/shuaib/classmate/activities/MainActivity.kt", "w") as f:
    f.write(content)
