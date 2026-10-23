package com.shuaib.classmate.models

import com.google.firebase.Timestamp

/**
 * GSTU Go - Comment on a community post (Lost & Found or Blood Request).
 *
 * Firestore path: community_comments/{commentId}
 * Collection query: where("postId", "==", id) and where("postType", "==", "lostfound"|"blood")
 */
data class PostComment(
    val id: String = "",
    val postId: String = "",
    val postType: String = "",          // "lostfound" or "blood"
    val authorId: String = "",
    val authorName: String = "",
    val authorPhotoUrl: String = "",
    val text: String = "",
    val timestamp: Timestamp? = null,
    val isDeleted: Boolean = false      // Soft-delete for admin moderation
)
