import re

with open('app/src/main/java/com/shuaib/classmate/activities/RegisterActivity.kt', 'r') as f:
    content = f.read()

# Add RadioGroup listener in onCreate
on_create_code = """
        binding.rgRole.setOnCheckedChangeListener { _, checkedId ->
            val isTeacher = checkedId == R.id.rbTeacher
            
            // Toggle visibility
            binding.tilStudentId.visibility = if (isTeacher) View.GONE else View.VISIBLE
            binding.tilBatch.visibility = if (isTeacher) View.GONE else View.VISIBLE
            binding.tilFatherName.visibility = if (isTeacher) View.GONE else View.VISIBLE
            binding.tilMotherName.visibility = if (isTeacher) View.GONE else View.VISIBLE
            binding.tilPresentAddress.visibility = if (isTeacher) View.GONE else View.VISIBLE
            binding.tilPermanentAddress.visibility = if (isTeacher) View.GONE else View.VISIBLE
            binding.tilDistrict.visibility = if (isTeacher) View.GONE else View.VISIBLE
            binding.tilBloodGroup.visibility = if (isTeacher) View.GONE else View.VISIBLE
            
            binding.tilDepartment.visibility = if (isTeacher) View.VISIBLE else View.GONE
            binding.tilWhatsApp.visibility = if (isTeacher) View.VISIBLE else View.GONE
            binding.tilPhone.visibility = if (isTeacher) View.VISIBLE else View.GONE
        }
        
        // Trigger initial state
        binding.rgRole.check(R.id.rbStudent)
        binding.tilDepartment.visibility = View.GONE
        binding.tilWhatsApp.visibility = View.GONE
        binding.tilPhone.visibility = View.GONE
"""
content = content.replace('updateStepUI()', on_create_code + '\n        updateStepUI()')

# Update registerUser() to pass all parameters
register_user_pattern = re.compile(r'private fun registerUser\(\).*?createEmailAccount\(.*?password\)', re.DOTALL)
register_user_new = """private fun registerUser() {
        if (authInProgress) return
        val isTeacher = binding.rbTeacher.isChecked
        val name = binding.etName.text.toString().trim()
        val studentId = if (isTeacher) "" else StudentIdUtils.normalize(binding.etStudentId.text.toString())
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val confirmPassword = binding.etConfirmPassword.text.toString()
        
        // Additional fields
        val department = if (isTeacher) binding.etDepartment.text.toString().trim() else ""
        val whatsapp = if (isTeacher) binding.etWhatsApp.text.toString().trim() else ""
        val phone = if (isTeacher) binding.etPhone.text.toString().trim() else ""
        
        val batch = if (isTeacher) "" else binding.etBatch.text.toString().trim()
        val fatherName = if (isTeacher) "" else binding.etFatherName.text.toString().trim()
        val motherName = if (isTeacher) "" else binding.etMotherName.text.toString().trim()
        val presentAddress = if (isTeacher) "" else binding.etPresentAddress.text.toString().trim()
        val permanentAddress = if (isTeacher) "" else binding.etPermanentAddress.text.toString().trim()
        val district = if (isTeacher) "" else binding.etDistrict.text.toString().trim()
        val bloodGroup = if (isTeacher) "" else binding.etBloodGroup.text.toString().trim()

        Log.d("AuthTrace", "1. Signup button clicked")
        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showError("Please fill all required fields")
            shakeForm()
            return
        }
        if (password != confirmPassword) {
            showError("Passwords do not match")
            shakeForm()
            return
        }
        if (isTeacher && (department.isEmpty() || phone.isEmpty())) {
            showError("Department and Phone are required for Teachers")
            shakeForm()
            return
        }
        if (!isTeacher && studentId.isEmpty()) {
            showError("Student ID is required")
            shakeForm()
            return
        }

        AuthDebug.d("Email signup start email=${AuthDebug.maskEmail(email)}")
        setLoading(true)
        createEmailAccount(
            name, studentId, email, password, isTeacher, department, whatsapp, phone,
            batch, fatherName, motherName, presentAddress, permanentAddress, district, bloodGroup
        )
"""
content = register_user_pattern.sub(register_user_new, content)

# Update validateForm to just return true (since we do validation in registerUser now)
# Actually, wait, let's just leave validateForm as is, since register_user_pattern replaced the old validation call.
content = content.replace("private fun createEmailAccount(name: String, studentId: String, email: String, password: String)",
    "private fun createEmailAccount(name: String, studentId: String, email: String, password: String, isTeacher: Boolean, department: String, whatsapp: String, phone: String, batch: String, fatherName: String, motherName: String, presentAddress: String, permanentAddress: String, district: String, bloodGroup: String)")

# In createEmailAccount, update saveUserToFirestore call
content = content.replace("saveUserToFirestore(firebaseUser.uid, name, studentId, email, \"\")",
    "saveUserToFirestore(firebaseUser.uid, name, studentId, email, \"\", isTeacher, department, whatsapp, phone, batch, fatherName, motherName, presentAddress, permanentAddress, district, bloodGroup)")

# Update saveUserToFirestore signature and body
save_user_old = """private fun saveUserToFirestore(uid: String, name: String, studentId: String, email: String, photoUrl: String) {"""
save_user_new = """private fun saveUserToFirestore(uid: String, name: String, studentId: String, email: String, photoUrl: String, isTeacher: Boolean, department: String, whatsapp: String, phone: String, batch: String, fatherName: String, motherName: String, presentAddress: String, permanentAddress: String, district: String, bloodGroup: String) {"""
content = content.replace(save_user_old, save_user_new)

# Update userMap in saveUserToFirestore
user_map_old = """            val userMap = hashMapOf<String, Any>(
                "uid" to uid,
                "name" to name,
                "fullName" to name,
                "studentId" to studentId,
                "department" to "",
                "email" to email,
                "photoUrl" to photoUrl,
                "role" to "student",
                "approved" to false,
                "permissions" to User.DEFAULT_PERMISSIONS,
                "favoriteSubjects" to emptyList<String>(),
                "favoritePdfIds" to emptyList<String>(),
                "authProvider" to "email",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )"""
            
user_map_new = """            val userMap = hashMapOf<String, Any>(
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
            )"""
content = content.replace(user_map_old, user_map_new)

with open('app/src/main/java/com/shuaib/classmate/activities/RegisterActivity.kt', 'w') as f:
    f.write(content)

print("Updated RegisterActivity.kt")
