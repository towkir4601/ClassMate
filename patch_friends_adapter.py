import re

with open('app/src/main/java/com/shuaib/classmate/adapters/FriendsAdapter.kt', 'r') as f:
    content = f.read()

bad = """            val details = listOfNotNull(
                friend.studentId.takeIf { it.isNotBlank() },
                friend.batch.takeIf { it.isNotBlank() }?.let { "Batch: $it" },
                friend.bloodGroup.takeIf { it.isNotBlank() },
                friend.homeDistrict.takeIf { it.isNotBlank() }
            ).joinToString(" • ")"""
good = """            val details = if (friend.role == "teacher") {
                listOfNotNull(
                    friend.department.takeIf { it.isNotBlank() }?.let { "Dept: $it" },
                    friend.bloodGroup.takeIf { it.isNotBlank() }
                ).joinToString(" • ")
            } else {
                listOfNotNull(
                    friend.studentId.takeIf { it.isNotBlank() },
                    friend.batch.takeIf { it.isNotBlank() }?.let { "Batch: $it" },
                    friend.bloodGroup.takeIf { it.isNotBlank() },
                    friend.homeDistrict.takeIf { it.isNotBlank() }
                ).joinToString(" • ")
            }"""
content = content.replace(bad, good)
with open('app/src/main/java/com/shuaib/classmate/adapters/FriendsAdapter.kt', 'w') as f:
    f.write(content)
