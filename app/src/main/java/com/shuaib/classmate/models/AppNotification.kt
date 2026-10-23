package com.shuaib.classmate.models

import com.google.firebase.Timestamp

/**
 * GSTU Go - Unified Notification Model.
 * Supports:
 * - Admin notifications ("item_given", "item_found", "blood_donated", "report")
 * - User/author notifications ("react", "share")
 */
data class AppNotification(
    val id: String = "",
    val type: String = "",                  // "item_given", "item_found", "blood_donated", "report", "react", "share"
    val title: String = "",
    val message: String = "",
    val targetType: String = "admin",       // "admin" or "user"
    val targetUserId: String = "admin",     // "admin" for all admins/superadmins, or specific UID
    val postId: String = "",
    val postType: String = "",              // "lost_found" or "blood_request"
    val actorId: String = "",
    val actorName: String = "",
    val timestamp: Timestamp? = null,
    val isRead: Boolean = false,
    val readBy: List<String> = emptyList(),
    val extraNote: String = ""
) {
    val isAdminAlert: Boolean get() = targetType.equals("admin", ignoreCase = true) || targetUserId.equals("admin", ignoreCase = true)
}
