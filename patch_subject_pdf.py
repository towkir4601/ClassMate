import re

with open('app/src/main/java/com/shuaib/classmate/fragments/SubjectPdfListFragment.kt', 'r') as f:
    content = f.read()

# Add imports if missing
if 'com.google.firebase.auth.FirebaseAuth' not in content:
    content = content.replace('import com.google.firebase.firestore.FirebaseFirestore', 'import com.google.firebase.auth.FirebaseAuth\nimport com.google.firebase.firestore.FirebaseFirestore')

# Add variables
if 'private var currentUserBatch = ""' not in content:
    content = content.replace('private var allResources = emptyList<PdfFile>()', 'private var allResources = emptyList<PdfFile>()\n    private var currentUserBatch = ""\n    private var isAdmin = false\n    private val auth = FirebaseAuth.getInstance()')

# Modify fetchPdfs to fetch user first
old_fetch = '''    private fun fetchPdfs() {
        val isSwipeRefreshing = binding.swipeRefresh.isRefreshing
        if (!isSwipeRefreshing) {
            binding.shimmerView.isVisible = true
            binding.shimmerView.startShimmer()
            binding.rvSubjectPdfs.isVisible = false
        }
        binding.swipeRefresh.isRefreshing = true
        binding.tvEmptyState.isVisible = false

        db.collection("library_files")'''

new_fetch = '''    private fun fetchPdfs() {
        val isSwipeRefreshing = binding.swipeRefresh.isRefreshing
        if (!isSwipeRefreshing) {
            binding.shimmerView.isVisible = true
            binding.shimmerView.startShimmer()
            binding.rvSubjectPdfs.isVisible = false
        }
        binding.swipeRefresh.isRefreshing = true
        binding.tvEmptyState.isVisible = false

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

if old_fetch in content:
    content = content.replace(old_fetch, new_fetch)

# Add filter by batch
old_filter = '''                allResources = snapshot.documents.map { doc -> doc.toPdfFile() }
                    .filterNot { it.isDeleted }
                    .sortedByDescending { it.timestamp ?: it.createdAt }'''

new_filter = '''                allResources = snapshot.documents.map { doc -> doc.toPdfFile() }
                    .filterNot { it.isDeleted }
                    .filter { isAdmin || it.batch.isEmpty() || it.batch == currentUserBatch }
                    .sortedByDescending { it.timestamp ?: it.createdAt }'''

if old_filter in content:
    content = content.replace(old_filter, new_filter)

with open('app/src/main/java/com/shuaib/classmate/fragments/SubjectPdfListFragment.kt', 'w') as f:
    f.write(content)
print("Patched SubjectPdfListFragment")
