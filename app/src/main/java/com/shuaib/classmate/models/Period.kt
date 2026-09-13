// com/shuaib/classmate/models/Period.kt
package com.shuaib.classmate.models

data class Period(
    val id: String = "",
    val subject: String = "",
    val teacher: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val room: String = "", // Default room

    // Cancellation fields
    val isCancelled: Boolean = false,
    val cancelDate: String = "",
    // Format: "2026-04-11" (yyyy-MM-dd)
    // Empty string means not cancelled

    // Substitute fields
    val isSubstitute: Boolean = false,
    val substituteTeacher: String = "",
    val substituteDate: String = "",
    // Format: "2026-04-11" (yyyy-MM-dd)

    // Dynamic Override fields (for a specific date only)
    val isDynamicOverride: Boolean = false,
    val overrideDate: String = "",
    val overrideRoom: String = "",
    val overrideStartTime: String = "",
    val overrideEndTime: String = "",

    // Temporary class for today only
    val isTemporary: Boolean = false,
    val temporaryDate: String = "",
    
    // Batch
    val batch: String = ""
)
