import re

with open('app/src/main/java/com/shuaib/classmate/fragments/LibrarySearchFragment.kt', 'r') as f:
    content = f.read()

if 'private var currentUserBatch = ""' not in content:
    content = content.replace('private var allFiles = emptyList<PdfFile>()', 'private var allFiles = emptyList<PdfFile>()\n    private var currentUserBatch = ""\n    private var isAdmin = false\n    private val auth = FirebaseAuth.getInstance()')
if 'import com.google.firebase.auth.FirebaseAuth' not in content:
    content = content.replace('import com.google.firebase.firestore.FirebaseFirestore', 'import com.google.firebase.auth.FirebaseAuth\nimport com.google.firebase.firestore.FirebaseFirestore')

old_load = '''    private fun loadFiles() {
        binding.shimmerView.isVisible = true
        binding.shimmerView.startShimmer()
        db.collection("library_files")'''

new_load = '''    private fun loadFiles() {
        binding.shimmerView.isVisible = true
        binding.shimmerView.startShimmer()
        val uid = auth.currentUser?.uid
        if (uid != null) {
            db.collection("users").document(uid).get().addOnSuccessListener { doc ->
                currentUserBatch = doc.getString("batch") ?: ""
                val role = doc.getString("role") ?: "student"
                isAdmin = (role == "superadmin" || role == "admin")
                fetchLibraryFiles()
            }.addOnFailureListener {
                fetchLibraryFiles()
            }
        } else {
            fetchLibraryFiles()
        }
    }

    private fun fetchLibraryFiles() {
        db.collection("library_files")'''

content = content.replace(old_load, new_load)

old_filter = '''                allFiles = snapshot.documents.map { doc -> doc.toPdfFile() }
                    .filterNot { it.isDeleted }
                    .sortedByDescending { it.timestamp ?: it.createdAt }'''

new_filter = '''                allFiles = snapshot.documents.map { doc -> doc.toPdfFile() }
                    .filterNot { it.isDeleted }
                    .filter { isAdmin || it.batch.isEmpty() || it.batch == currentUserBatch }
                    .sortedByDescending { it.timestamp ?: it.createdAt }'''

content = content.replace(old_filter, new_filter)

with open('app/src/main/java/com/shuaib/classmate/fragments/LibrarySearchFragment.kt', 'w') as f:
    f.write(content)
print("Patched LibrarySearchFragment")
