import re

# Patch Notice
with open('app/src/main/java/com/shuaib/classmate/models/Notice.kt', 'r') as f:
    content = f.read()

bad_notice = """    val readCount: Int = 0
) {"""
good_notice = """    val readCount: Int = 0,
    val targetBatch: String = "all"
) {"""

content = content.replace(bad_notice, good_notice)
with open('app/src/main/java/com/shuaib/classmate/models/Notice.kt', 'w') as f:
    f.write(content)

# Patch Poll
try:
    with open('app/src/main/java/com/shuaib/classmate/models/Poll.kt', 'r') as f:
        content = f.read()

    bad_poll = """    val votersList: List<String> = emptyList()
) {"""
    good_poll = """    val votersList: List<String> = emptyList(),
    val targetBatch: String = "all"
) {"""

    content = content.replace(bad_poll, good_poll)
    with open('app/src/main/java/com/shuaib/classmate/models/Poll.kt', 'w') as f:
        f.write(content)
except Exception:
    pass
