package com.shuaib.classmate.models

data class BatchListResponse(
    val success: Boolean = false,
    val batches: List<BatchItem>? = emptyList(),
    val message: String? = null
)

data class BatchDetailResponse(
    val success: Boolean = false,
    val batch: BatchItem? = null,
    val message: String? = null
)

data class BatchCounts(
    val teachers: Int? = 0,
    val courses: Int? = 0,
    val rooms: Int? = 0,
    val assignments: Int? = 0
)

data class BatchItem(
    val id: Int = 0,
    val filename: String? = null,
    val faculty: String? = null,
    val department: String? = null,
    val year: String? = null,
    val semester: String? = null,
    val status: String? = null,
    val created_at: String? = null,
    val counts: BatchCounts? = null,
    val has_schedule: Boolean = false,
    val error_log: Any? = null
)

data class ScheduleAssignment(
    val course_code: String = "",
    val course_name: String? = null,
    val teacher_abbr: String = "",
    val room_id: String = "",
    val day: String = "",
    val slot_start: Any? = null,
    val slot_end: Any? = null,
    val year_sem: String = "",
    val session_index: Int? = 1
)

data class ScheduleResponse(
    val success: Boolean = false,
    val code: String? = null,
    val message: String? = null,
    val batch_id: Int? = null,
    val assignments_count: Int? = null,
    val score: Double? = null,
    val assignments: List<ScheduleAssignment>? = emptyList(),
    val config: Map<String, Any?>? = emptyMap(),
    val year_sem_list: List<String>? = emptyList(),
    val day_list: List<String>? = emptyList()
)
