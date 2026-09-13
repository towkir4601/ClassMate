    private fun parseUserSafely(doc: com.google.firebase.firestore.DocumentSnapshot): com.shuaib.classmate.models.User? {
        if (!doc.exists()) return null
        return try {
            com.shuaib.classmate.models.User(
                uid = doc.id,
                name = doc.getString("name") ?: "",
                email = doc.getString("email") ?: "",
                photoUrl = doc.getString("photoUrl") ?: "",
                studentId = doc.getString("studentId") ?: "",
                phone = doc.getString("phone") ?: "",
                bloodGroup = doc.getString("bloodGroup") ?: "",
                homeDistrict = doc.getString("homeDistrict") ?: "",
                address = doc.getString("address") ?: "",
                role = doc.getString("role") ?: "student",
                isVerified = doc.getBoolean("isVerified") ?: false,
                isBanned = doc.getBoolean("isBanned") ?: false
            )
        } catch (e: Exception) {
            null
        }
    }
