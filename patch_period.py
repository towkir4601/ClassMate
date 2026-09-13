import re

with open('app/src/main/java/com/shuaib/classmate/models/Period.kt', 'r') as f:
    content = f.read()

replacement = """    val isDynamicOverride: Boolean = false,
    val overrideDate: String = "",
    val overrideRoom: String = "",
    val overrideStartTime: String = "",
    val overrideEndTime: String = "",

    // Temporary class for today only
    val isTemporary: Boolean = false,
    val temporaryDate: String = ""
)"""
content = content.replace("    val isDynamicOverride: Boolean = false,\n    val overrideDate: String = \"\",\n    val overrideRoom: String = \"\",\n    val overrideStartTime: String = \"\",\n    val overrideEndTime: String = \"\"\n)", replacement)

with open('app/src/main/java/com/shuaib/classmate/models/Period.kt', 'w') as f:
    f.write(content)
