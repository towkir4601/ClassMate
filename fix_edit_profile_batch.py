import re

with open('app/src/main/java/com/shuaib/classmate/activities/EditProfileActivity.kt', 'r') as f:
    content = f.read()

# Add originalBatch tracking
bad_class = "class EditProfileActivity : AppCompatActivity() {"
good_class = "class EditProfileActivity : AppCompatActivity() {\n    private var originalBatch: String = \"\""
if good_class not in content:
    content = content.replace(bad_class, good_class)

bad_load = """                    binding.etWhatsApp.setText(doc.getString("whatsappNumber") ?: "")
                    binding.etBatch.setText(doc.getString("batch") ?: "")
                    binding.etFatherName.setText(doc.getString("fatherName") ?: "")"""
good_load = """                    binding.etWhatsApp.setText(doc.getString("whatsappNumber") ?: "")
                    binding.etBatch.setText(doc.getString("batch") ?: "")
                    originalBatch = doc.getString("batch") ?: ""
                    binding.etFatherName.setText(doc.getString("fatherName") ?: "")"""
if good_load not in content:
    content = content.replace(bad_load, good_load)
    
# Update save logic
bad_save = """        val updates = hashMapOf<String, Any>(
            "name" to binding.etName.text.toString().trim(),
            "studentId" to binding.etStudentId.text.toString().trim(),
            "phone" to binding.etPhone.text.toString().trim(),
            "whatsappNumber" to binding.etWhatsApp.text.toString().trim(),
            "batch" to binding.etBatch.text.toString().trim(),
            "fatherName" to binding.etFatherName.text.toString().trim(),
            "motherName" to binding.etMotherName.text.toString().trim(),
            "presentAddress" to binding.etPresentAddress.text.toString().trim(),
            "permanentAddress" to binding.etPermanentAddress.text.toString().trim(),
            "homeDistrict" to binding.etDistrict.text.toString().trim(),
            "bloodGroup" to binding.etBloodGroup.text.toString().trim(),
            "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )
        
        binding.btnSave.isEnabled = false
        binding.btnSave.text = "Saving..."
        
        db.collection("users").document(uid)
            .set(updates, SetOptions.merge())"""

good_save = """        val newBatch = binding.etBatch.text.toString().trim()
        val updates = hashMapOf<String, Any>(
            "name" to binding.etName.text.toString().trim(),
            "studentId" to binding.etStudentId.text.toString().trim(),
            "phone" to binding.etPhone.text.toString().trim(),
            "whatsappNumber" to binding.etWhatsApp.text.toString().trim(),
            "batch" to newBatch,
            "fatherName" to binding.etFatherName.text.toString().trim(),
            "motherName" to binding.etMotherName.text.toString().trim(),
            "presentAddress" to binding.etPresentAddress.text.toString().trim(),
            "permanentAddress" to binding.etPermanentAddress.text.toString().trim(),
            "homeDistrict" to binding.etDistrict.text.toString().trim(),
            "bloodGroup" to binding.etBloodGroup.text.toString().trim(),
            "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )
        
        if (newBatch != originalBatch && originalBatch.isNotEmpty()) {
            updates["approved"] = false
            android.widget.Toast.makeText(this, "Batch changed. You will need admin approval again.", android.widget.Toast.LENGTH_LONG).show()
        }
        
        binding.btnSave.isEnabled = false
        binding.btnSave.text = "Saving..."
        
        db.collection("users").document(uid)
            .set(updates, SetOptions.merge())"""
content = content.replace(bad_save, good_save)

with open('app/src/main/java/com/shuaib/classmate/activities/EditProfileActivity.kt', 'w') as f:
    f.write(content)
