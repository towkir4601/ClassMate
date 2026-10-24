package com.shuaib.classmate.models

import com.google.firebase.Timestamp

data class LostFoundItem(
    val id: String = "",
    val type: String = "found", // "lost" or "found"
    val title: String = "",
    val itemCategory: String = "general",
    val description: String = "",
    val location: String = "",
    val contactPhone: String = "",
    val reporterName: String = "",
    val reporterId: String = "",
    val imageUrl: String = "",
    val timestamp: Timestamp? = null,
    val isResolved: Boolean = false
) {
    val isFound: Boolean get() = type.equals("found", ignoreCase = true)
    val isLost: Boolean get() = type.equals("lost", ignoreCase = true)
}
