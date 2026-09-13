import re

with open('app/src/main/java/com/shuaib/classmate/activities/RegisterActivity.kt', 'r') as f:
    content = f.read()

# Add tilBatch to updateFieldsForRole
bad_role = """            binding.tilStudentId.visibility = visibility
            binding.tilFatherName.visibility = visibility"""
good_role = """            binding.tilStudentId.visibility = visibility
            binding.tilBatch.visibility = visibility
            binding.tilFatherName.visibility = visibility"""
content = content.replace(bad_role, good_role)

# Auto-fill batch when student ID changes
bad_anim = """    private fun setupAnimations() {"""
good_anim = """    private fun setupBatchAutoFill() {
        binding.etStudentId.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                if (binding.rgRole.checkedRadioButtonId == R.id.rbTeacher) return
                val idStr = s?.toString()?.trim() ?: ""
                val prefix = idStr.take(2).toIntOrNull()
                if (prefix != null) {
                    binding.etBatch.setText((prefix - 10).toString())
                }
            }
        })
    }

    private fun setupAnimations() {"""
content = content.replace(bad_anim, good_anim)

# Call setupBatchAutoFill in onCreate
bad_onCreate = """        setupRoleToggle()
        setupAnimations()"""
good_onCreate = """        setupRoleToggle()
        setupBatchAutoFill()
        setupAnimations()"""
content = content.replace(bad_onCreate, good_onCreate)

# Use etBatch in finishAuthFlow
bad_finish = """    private fun finishAuthFlow(uid: String) {
        Log.d("AuthTrace", "8. Navigation started")
        ensureRoleDefaults(uid)
        val isTeacher = binding.rgRole.checkedRadioButtonId == R.id.rbTeacher
        val calculatedBatch = if (isTeacher) "" else {
            val p = binding.etStudentId.text.toString().take(2).toIntOrNull()
            if (p != null) (p - 10).toString() else ""
        }
        identifyUserInOneSignal(uid, calculatedBatch)"""
good_finish = """    private fun finishAuthFlow(uid: String) {
        Log.d("AuthTrace", "8. Navigation started")
        ensureRoleDefaults(uid)
        val isTeacher = binding.rgRole.checkedRadioButtonId == R.id.rbTeacher
        val batch = if (isTeacher) "" else binding.etBatch.text.toString().trim()
        identifyUserInOneSignal(uid, batch)"""
content = content.replace(bad_finish, good_finish)

with open('app/src/main/java/com/shuaib/classmate/activities/RegisterActivity.kt', 'w') as f:
    f.write(content)
