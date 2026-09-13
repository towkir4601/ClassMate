import re

with open('app/src/main/java/com/shuaib/classmate/activities/RegisterActivity.kt', 'r') as f:
    content = f.read()

# Fix setupRoleToggle
bad_toggle = """    private fun setupRoleToggle() {
        binding.rgRole.setOnCheckedChangeListener { _, checkedId ->
            val isTeacher = checkedId == R.id.rbTeacher
            binding.tilStudentId.visibility = if (isTeacher) android.view.View.GONE else android.view.View.VISIBLE
            binding.tilBatch.visibility = if (isTeacher) android.view.View.GONE else android.view.View.VISIBLE
            binding.tilFatherName.visibility = if (isTeacher) android.view.View.GONE else android.view.View.VISIBLE
            binding.tilMotherName.visibility = if (isTeacher) android.view.View.GONE else android.view.View.VISIBLE
        }
    }"""
good_toggle = """    private fun setupRoleToggle() {
        binding.rgRole.setOnCheckedChangeListener { _, checkedId ->
            val isTeacher = checkedId == R.id.rbTeacher
            binding.tilStudentId.visibility = if (isTeacher) android.view.View.GONE else android.view.View.VISIBLE
            binding.tilBatch.visibility = if (isTeacher) android.view.View.GONE else android.view.View.VISIBLE
            binding.tilFatherName.visibility = if (isTeacher) android.view.View.GONE else android.view.View.VISIBLE
            binding.tilMotherName.visibility = if (isTeacher) android.view.View.GONE else android.view.View.VISIBLE
        }
        val initialTeacher = binding.rgRole.checkedRadioButtonId == R.id.rbTeacher
        binding.tilStudentId.visibility = if (initialTeacher) android.view.View.GONE else android.view.View.VISIBLE
        binding.tilBatch.visibility = if (initialTeacher) android.view.View.GONE else android.view.View.VISIBLE
        binding.tilFatherName.visibility = if (initialTeacher) android.view.View.GONE else android.view.View.VISIBLE
        binding.tilMotherName.visibility = if (initialTeacher) android.view.View.GONE else android.view.View.VISIBLE
    }"""
content = content.replace(bad_toggle, good_toggle)

# Fix registerUser values
bad_vals = """        // Additional fields
        val department = if (isTeacher) binding.etDepartment.text.toString().trim() else ""
        val whatsapp = if (isTeacher) binding.etWhatsApp.text.toString().trim() else ""
        val phone = if (isTeacher) binding.etPhone.text.toString().trim() else ""
        
        val batch = if (isTeacher) "" else binding.etBatch.text.toString().trim()
        val fatherName = if (isTeacher) "" else binding.etFatherName.text.toString().trim()
        val motherName = if (isTeacher) "" else binding.etMotherName.text.toString().trim()
        val presentAddress = if (isTeacher) "" else binding.etPresentAddress.text.toString().trim()
        val permanentAddress = if (isTeacher) "" else binding.etPermanentAddress.text.toString().trim()
        val district = if (isTeacher) "" else binding.etDistrict.text.toString().trim()
        val bloodGroup = if (isTeacher) "" else binding.etBloodGroup.text.toString().trim()"""
        
good_vals = """        // Additional fields
        val department = binding.etDepartment.text.toString().trim()
        val whatsapp = binding.etWhatsApp.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        
        val batch = if (isTeacher) "" else binding.etBatch.text.toString().trim()
        val fatherName = if (isTeacher) "" else binding.etFatherName.text.toString().trim()
        val motherName = if (isTeacher) "" else binding.etMotherName.text.toString().trim()
        val presentAddress = binding.etPresentAddress.text.toString().trim()
        val permanentAddress = binding.etPermanentAddress.text.toString().trim()
        val district = binding.etDistrict.text.toString().trim()
        val bloodGroup = binding.etBloodGroup.text.toString().trim()"""

content = content.replace(bad_vals, good_vals)

with open('app/src/main/java/com/shuaib/classmate/activities/RegisterActivity.kt', 'w') as f:
    f.write(content)
