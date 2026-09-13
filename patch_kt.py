with open("app/src/main/java/com/shuaib/classmate/activities/EditProfileActivity.kt", "r") as f:
    content = f.read()

# Load User Profile Patch
old_load = """                    binding.etName.setText(doc.getString("name") ?: "")
                    binding.etStudentId.setText(doc.getString("studentId") ?: "")
                    binding.etPhone.setText(doc.getString("phone") ?: "")"""

new_load = """                    binding.etName.setText(doc.getString("name") ?: "")
                    binding.etStudentId.setText(doc.getString("studentId") ?: "")
                    binding.etDepartment.setText(doc.getString("department") ?: "")
                    binding.etPhone.setText(doc.getString("phone") ?: "")"""

content = content.replace(old_load, new_load)


# Role Hide Patch
old_role = """                    if (doc.getString("role") == "teacher") {
                        (binding.etStudentId.parent as? android.view.View)?.visibility = android.view.View.GONE
                        (binding.etBatch.parent as? android.view.View)?.visibility = android.view.View.GONE
                        (binding.etFatherName.parent as? android.view.View)?.visibility = android.view.View.GONE
                        (binding.etMotherName.parent as? android.view.View)?.visibility = android.view.View.GONE
                    }"""

new_role = """                    if (doc.getString("role") == "teacher") {
                        (binding.etStudentId.parent as? android.view.View)?.visibility = android.view.View.GONE
                        (binding.etBatch.parent as? android.view.View)?.visibility = android.view.View.GONE
                        (binding.etFatherName.parent as? android.view.View)?.visibility = android.view.View.GONE
                        (binding.etMotherName.parent as? android.view.View)?.visibility = android.view.View.GONE
                        (binding.etPresentAddress.parent as? android.view.View)?.visibility = android.view.View.GONE
                        (binding.etPermanentAddress.parent as? android.view.View)?.visibility = android.view.View.GONE
                    }"""

content = content.replace(old_role, new_role)


# Save Profile Patch
old_save = """            "name" to binding.etName.text.toString().trim(),
            "studentId" to binding.etStudentId.text.toString().trim(),
            "phone" to binding.etPhone.text.toString().trim(),"""

new_save = """            "name" to binding.etName.text.toString().trim(),
            "studentId" to binding.etStudentId.text.toString().trim(),
            "department" to binding.etDepartment.text.toString().trim(),
            "phone" to binding.etPhone.text.toString().trim(),"""

content = content.replace(old_save, new_save)

with open("app/src/main/java/com/shuaib/classmate/activities/EditProfileActivity.kt", "w") as f:
    f.write(content)
