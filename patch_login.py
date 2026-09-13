import re

with open("app/src/main/java/com/shuaib/classmate/activities/LoginActivity.kt", "r") as f:
    content = f.read()

old_perms = """            val existingPerms = doc.get("permissions") as? Map<*, *> ?: emptyMap<String, Boolean>()
            val missingPerms = User.DEFAULT_PERMISSIONS.filter { !existingPerms.containsKey(it.key) }

            fun finishNormalization() {
                if (missingPerms.isNotEmpty()) {
                    val payload = mapOf("permissions" to missingPerms)
                    logFirestoreWrite("users/$uid", uid, payload)
                    userRef.set(payload, SetOptions.merge())
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Log.d("FirestoreDebug", "Write success: users/$uid")
                            } else {
                                task.exception?.let { logFirestoreFailure(it) }
                            }
                            identifyUserAndNavigate(uid, role, existingData["batch"] as? String ?: "")
                        }
                } else {
                    identifyUserAndNavigate(uid, role, existingData["batch"] as? String ?: "")
                }
            }"""

new_perms = """            fun finishNormalization() {
                identifyUserAndNavigate(uid, role, existingData["batch"] as? String ?: "")
            }"""

content = content.replace(old_perms, new_perms)

with open("app/src/main/java/com/shuaib/classmate/activities/LoginActivity.kt", "w") as f:
    f.write(content)
