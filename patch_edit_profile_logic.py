import re

with open('app/src/main/java/com/shuaib/classmate/activities/EditProfileActivity.kt', 'r') as f:
    content = f.read()

# Make sure we declare originalBatch to compare
bad_load = """                    binding.etWhatsApp.setText(doc.getString("whatsappNumber") ?: "")
                    binding.etFatherName.setText(doc.getString("fatherName") ?: "")"""
good_load = """                    binding.etWhatsApp.setText(doc.getString("whatsappNumber") ?: "")
                    binding.etBatch.setText(doc.getString("batch") ?: "")
                    originalBatch = doc.getString("batch") ?: ""
                    binding.etFatherName.setText(doc.getString("fatherName") ?: "")"""
content = content.replace(bad_load, good_load)

bad_save = """            val updates = mapOf(
                "name" to binding.etName.text.toString().trim(),
                "studentId" to binding.etStudentId.text.toString().trim(),
                "phone" to binding.etPhone.text.toString().trim(),"""
good_save = """            val newBatch = binding.etBatch.text.toString().trim()
            val updates = mutableMapOf<String, Any>(
                "name" to binding.etName.text.toString().trim(),
                "studentId" to binding.etStudentId.text.toString().trim(),
                "batch" to newBatch,
                "phone" to binding.etPhone.text.toString().trim(),"""
content = content.replace(bad_save, good_save)

bad_map = """                "presentAddress" to binding.etPresentAddress.text.toString().trim(),
                "permanentAddress" to binding.etPermanentAddress.text.toString().trim(),
                "homeDistrict" to binding.etDistrict.text.toString().trim(),
                "bloodGroup" to binding.etBloodGroup.text.toString().trim()
            )

            db.collection("users").document(uid).update(updates)"""
good_map = """                "presentAddress" to binding.etPresentAddress.text.toString().trim(),
                "permanentAddress" to binding.etPermanentAddress.text.toString().trim(),
                "homeDistrict" to binding.etDistrict.text.toString().trim(),
                "bloodGroup" to binding.etBloodGroup.text.toString().trim()
            )

            if (newBatch != originalBatch && originalBatch.isNotEmpty()) {
                updates["approved"] = false
                android.widget.Toast.makeText(this, "Batch changed. You will need admin approval again.", android.widget.Toast.LENGTH_LONG).show()
            }

            db.collection("users").document(uid).update(updates)"""
content = content.replace(bad_map, good_map)

# add originalBatch variable
bad_class = """class EditProfileActivity : AppCompatActivity() {"""
good_class = """class EditProfileActivity : AppCompatActivity() {
    private var originalBatch: String = ""
"""
content = content.replace(bad_class, good_class)

with open('app/src/main/java/com/shuaib/classmate/activities/EditProfileActivity.kt', 'w') as f:
    f.write(content)
