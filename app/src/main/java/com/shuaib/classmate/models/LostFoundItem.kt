package com.shuaib.classmate.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

/**
 * GSTU Go - Lost & Found post model.
 * Posts start as [pendingApproval]=true and must be approved by admin/superadmin
 * before they are publicly visible. Once approved, [approved]=true and [pendingApproval]=false.
 */
data class LostFoundItem(
    val id: String = "",
    val type: String = "found",             // "lost" or "found"
    val title: String = "",
    val itemCategory: String = "general",
    val description: String = "",
    val location: String = "",
    val contactPhone: String = "",
    val reporterName: String = "",
    val reporterId: String = "",
    val imageUrl: String = "",              // Optional photo of the item
    val timestamp: Timestamp? = null,
    val isResolved: Boolean = false,

    // Approval workflow (admin must approve before post is public)
    val approved: Boolean = false,
    val pendingApproval: Boolean = true,
    val approvedBy: String = "",            // uid of admin who approved
    val approvedAt: Timestamp? = null,

    // Social & Engagement features
    val loveCount: Int = 0,
    val loveReactors: List<String> = emptyList(), // uids of users who reacted
    val commentsCount: Int = 0,
    val shareToken: String = "",            // Unique deep-link token for this post
    val sharesCount: Int = 0,
    val viewsCount: Int = 0,
    val viewers: List<String> = emptyList(),
    val resolvedStatus: String = "",        // "item_given" or "item_found"
    val reportedCount: Int = 0,
    val reportedBy: List<String> = emptyList()
) {
    @get:Exclude
    val isFound: Boolean get() = type.equals("found", ignoreCase = true)

    @get:Exclude
    val isLost: Boolean get() = type.equals("lost", ignoreCase = true)

    @get:Exclude
    val isPublic: Boolean get() = approved && !pendingApproval

    @get:Exclude
    val isItemGiven: Boolean get() = resolvedStatus.equals("item_given", ignoreCase = true)

    @get:Exclude
    val isItemFound: Boolean get() = resolvedStatus.equals("item_found", ignoreCase = true)
}

