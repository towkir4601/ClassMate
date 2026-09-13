with open("app/src/main/java/com/shuaib/classmate/activities/PdfUploadActivity.kt", "r") as f:
    content = f.read()

# 1. Add listener for btnDeleteSubject in onCreate
old_add_btn_listener = """        binding.btnAddSubject.setOnClickListener {
            showAddSubjectDialog()
        }"""
new_add_btn_listener = """        binding.btnAddSubject.setOnClickListener {
            showAddSubjectDialog()
        }
        binding.btnDeleteSubject.setOnClickListener {
            showDeleteSubjectDialog()
        }
        
        binding.dropdownSubject.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.btnDeleteSubject.visibility = if (s.isNullOrEmpty()) android.view.View.GONE else android.view.View.VISIBLE
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })"""

content = content.replace(old_add_btn_listener, new_add_btn_listener)

# 2. Add showDeleteSubjectDialog function
show_add = """    private fun showAddSubjectDialog() {"""
show_delete = """    private fun showDeleteSubjectDialog() {
        val subjectName = binding.dropdownSubject.text.toString().trim()
        if (subjectName.isEmpty()) return
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Course")
            .setMessage("Are you sure you want to delete '$subjectName'? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                val subject = com.shuaib.classmate.models.SubjectList.subjects.find { it.name.equals(subjectName, ignoreCase = true) }
                if (subject != null && subject.id.isNotEmpty()) {
                    db.collection("subjects").document(subject.id).delete()
                        .addOnSuccessListener {
                            Toast.makeText(this, "Course deleted", Toast.LENGTH_SHORT).show()
                            binding.dropdownSubject.setText("", false)
                            binding.etCourseCode.setText("")
                            binding.btnDeleteSubject.visibility = android.view.View.GONE
                            com.shuaib.classmate.models.SubjectList.fetchSubjects {
                                setupDropdown()
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Failed to delete: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "Course not found", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddSubjectDialog() {"""

content = content.replace(show_add, show_delete)

with open("app/src/main/java/com/shuaib/classmate/activities/PdfUploadActivity.kt", "w") as f:
    f.write(content)
