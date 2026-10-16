// com/shuaib/classmate/utils/DateHelper.kt
package com.shuaib.classmate.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object DateHelper {

    private val dateFormat = SimpleDateFormat(
        "yyyy-MM-dd", Locale.US
    )

    // Get today's date as "yyyy-MM-dd"
    fun today(): String {
        return dateFormat.format(Calendar.getInstance().time)
    }

    // Get tomorrow's date as "yyyy-MM-dd"
    fun tomorrow(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 1)
        return dateFormat.format(cal.time)
    }

    // Get day string for Firestore
    // e.g "saturday", "sunday" etc
    fun todayDayString(): String {
        val cal = Calendar.getInstance()
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SATURDAY  -> "saturday"
            Calendar.SUNDAY    -> "sunday"
            Calendar.MONDAY    -> "monday"
            Calendar.TUESDAY   -> "tuesday"
            Calendar.WEDNESDAY -> "wednesday"
            Calendar.THURSDAY  -> "thursday"
            Calendar.FRIDAY     -> "friday"
            else               -> "saturday"
        }
    }

    // Get tomorrow's day string
    fun tomorrowDayString(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 1)
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SATURDAY  -> "saturday"
            Calendar.SUNDAY    -> "sunday"
            Calendar.MONDAY    -> "monday"
            Calendar.TUESDAY   -> "tuesday"
            Calendar.WEDNESDAY -> "wednesday"
            Calendar.THURSDAY  -> "thursday"
            Calendar.FRIDAY     -> "friday"
            else               -> "saturday"
        }
    }

    // Check if a cancelDate is still active (today or future)
    fun isCancellationActive(cancelDate: String): Boolean {
        if (cancelDate.isEmpty()) return false
        return cancelDate == today()
    }

    // Format for display e.g "Today, Apr 11"
    fun formatForDisplay(dateStr: String): String {
        return when (dateStr) {
            today() -> "Today"
            tomorrow() -> "Tomorrow"
            else -> dateStr
        }
    }

    /**
     * Accurately parses any time string (e.g. "09:00 AM", "2:10 PM", "14:10", "11:30 am")
     * into minutes from midnight (0..1439).
     * Prevents string-sorting bugs (where "02:10 PM" came before "09:00 AM").
     */
    fun parseTimeToMinutes(timeStr: String): Int {
        val clean = timeStr.trim()
        if (clean.isBlank()) return 0

        // 1. Try 12-hour format with AM/PM (e.g. "09:00 AM", "2:10 PM", "11:30 am")
        try {
            val upper = clean.uppercase(Locale.US)
            val isPm = upper.contains("PM")
            val isAm = upper.contains("AM")
            if (isPm || isAm) {
                val digitsOnly = upper.replace("AM", "").replace("PM", "").trim()
                val colonParts = digitsOnly.split(":")
                if (colonParts.size >= 2) {
                    var hour = colonParts[0].trim().toInt()
                    val minute = colonParts[1].trim().toInt()
                    if (isPm && hour < 12) hour += 12
                    if (isAm && hour == 12) hour = 0
                    return hour * 60 + minute
                }
            }
        } catch (_: Exception) {}

        // 2. Try LocalTime parsing for 24-hour format (e.g. "14:10", "09:00")
        try {
            val t = java.time.LocalTime.parse(clean)
            return t.hour * 60 + t.minute
        } catch (_: Exception) {}

        // 3. Simple fallback splitting by ":"
        try {
            val colonParts = clean.split(":")
            if (colonParts.size >= 2) {
                val hour = colonParts[0].filter { it.isDigit() }.toInt()
                val minute = colonParts[1].filter { it.isDigit() }.toInt()
                return hour * 60 + minute
            }
        } catch (_: Exception) {}

        return 0
    }

    /**
     * Accurately parses any time string into seconds from midnight (0..86399).
     */
    fun parseTimeToSeconds(timeStr: String): Int {
        return parseTimeToMinutes(timeStr) * 60
    }
}
