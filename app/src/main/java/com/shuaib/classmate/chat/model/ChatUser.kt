package com.shuaib.classmate.chat.model

data class ChatUser(
    val id: String,
    val name: String,
    val avatarUrl: String,
    val role: String = "student",
    val isOnline: Boolean = false,
    val email: String = "",
    val isVerified: Boolean = false
) {
    val isBlueVerified: Boolean
        get() {
            val em = email.trim().lowercase()
            return isVerified || em.endsWith(".edu.bd") || em.endsWith("@gstu.ac.bd") || em.contains(".gstu.ac.bd") ||
                    role.lowercase() in listOf("teacher", "admin", "superadmin", "university_admin")
        }
}
