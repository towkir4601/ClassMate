package com.shuaib.classmate.models

import com.google.firebase.Timestamp

/**
 * GSTU Go - Blood donation request post.
 * Visible to all users. Blood requests are NOT subject to admin approval —
 * they are visible immediately so help reaches the requester faster.
 */
data class BloodRequest(
    val id: String = "",
    val bloodGroup: String = "",
    val patientName: String = "",
    val hospitalName: String = "",
    val contactPhone: String = "",
    val bagsNeeded: Int = 1,
    val urgencyNote: String = "",
    val requesterId: String = "",
    val requesterName: String = "",
    val imageUrl: String = "",              // Optional supporting photo
    val timestamp: Timestamp? = null,
    val isFulfilled: Boolean = false,

    // Social features
    val loveCount: Int = 0,
    val loveReactors: List<String> = emptyList(), // uids of users who reacted
    val commentsCount: Int = 0,
    val shareToken: String = ""             // Unique deep-link token for this post
)

