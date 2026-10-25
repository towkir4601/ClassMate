package com.shuaib.classmate.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * GSTU Go - Comment on a CampusFeed post.
 */
@IgnoreExtraProperties
data class CampusFeedComment(
    val id: String = "",
    val postId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorHandle: String = "",
    val authorAvatar: String = "",
    val authorRole: String = "student",
    val content: String = "",
    val timestamp: Timestamp? = null,
    val likesCount: Int = 0,
    val likers: List<String> = emptyList()
) {
    fun isLikedBy(userId: String): Boolean =
        userId.isNotBlank() && likers.contains(userId)
}
