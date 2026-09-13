import re

with open('app/src/main/java/com/shuaib/classmate/fragments/SubjectPdfListFragment.kt', 'r') as f:
    content = f.read()

bad_fetch = """    private fun fetchPdfs() {
        binding.shimmerView.isVisible = true
        binding.shimmerView.startShimmer()
        binding.rvSubjectPdfs.isVisible = false

        db.collection("library_files")
            .whereEqualTo("subject", args.subjectName)
            .whereEqualTo("isDeleted", false)
            .get()
            .addOnSuccessListener { snapshot ->"""

good_fetch = """    private fun fetchPdfs() {
        binding.shimmerView.isVisible = true
        binding.shimmerView.startShimmer()
        binding.rvSubjectPdfs.isVisible = false

        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get().addOnSuccessListener { userDoc ->
            val userRole = userDoc.getString("role") ?: "student"
            val userBatch = userDoc.getString("batch") ?: ""

            var query = db.collection("library_files")
                .whereEqualTo("subject", args.subjectName)
                .whereEqualTo("isDeleted", false)
                
            // Apply batch filter for students/admins so they only see their own batch's material
            if (userRole != "superadmin" && userRole != "teacher" && userBatch.isNotBlank()) {
                query = query.whereEqualTo("batch", userBatch)
            }

            query.get()
                .addOnSuccessListener { snapshot ->"""

content = content.replace(bad_fetch, good_fetch)

# Add closing bracket for the success listener
bad_end = """            .addOnFailureListener { e ->
                if (_binding == null) return@addOnFailureListener
                binding.shimmerView.stopShimmer()
                binding.shimmerView.isVisible = false
                Toast.makeText(context, "Failed to load resources: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }"""
good_end = """            .addOnFailureListener { e ->
                if (_binding == null) return@addOnFailureListener
                binding.shimmerView.stopShimmer()
                binding.shimmerView.isVisible = false
                Toast.makeText(context, "Failed to load resources: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }"""
content = content.replace(bad_end, good_end)

with open('app/src/main/java/com/shuaib/classmate/fragments/SubjectPdfListFragment.kt', 'w') as f:
    f.write(content)
