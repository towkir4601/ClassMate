import re

with open('app/src/main/java/com/shuaib/classmate/activities/RegisterActivity.kt', 'r') as f:
    content = f.read()

bad = """                "photoUrl" to photoUrl,
                "role" to if (isTeacher) "teacher" else "student",
                "approved" to false,
                "permissions" to User.DEFAULT_PERMISSIONS,
                "favoriteSubjects" to emptyList<String>(),
                "favoritePdfIds" to emptyList<String>(),
                "authProvider" to "google",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )"""

good = """                "photoUrl" to photoUrl,
                "role" to if (isTeacher) "teacher" else "student",
                "approved" to false,
                "permissions" to User.DEFAULT_PERMISSIONS,
                "favoriteSubjects" to emptyList<String>(),
                "favoritePdfIds" to emptyList<String>(),
                "authProvider" to "google",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
                "batch" to "",
                "phone" to "",
                "whatsappNumber" to "",
                "fatherName" to "",
                "motherName" to "",
                "presentAddress" to "",
                "permanentAddress" to "",
                "homeDistrict" to "",
                "bloodGroup" to ""
            )"""

content = content.replace(bad, good)

with open('app/src/main/java/com/shuaib/classmate/activities/RegisterActivity.kt', 'w') as f:
    f.write(content)
