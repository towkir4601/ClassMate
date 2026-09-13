with open("app/src/main/java/com/shuaib/classmate/fragments/FriendsFragment.kt", "r") as f:
    content = f.read()

old_block = """            selectedDistrict?.let { district ->
                if (isTeacherMode) {
                    filtered = filtered.filter { it.department.equals(district, ignoreCase = true) }
                } else {
                    filtered = filtered.filter { it.homeDistrict.equals(district, ignoreCase = true) }
                }
            }
        }"""

new_block = """            selectedDistrict?.let { district ->
                if (isTeacherMode) {
                    filtered = filtered.filter { it.department.equals(district, ignoreCase = true) }
                } else {
                    filtered = filtered.filter { it.homeDistrict.equals(district, ignoreCase = true) }
                }
            }
            selectedBatch?.let { batch ->
                filtered = filtered.filter { it.batch.equals(batch, ignoreCase = true) }
            }
        }"""

content = content.replace(old_block, new_block)

with open("app/src/main/java/com/shuaib/classmate/fragments/FriendsFragment.kt", "w") as f:
    f.write(content)
