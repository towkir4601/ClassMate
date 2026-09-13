#!/bin/bash

# Add click listeners and show dialog
cat << 'INNEREOF' > TempDialog.kt
    private fun showAddSubjectDialog(type: String) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_subject, null)
        val etName = dialogView.findViewById<android.widget.EditText>(R.id.etSubjectName)
        val etCode = dialogView.findViewById<android.widget.EditText>(R.id.etSubjectCode)

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Add New Course")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val name = etName.text.toString().trim()
                val code = etCode.text.toString().trim()
                if (name.isNotEmpty()) {
                    val newSubject = hashMapOf(
                        "name" to name,
                        "code" to code,
                        "type" to type
                    )
                    db.collection("subjects").add(newSubject)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Course added", Toast.LENGTH_SHORT).show()
                            loadLibraryData()
                        }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
INNEREOF

# I'll create layout file dialog_add_subject.xml
