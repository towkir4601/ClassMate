package com.shuaib.classmate.models

import com.google.firebase.Timestamp

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
    val timestamp: Timestamp? = null,
    val isFulfilled: Boolean = false
)
