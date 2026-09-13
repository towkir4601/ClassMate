import re

with open('app/src/main/java/com/shuaib/classmate/activities/RegisterActivity.kt', 'r') as f:
    content = f.read()

# Replace saveUserToFirestore completely
pattern = re.compile(r'private fun saveUserToFirestore.*?addOnFailureListener \{ e ->.*?shakeForm\(\).*?\}', re.DOTALL)
replacement = """private fun saveUserToFirestore(uid: String, name: String, studentId: String, email: String, photoUrl: String, isTeacher: Boolean, department: String, whatsapp: String, phone: String, batch: String, fatherName: String, motherName: String, presentAddress: String, permanentAddress: String, district: String, bloodGroup: String) {
        val userMap = hashMapOf<String, Any>(
            "uid" to uid,
            "name" to name,
            "fullName" to name,
            "studentId" to studentId,
            "department" to department,
            "email" to email,
            "photoUrl" to photoUrl,
            "role" to if (isTeacher) "teacher" else "student",
            "approved" to false,
            "permissions" to User.DEFAULT_PERMISSIONS,
            "favoriteSubjects" to emptyList<String>(),
            "favoritePdfIds" to emptyList<String>(),
            "authProvider" to "email",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
            "whatsappNumber" to whatsapp,
            "phone" to phone,
            "batch" to batch,
            "fatherName" to fatherName,
            "motherName" to motherName,
            "presentAddress" to presentAddress,
            "permanentAddress" to permanentAddress,
            "homeDistrict" to district,
            "bloodGroup" to bloodGroup
        )

        val userRef = FirebaseFirestore.getInstance().document("users/$uid")

        Log.d("AuthTrace", "6. Firestore users/{uid} write started")
        logFirestoreWrite("users/$uid", uid, userMap)
        userRef.set(userMap, SetOptions.merge())
            .addOnSuccessListener {
                Log.d("FirestoreDebug", "Write success: users/$uid")
                Log.d("AuthTrace", "7. Firestore users/{uid} write success")

                if (!isTeacher && studentId.isNotBlank() && StudentIdUtils.isValid(studentId)) {
                    val lookupRef = firestore.collection("student_id_lookup").document(studentId)
                    val lookupMap = hashMapOf<String, Any>(
                        "uid" to uid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    logFirestoreWrite("student_id_lookup/$studentId", uid, lookupMap)
                    lookupRef.set(lookupMap).addOnFailureListener { logFirestoreFailure(it) }
                }

                com.shuaib.classmate.utils.NotificationSender.sendRegistrationAlert(name, if (isTeacher) "Teacher" else studentId)
                finishAuthFlow(uid)
            }
            .addOnFailureListener { e ->
                logFirestoreFailure(e)
                Log.d("AuthTrace", "7. Firestore users/{uid} write failure: ${e.message}")
                AuthDebug.logFirestoreFailure("email_signup_profile_write", e)
                setLoading(false)
                showError("Could not save profile: ${e.message}")
                shakeForm()
            }"""
            
if pattern.search(content):
    content = pattern.sub(replacement, content, 1) # Replace first match (the one at the bottom)
    with open('app/src/main/java/com/shuaib/classmate/activities/RegisterActivity.kt', 'w') as f:
        f.write(content)
    print("Updated saveUserToFirestore")
else:
    print("Could not find saveUserToFirestore")
