import re

with open('app/src/main/java/com/shuaib/classmate/fragments/LibraryAllFilesFragment.kt', 'r') as f:
    content = f.read()

bad_fetch = """    private fun loadAllFiles() {
        binding.shimmerView.isVisible = true
        binding.shimmerView.startShimmer()
        binding.rvAllFiles.isVisible = false

        db.collection("library_files")
            .whereEqualTo("isDeleted", false)
            .get()
            .addOnSuccessListener { snapshot ->"""

good_fetch = """    private fun loadAllFiles() {
        binding.shimmerView.isVisible = true
        binding.shimmerView.startShimmer()
        binding.rvAllFiles.isVisible = false

        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get().addOnSuccessListener { userDoc ->
            val userRole = userDoc.getString("role") ?: "student"
            val userBatch = userDoc.getString("batch") ?: ""

            var query = db.collection("library_files")
                .whereEqualTo("isDeleted", false)
                
            if (userRole != "superadmin" && userRole != "teacher" && userBatch.isNotBlank()) {
                query = query.whereEqualTo("batch", userBatch)
            }

            query.get()
                .addOnSuccessListener { snapshot ->"""

content = content.replace(bad_fetch, good_fetch)

bad_end = """            .addOnFailureListener { e ->
                if (_binding == null) return@addOnFailureListener
                binding.shimmerView.stopShimmer()
                binding.shimmerView.isVisible = false
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                binding.swipeRefresh.isRefreshing = false
            }
    }"""
good_end = """            .addOnFailureListener { e ->
                if (_binding == null) return@addOnFailureListener
                binding.shimmerView.stopShimmer()
                binding.shimmerView.isVisible = false
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }"""
content = content.replace(bad_end, good_end)

with open('app/src/main/java/com/shuaib/classmate/fragments/LibraryAllFilesFragment.kt', 'w') as f:
    f.write(content)
