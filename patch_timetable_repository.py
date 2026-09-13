import re

with open('app/src/main/java/com/shuaib/classmate/repositories/TimetableRepository.kt', 'r') as f:
    content = f.read()

bad_mapping = """                overrideDate = doc.getString("overrideDate") ?: "",
                overrideRoom = doc.getString("overrideRoom") ?: "",
                overrideStartTime = doc.getString("overrideStartTime") ?: "",
                overrideEndTime = doc.getString("overrideEndTime") ?: ""
            )"""
good_mapping = """                overrideDate = doc.getString("overrideDate") ?: "",
                overrideRoom = doc.getString("overrideRoom") ?: "",
                overrideStartTime = doc.getString("overrideStartTime") ?: "",
                overrideEndTime = doc.getString("overrideEndTime") ?: "",
                isTemporary = doc.getBoolean("isTemporary") ?: false,
                temporaryDate = doc.getString("temporaryDate") ?: "",
                batch = doc.getString("batch") ?: ""
            )"""
content = content.replace(bad_mapping, good_mapping)

with open('app/src/main/java/com/shuaib/classmate/repositories/TimetableRepository.kt', 'w') as f:
    f.write(content)
