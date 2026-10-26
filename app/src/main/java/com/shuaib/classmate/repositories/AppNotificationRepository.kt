package com.shuaib.classmate.repositories

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.shuaib.classmate.models.AppNotification

class AppNotificationRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val collection = firestore.collection("notifications")

    companion object {
        private val localNotifications = mutableListOf<AppNotification>()

        fun addLocal(notification: AppNotification) {
            localNotifications.removeAll { it.id == notification.id }
            localNotifications.add(0, notification)
        }
    }

    /**
     * Dispatch notification to superadmin and admin (for reports, item given, item found, blood donated).
     */
    fun sendNotificationToAdmins(
        type: String,
        title: String,
        message: String,
        postId: String = "",
        postType: String = "",
        actorId: String = "",
        actorName: String = "",
        extraNote: String = "",
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        val docRef = collection.document()
        val notif = AppNotification(
            id = docRef.id,
            type = type,
            title = title,
            message = message,
            targetType = "admin",
            targetUserId = "admin",
            postId = postId,
            postType = postType,
            actorId = actorId,
            actorName = actorName,
            extraNote = extraNote,
            timestamp = Timestamp.now(),
            isRead = false,
            readBy = emptyList()
        )
        addLocal(notif)
        docRef.set(notif)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    /**
     * Dispatch notification to the original post author (for react or share events).
     */
    fun sendNotificationToAuthor(
        authorId: String,
        type: String,
        title: String,
        message: String,
        postId: String = "",
        postType: String = "",
        actorId: String = "",
        actorName: String = "",
        extraNote: String = "",
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        if (authorId.isBlank() || (actorId.isNotBlank() && authorId == actorId)) {
            // Do not notify self
            return
        }

        val docRef = collection.document()
        val notif = AppNotification(
            id = docRef.id,
            type = type,
            title = title,
            message = message,
            targetType = "user",
            targetUserId = authorId,
            postId = postId,
            postType = postType,
            actorId = actorId,
            actorName = actorName,
            extraNote = extraNote,
            timestamp = Timestamp.now(),
            isRead = false,
            readBy = emptyList()
        )
        addLocal(notif)
        docRef.set(notif)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    /**
     * Listen in real-time to notifications relevant to this user.
     * Admins receive both admin notifications and their own author notifications.
     * Regular users receive their own author notifications.
     */
    fun listenNotifications(
        userId: String,
        isAdmin: Boolean,
        onUpdate: (List<AppNotification>) -> Unit
    ): ListenerRegistration {
        return collection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(60)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    val fallback = localNotifications.filter { n ->
                        (isAdmin && (n.targetUserId == "admin" || n.targetType == "admin")) || n.targetUserId == userId
                    }
                    onUpdate(fallback)
                    return@addSnapshotListener
                }

                val remote = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(AppNotification::class.java)?.copy(id = doc.id)
                }

                val merged = (remote + localNotifications)
                    .distinctBy { it.id }
                    .filter { n ->
                        if (isAdmin) {
                            n.targetUserId == "admin" || n.targetType == "admin" || n.targetUserId == userId
                        } else {
                            n.targetUserId == userId
                        }
                    }
                    .sortedByDescending { it.timestamp?.seconds ?: 0L }

                onUpdate(merged)
            }
    }

    /**
     * Mark a single notification as read by this user.
     */
    fun markAsRead(notificationId: String, userId: String) {
        synchronized(localNotifications) {
            val idx = localNotifications.indexOfFirst { it.id == notificationId }
            if (idx >= 0) {
                val item = localNotifications[idx]
                val newReadBy = if (userId.isNotBlank() && !item.readBy.contains(userId)) item.readBy + userId else item.readBy
                localNotifications[idx] = item.copy(isRead = true, readBy = newReadBy)
            }
        }
        val updates = mutableMapOf<String, Any>("isRead" to true)
        if (userId.isNotBlank()) {
            updates["readBy"] = FieldValue.arrayUnion(userId)
        }
        collection.document(notificationId).update(updates)
            .addOnFailureListener { e ->
                android.util.Log.e("AppNotifRepo", "Failed to mark as read: ${e.message}", e)
            }
    }

    /**
     * Mark all given notifications as read.
     */
    fun markAllAsRead(notifications: List<AppNotification>, userId: String) {
        val notifIds = notifications.map { it.id }.toSet()
        synchronized(localNotifications) {
            for (i in localNotifications.indices) {
                if (localNotifications[i].id in notifIds) {
                    val item = localNotifications[i]
                    val newReadBy = if (userId.isNotBlank() && !item.readBy.contains(userId)) item.readBy + userId else item.readBy
                    localNotifications[i] = item.copy(isRead = true, readBy = newReadBy)
                }
            }
        }
        if (notifications.isEmpty()) return
        val batch = firestore.batch()
        for (n in notifications) {
            val docRef = collection.document(n.id)
            val updates = mutableMapOf<String, Any>("isRead" to true)
            if (userId.isNotBlank()) {
                updates["readBy"] = FieldValue.arrayUnion(userId)
            }
            batch.update(docRef, updates)
        }
        batch.commit()
            .addOnFailureListener { e ->
                android.util.Log.e("AppNotifRepo", "Failed to mark all as read: ${e.message}", e)
            }
    }
}
