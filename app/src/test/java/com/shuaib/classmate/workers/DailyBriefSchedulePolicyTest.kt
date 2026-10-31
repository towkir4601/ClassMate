package com.shuaib.classmate.workers

import com.shuaib.classmate.models.Period
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class DailyBriefSchedulePolicyTest {

    private val today = LocalDate.of(2026, 10, 27) // Tuesday
    private val tomorrow = today.plusDays(1)

    @Test
    fun testTargetDateAtMidnight() {
        val midnight = LocalTime.of(0, 0)
        assertEquals(today, DailyBriefSchedulePolicy.getTargetDate(today, midnight))
    }

    @Test
    fun testTargetDateInMorning() {
        val morning = LocalTime.of(8, 30)
        assertEquals(today, DailyBriefSchedulePolicy.getTargetDate(today, morning))
    }

    @Test
    fun testTargetDateInEveningTargetsTomorrow() {
        val evening = LocalTime.of(18, 0)
        assertEquals(tomorrow, DailyBriefSchedulePolicy.getTargetDate(today, evening))

        val lateEvening = LocalTime.of(21, 30)
        assertEquals(tomorrow, DailyBriefSchedulePolicy.getTargetDate(today, lateEvening))
    }

    @Test
    fun testBriefStaleWhenCurrentTimeIsPastFirstClassStartTime() {
        // Class starts at 02:10 PM (14:10). Current time is 4:30 PM (16:30).
        val nowTime = LocalTime.of(16, 30)
        val periods = listOf(
            Period(id = "1", startTime = "02:10 PM", endTime = "03:00 PM", subject = "Algorithms"),
            Period(id = "2", startTime = "03:10 PM", endTime = "04:00 PM", subject = "Database Systems")
        )

        val isStale = DailyBriefSchedulePolicy.isBriefStale(
            targetDate = today,
            nowDate = today,
            nowTime = nowTime,
            activePeriods = periods
        )
        assertTrue("Notification at 4:30 PM for a 2:10 PM class MUST be marked stale", isStale)
    }

    @Test
    fun testBriefNotStaleWhenScheduledBeforeClassStartsInMorning() {
        // Class starts at 09:00 AM. Current time is 00:00 (Midnight) or 08:00 AM.
        val midnight = LocalTime.of(0, 0)
        val morning = LocalTime.of(8, 0)
        val periods = listOf(
            Period(id = "1", startTime = "09:00 AM", endTime = "10:00 AM", subject = "Compiler Design")
        )

        assertFalse(
            "Midnight briefing for morning class is not stale",
            DailyBriefSchedulePolicy.isBriefStale(today, today, midnight, periods)
        )
        assertFalse(
            "8 AM briefing for 9 AM class is not stale",
            DailyBriefSchedulePolicy.isBriefStale(today, today, morning, periods)
        )
    }

    @Test
    fun testBriefStaleInAfternoonEvenIfClassIsLater() {
        // Daily morning briefings after 2:00 PM should be treated as stale
        val lateAfternoon = LocalTime.of(14, 15)
        val periods = listOf(
            Period(id = "1", startTime = "05:00 PM", endTime = "06:00 PM", subject = "Elective")
        )

        assertTrue(
            "Daily morning briefing at 2:15 PM is stale",
            DailyBriefSchedulePolicy.isBriefStale(today, today, lateAfternoon, periods)
        )
    }

    @Test
    fun testCancelledClassesStaleAfterMidday() {
        val emptyActivePeriods = emptyList<Period>() // All cancelled

        val morningTime = LocalTime.of(8, 0)
        val afternoonTime = LocalTime.of(16, 30)

        assertFalse(
            "Morning cancellation notice is not stale",
            DailyBriefSchedulePolicy.isBriefStale(today, today, morningTime, emptyActivePeriods)
        )
        assertTrue(
            "Late afternoon cancellation notice is stale",
            DailyBriefSchedulePolicy.isBriefStale(today, today, afternoonTime, emptyActivePeriods)
        )
    }

    @Test
    fun testBriefForTomorrowIsNotStale() {
        // Evening run targeting tomorrow
        val eveningTime = LocalTime.of(19, 0)
        val periods = listOf(
            Period(id = "1", startTime = "09:00 AM", endTime = "10:00 AM", subject = "Math")
        )

        assertFalse(
            "Brief targeting tomorrow is not stale",
            DailyBriefSchedulePolicy.isBriefStale(tomorrow, today, eveningTime, periods)
        )
    }
}
