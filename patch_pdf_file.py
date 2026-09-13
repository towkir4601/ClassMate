import re

with open('app/src/main/java/com/shuaib/classmate/models/PdfFile.kt', 'r') as f:
    content = f.read()

replacement = """    val downloadCount: Long = 0L,
    val isDeleted: Boolean = false,
    val batch: String = ""
)"""
content = content.replace("    val downloadCount: Long = 0L,\n    val isDeleted: Boolean = false\n)", replacement)

with open('app/src/main/java/com/shuaib/classmate/models/PdfFile.kt', 'w') as f:
    f.write(content)
