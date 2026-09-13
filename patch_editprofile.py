with open("app/src/main/java/com/shuaib/classmate/activities/EditProfileActivity.kt", "r") as f:
    content = f.read()

old_block = """                    binding.etBloodGroup.setText(doc.getString("bloodGroup") ?: "")
                }
            }"""

new_block = """                    binding.etBloodGroup.setText(doc.getString("bloodGroup") ?: "")
                    
                    if (doc.getString("role") == "teacher") {
                        (binding.etStudentId.parent as? android.view.View)?.visibility = android.view.View.GONE
                        (binding.etBatch.parent as? android.view.View)?.visibility = android.view.View.GONE
                        (binding.etFatherName.parent as? android.view.View)?.visibility = android.view.View.GONE
                        (binding.etMotherName.parent as? android.view.View)?.visibility = android.view.View.GONE
                    }
                }
            }"""

content = content.replace(old_block, new_block)

with open("app/src/main/java/com/shuaib/classmate/activities/EditProfileActivity.kt", "w") as f:
    f.write(content)
