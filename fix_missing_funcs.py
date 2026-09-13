import re

with open('app/src/main/java/com/shuaib/classmate/activities/RegisterActivity.kt', 'r') as f:
    content = f.read()

missing_funcs = """
    private fun logFirestoreWrite(path: String, uid: String, payload: Map<*, *>) {
        Log.d("FirestoreDebug", "About to write to: $path")
        Log.d("FirestoreDebug", "Auth uid: ${FirebaseAuth.getInstance().currentUser?.uid}")
        Log.d("FirestoreDebug", "Target uid: $uid")
        Log.d("FirestoreDebug", "Payload keys: ${payload.keys}")
        Log.d("FirestoreDebug", "Role value: ${payload["role"]}")
        Log.d("FirestoreDebug", "Permissions value: ${payload["permissions"]}")
    }

    private fun logFirestoreFailure(e: Exception) {
        Log.e("FirestoreDebug", "FULL ERROR: ${e.message}")
        Log.e("FirestoreDebug", "ERROR CLASS: ${e.javaClass.name}")
        Log.e("FirestoreDebug", "CAUSE: ${e.cause?.message}")
    }

    private fun finishAuthFlow(uid: String) {
        Log.d("AuthTrace", "8. Navigation started")
        identifyUserInOneSignal(uid)
        setLoading(false)
        
        startActivity(android.content.Intent(this, com.shuaib.classmate.activities.CompleteProfileActivity::class.java))
        finishAffinity()
    }

    private fun validateProfileStep(): Boolean {
        var valid = true
        binding.tilName.error = null
        binding.tilStudentId.error = null

        val name = binding.etName.text.toString().trim()
        val studentId = com.shuaib.classmate.utils.StudentIdUtils.normalize(binding.etStudentId.text.toString())
        val isTeacher = binding.rgRole.checkedRadioButtonId == R.id.rbTeacher
        
        if (name.length < 2) {
            binding.tilName.error = "Name must be at least 2 characters"
            valid = false
        }
        if (!isTeacher && !com.shuaib.classmate.utils.StudentIdUtils.isValid(studentId)) {
            binding.tilStudentId.error = "Enter valid student ID"
            valid = false
        }
        if (!valid) shakeForm()
        return valid
    }

    private fun updatePasswordStrength(password: String) {"""

content = content.replace("    private fun updatePasswordStrength(password: String) {", missing_funcs)

with open('app/src/main/java/com/shuaib/classmate/activities/RegisterActivity.kt', 'w') as f:
    f.write(content)
