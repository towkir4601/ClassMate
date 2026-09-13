import re

with open('app/src/main/java/com/shuaib/classmate/activities/RegisterActivity.kt', 'r') as f:
    content = f.read()

# Add call in onCreate
bad_on_create = """        setupPasswordStrength()
        setupKeyboardActions()
        setupStudentIdFormatting()
        showStep(0, focus = false)"""

good_on_create = """        setupPasswordStrength()
        setupKeyboardActions()
        setupStudentIdFormatting()
        setupRoleToggle()
        showStep(0, focus = false)"""

content = content.replace(bad_on_create, good_on_create)

# Add function setupRoleToggle
new_func = """
    private fun setupRoleToggle() {
        binding.rgRole.setOnCheckedChangeListener { _, checkedId ->
            val isTeacher = checkedId == R.id.rbTeacher
            binding.tilStudentId.visibility = if (isTeacher) android.view.View.GONE else android.view.View.VISIBLE
            binding.tilBatch.visibility = if (isTeacher) android.view.View.GONE else android.view.View.VISIBLE
            binding.tilFatherName.visibility = if (isTeacher) android.view.View.GONE else android.view.View.VISIBLE
            binding.tilMotherName.visibility = if (isTeacher) android.view.View.GONE else android.view.View.VISIBLE
        }
    }
"""

content = content.replace("    private fun setupAnimations() {", new_func + "\n    private fun setupAnimations() {")

with open('app/src/main/java/com/shuaib/classmate/activities/RegisterActivity.kt', 'w') as f:
    f.write(content)
