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

                val merged = (localNotifications + remote)
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
        collection.document(notificationId).update(
            "isRead", true,
            "readBy", FieldValue.arrayUnion(userId)
        )
    }

    /**
     * Mark all given notifications as read.
     */
    fun markAllAsRead(notifications: List<AppNotification>, userId: String) {
        val batch = firestore.batch()
        for (n in notifications) {
            val docRef = collection.document(n.id)
            batch.update(docRef, "isRead", true)
            if (userId.isNotBlank()) {
                batch.update(docRef, "readBy", FieldValue.arrayUnion(userId))
            }
        }
        batch.commit()
    }
}
