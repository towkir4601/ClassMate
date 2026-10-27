package com.shuaib.classmate.models

import com.google.firebase.Timestamp

data class RoutineSwapRequest(
    val id: String = "",
    val date: String = "",              // Format: "yyyy-MM-dd"
    val day: String = "",               // e.g. "saturday", "sunday", etc.
    val department: String = "",
    val batch: String = "",

    // Requester info & period
    val requesterUid: String = "",
    val requesterName: String = "",
    val requesterPeriodId: String = "",
    val requesterSubject: String = "",
    val requesterStartTime: String = "",
    val requesterEndTime: String = "",
    val requesterRoom: String = "",

    // Target teacher info & period
    val targetUid: String = "",
    val targetTeacherName: String = "",
    val targetPeriodId: String = "",
    val targetSubject: String = "",
    val targetStartTime: String = "",
    val targetEndTime: String = "",
    val targetRoom: String = "",

    val note: String = "",
    val status: String = STATUS_PENDING, // "pending", "accepted", "rejected", "cancelled"
    val createdAt: Timestamp? = null,
    val resolvedAt: Timestamp? = null
) {
    companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_ACCEPTED = "accepted"
        const val STATUS_REJECTED = "rejected"
        const val STATUS_CANCELLED = "cancelled"
    }
}
