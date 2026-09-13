import re

with open('app/src/main/java/com/shuaib/classmate/activities/RegisterActivity.kt', 'r') as f:
    content = f.read()

bad_toggle = """    private fun setupRoleToggle() {
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
good_toggle = """    private fun setupRoleToggle() {
        val applyRoleVisibility = {
            val isTeacher = binding.rgRole.checkedRadioButtonId == R.id.rbTeacher
            val visibility = if (isTeacher) android.view.View.GONE else android.view.View.VISIBLE
            binding.tilStudentId.visibility = visibility
            binding.tilBatch.visibility = visibility
            binding.tilFatherName.visibility = visibility
            binding.tilMotherName.visibility = visibility
            binding.tilPresentAddress.visibility = visibility
            binding.tilPermanentAddress.visibility = visibility
            binding.tilDistrict.visibility = visibility
        }
        binding.rgRole.setOnCheckedChangeListener { _, _ -> applyRoleVisibility() }
        applyRoleVisibility()
    }"""
content = content.replace(bad_toggle, good_toggle)

bad_vals = """        val presentAddress = binding.etPresentAddress.text.toString().trim()
        val permanentAddress = binding.etPermanentAddress.text.toString().trim()
        val district = binding.etDistrict.text.toString().trim()"""
        
good_vals = """        val presentAddress = if (isTeacher) "" else binding.etPresentAddress.text.toString().trim()
        val permanentAddress = if (isTeacher) "" else binding.etPermanentAddress.text.toString().trim()
        val district = if (isTeacher) "" else binding.etDistrict.text.toString().trim()"""

content = content.replace(bad_vals, good_vals)

with open('app/src/main/java/com/shuaib/classmate/activities/RegisterActivity.kt', 'w') as f:
    f.write(content)
