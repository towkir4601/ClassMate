import re

with open('app/src/main/java/com/shuaib/classmate/activities/PdfUploadActivity.kt', 'r') as f:
    content = f.read()

# 1. Add currentUserBatch
content = content.replace('private var currentUserName = ""', 'private var currentUserName = ""\n    private var currentUserBatch = ""')

# 2. Populate currentUserBatch
content = content.replace('currentUserName = doc.getString("name") ?: "Admin"', 'currentUserName = doc.getString("name") ?: "Admin"\n                currentUserBatch = doc.getString("batch") ?: ""')

# 3. Add to hashMaps
content = content.replace('"isDeleted" to false\n        )', '"isDeleted" to false,\n            "batch" to currentUserBatch\n        )')

with open('app/src/main/java/com/shuaib/classmate/activities/PdfUploadActivity.kt', 'w') as f:
    f.write(content)

