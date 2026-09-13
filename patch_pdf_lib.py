import re

with open('app/src/main/java/com/shuaib/classmate/fragments/PdfLibraryFragment.kt', 'r') as f:
    content = f.read()

# Add currentUserBatch
content = content.replace('private var isAdmin = false', 'private var isAdmin = false\n    private var currentUserBatch = ""')

# Populate currentUserBatch
content = content.replace('isAdmin = (role == "superadmin"', 'currentUserBatch = doc.getString("batch") ?: ""\n                    isAdmin = (role == "superadmin"')

# Filter in fetch
fetch_old = """                allPdfs = snapshot.documents.map { doc -> doc.toPdfFile() }
                    .filterNot { it.isDeleted }
                    .sortedByDescending { it.timestamp ?: it.createdAt }"""
fetch_new = """                allPdfs = snapshot.documents.map { doc -> doc.toPdfFile() }
                    .filterNot { it.isDeleted }
                    .filter { isAdmin || it.batch.isEmpty() || it.batch == currentUserBatch }
                    .sortedByDescending { it.timestamp ?: it.createdAt }"""
content = content.replace(fetch_old, fetch_new)

with open('app/src/main/java/com/shuaib/classmate/fragments/PdfLibraryFragment.kt', 'w') as f:
    f.write(content)

