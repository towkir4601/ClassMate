import re

with open('app/src/main/java/com/shuaib/classmate/fragments/LibraryAllFilesFragment.kt', 'r') as f:
    content = f.read()

# Add currentUserBatch variable
if 'private var currentUserBatch = ""' not in content:
    content = content.replace('private var isAdmin = false', 'private var isAdmin = false\n    private var currentUserBatch = ""')

# Modify checkAdminStatus
old_check = '''    private fun checkAdminStatus() {
        val uid = auth.currentUser?.uid ?: run {
            loadAllFiles()
            return
        }
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (_binding == null) return@addOnSuccessListener
                val role = doc.getString("role") ?: "student"
                val canUploadPdf = doc.getBoolean("permissions.canUploadPDF") ?: false
                val canUploadLibrary = doc.getBoolean("permissions.canUploadLibrary") ?: false
                isAdmin = role == "superadmin" || role == "admin" || canUploadPdf || canUploadLibrary
                
                pdfAdapter = PdfAdapter(emptyList(), isAdmin)'''

new_check = '''    private fun checkAdminStatus() {
        val uid = auth.currentUser?.uid ?: run {
            loadAllFiles()
            return
        }
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (_binding == null) return@addOnSuccessListener
                val role = doc.getString("role") ?: "student"
                currentUserBatch = doc.getString("batch") ?: ""
                val canUploadPdf = doc.getBoolean("permissions.canUploadPDF") ?: false
                val canUploadLibrary = doc.getBoolean("permissions.canUploadLibrary") ?: false
                isAdmin = role == "superadmin" || role == "admin"
                val canManage = isAdmin || canUploadPdf || canUploadLibrary
                
                pdfAdapter = PdfAdapter(emptyList(), canManage)'''

content = content.replace(old_check, new_check)

# Add batch filter in loadAllFiles
old_filter = '''                val files = snapshot.documents.map { doc -> doc.toPdfFile() }
                    .filter { !it.isDeleted }
                    .sortedByDescending { it.timestamp ?: it.createdAt }'''

new_filter = '''                val files = snapshot.documents.map { doc -> doc.toPdfFile() }
                    .filter { !it.isDeleted }
                    .filter { isAdmin || it.batch.isEmpty() || it.batch == currentUserBatch }
                    .sortedByDescending { it.timestamp ?: it.createdAt }'''

content = content.replace(old_filter, new_filter)

with open('app/src/main/java/com/shuaib/classmate/fragments/LibraryAllFilesFragment.kt', 'w') as f:
    f.write(content)
print("Patched LibraryAllFilesFragment")
