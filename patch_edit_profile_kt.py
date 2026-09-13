import re

with open('app/src/main/java/com/shuaib/classmate/activities/EditProfileActivity.kt', 'r') as f:
    content = f.read()

bad1 = """                if (doc.exists()) {
                    binding.etPhone.setText(doc.getString("phone") ?: "")"""
good1 = """                if (doc.exists()) {
                    binding.etName.setText(doc.getString("name") ?: "")
                    binding.etStudentId.setText(doc.getString("studentId") ?: "")
                    binding.etPhone.setText(doc.getString("phone") ?: "")"""
content = content.replace(bad1, good1)

bad2 = """        val updates = hashMapOf<String, Any>(
            "phone" to binding.etPhone.text.toString().trim(),"""
good2 = """        val updates = hashMapOf<String, Any>(
            "name" to binding.etName.text.toString().trim(),
            "studentId" to binding.etStudentId.text.toString().trim(),
            "phone" to binding.etPhone.text.toString().trim(),"""
content = content.replace(bad2, good2)

with open('app/src/main/java/com/shuaib/classmate/activities/EditProfileActivity.kt', 'w') as f:
    f.write(content)
