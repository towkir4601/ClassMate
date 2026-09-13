package com.shuaib.classmate.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.shuaib.classmate.models.Period

@Entity(
    tableName = "timetable_periods",
    indices = [
        Index(value = ["day", "startTime"])
    ]
)
data class TimetableEntity(
    @PrimaryKey val cacheKey: String,
    val day: String,
    val id: String,
    val subject: String,
    val teacher: String,
    val startTime: String,
    val endTime: String,
    val room: String,
    val cancelDate: String,
    val substituteTeacher: String,
    val substituteDate: String,
    val overrideDate: String = "",
    val overrideRoom: String = "",
    val overrideStartTime: String = "",
    val overrideEndTime: String = "",
    val isTemporary: Boolean = false,
    val temporaryDate: String = "",
    val batch: String = "",
    val cachedAtMillis: Long = System.currentTimeMillis()
) {
    fun toPeriod(today: String): Period {
        val isActuallyCancelled = cancelDate == today
        val isActuallySubstitute = substituteDate == today
        val isActuallyOverridden = overrideDate == today
        
        val actualStartTime = if (isActuallyOverridden && overrideStartTime.isNotBlank()) overrideStartTime else startTime
        val actualEndTime = if (isActuallyOverridden && overrideEndTime.isNotBlank()) overrideEndTime else endTime
        val actualRoom = if (isActuallyOverridden) overrideRoom else room
        
        return Period(
            id = id,
            subject = subject,
            teacher = teacher,
            startTime = actualStartTime,
            endTime = actualEndTime,
            room = actualRoom,
            isCancelled = isActuallyCancelled,
            cancelDate = cancelDate,
            isSubstitute = isActuallySubstitute,
            substituteTeacher = if (isActuallySubstitute) substituteTeacher else "",
            substituteDate = substituteDate,
            isDynamicOverride = isActuallyOverridden,
            overrideDate = overrideDate,
            overrideRoom = overrideRoom,
            overrideStartTime = overrideStartTime,
            overrideEndTime = overrideEndTime,
            isTemporary = isTemporary,
            temporaryDate = temporaryDate,
            batch = batch
        )
    }

    companion object {
        fun fromPeriod(day: String, period: Period): TimetableEntity = TimetableEntity(
            cacheKey = cacheKey(day, period.id),
            day = day,
            id = period.id,
            subject = period.subject,
            teacher = period.teacher,
            startTime = period.startTime,
            endTime = period.endTime,
            room = period.room,
            cancelDate = period.cancelDate,
            substituteTeacher = period.substituteTeacher,
            substituteDate = period.substituteDate,
            overrideDate = period.overrideDate,
            overrideRoom = period.overrideRoom,
            overrideStartTime = period.overrideStartTime,
            overrideEndTime = period.overrideEndTime,
            isTemporary = period.isTemporary,
            temporaryDate = period.temporaryDate,
            batch = period.batch
        )

        fun cacheKey(day: String, id: String): String = "${day.lowercase()}:$id"
    }
}
