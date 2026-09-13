import re

with open('app/src/main/java/com/shuaib/classmate/models/Period.kt', 'r') as f:
    content = f.read()

bad = """    // Temporary class for today only
    val isTemporary: Boolean = false,
    val temporaryDate: String = ""
)"""
good = """    // Temporary class for today only
    val isTemporary: Boolean = false,
    val temporaryDate: String = "",
    
    // Batch
    val batch: String = ""
)"""
content = content.replace(bad, good)

with open('app/src/main/java/com/shuaib/classmate/models/Period.kt', 'w') as f:
    f.write(content)
