package com.shuaib.classmate.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.shuaib.classmate.R
import com.shuaib.classmate.activities.MainActivity
import com.shuaib.classmate.models.Period
import com.shuaib.classmate.notices.NoticeUi
import com.shuaib.classmate.repositories.AcademicCalendarRepository
import com.shuaib.classmate.utils.DateHelper
import com.shuaib.classmate.utils.SubjectList
import kotlinx.coroutines.tasks.await
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class MorningBriefWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val firestore = FirebaseFirestore.getInstance()
        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid ?: return Result.success()

        try {
            val prefs = com.shuaib.classmate.utils.AppPreferences(applicationContext)
            if (!prefs.isNotificationsEnabled()) {
                return Result.success()
            }

            // Strictly exclude teachers from daily class update notifications
            var userRole = prefs.getUserRole()
            if (userRole.equals("teacher", ignoreCase = true)) {
                return Result.success()
            }

            val userDoc = runCatching {
                firestore.collection("users").document(uid).get().await()
            }.getOrNull()

            if (userDoc != null && userDoc.exists()) {
                val dbRole = userDoc.getString("role").orEmpty()
                if (dbRole.isNotBlank()) {
                    userRole = dbRole
                    prefs.setUserRole(dbRole)
                }
            }

            if (userRole.equals("teacher", ignoreCase = true)) {
                return Result.success()
            }

            val userSemester = userDoc?.getString("semester")?.takeIf { it.isNotBlank() }
                ?: prefs.getUserSemester()
            val userBatch = userDoc?.getString("batch").orEmpty().trim()

            // At 12:00 AM midnight (00:00..17:59), the current day is targeted.
            // In the evening (18:00..23:59), today's classes are over, so target is tomorrow.
            val nowTime = LocalTime.now()
            val nowDate = LocalDate.now()
            val targetLocalDate = DailyBriefSchedulePolicy.getTargetDate(nowDate, nowTime)
            val targetDateStr = targetLocalDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US))
            val targetDayName = when (targetLocalDate.dayOfWeek) {
                DayOfWeek.SATURDAY -> "saturday"
                DayOfWeek.SUNDAY -> "sunday"
                DayOfWeek.MONDAY -> "monday"
                DayOfWeek.TUESDAY -> "tuesday"
                DayOfWeek.WEDNESDAY -> "wednesday"
                DayOfWeek.THURSDAY -> "thursday"
                DayOfWeek.FRIDAY -> "friday"
                else -> "saturday"
            }

            val briefPrefs = applicationContext.getSharedPreferences("daily_brief_prefs", Context.MODE_PRIVATE)
            val dedupKey = "last_brief_date_$uid"
            if (briefPrefs.getString(dedupKey, "") == targetDateStr) {
                return Result.success()
            }

            val checkBatch = userSemester.ifBlank { userBatch }
            if (checkBatch.isNotBlank() &&
                AcademicCalendarRepository(firestore).isClassSuspendedForBatch(targetLocalDate, checkBatch)
            ) {
                briefPrefs.edit().putString(dedupKey, targetDateStr).apply()
                return Result.success()
            }

            // 1. Fetch classes for the target day
            val classesSnapshot = firestore.collection("timetable").document(targetDayName)
                .collection("periods")
                .orderBy("startTime")
                .get()
                .await()

            val rawPeriods = classesSnapshot.documents.mapNotNull { doc ->
                doc.toObject(Period::class.java)?.copy(id = doc.id)
            }

            // 2. Fetch active cancellation notices for the target date
            val cancelNoticesSnap = runCatching {
                firestore.collection("notices")
                    .whereEqualTo("isCancel", true)
                    .get()
                    .await()
            }.getOrNull()

            val cancelledSubjectsForDate = cancelNoticesSnap?.documents
                ?.map { NoticeUi.parseNotice(it) }
                ?.filter { !it.isDeleted && SubjectList.effectiveCancelDate(it) == targetDateStr }
                ?.mapNotNull { notice ->
                    notice.subject.takeIf { it.isNotBlank() && !it.equals("General", true) }
                        ?: notice.body.substringBefore(" class has been cancelled", "").takeIf { it.isNotBlank() }
                }
                .orEmpty()

            // Filter periods for this student's semester/batch and temporary date
            val studentPeriods = rawPeriods.filter { period ->
                val pBatch = period.batch.trim()
                val batchMatches = pBatch.isEmpty() ||
                    pBatch.equals("all", ignoreCase = true) ||
                    (userSemester.isNotBlank() && pBatch.equals(userSemester, ignoreCase = true)) ||
                    (userBatch.isNotBlank() && pBatch.equals(userBatch, ignoreCase = true))
                val tempMatches = !period.isTemporary ||
                    period.temporaryDate.isBlank() ||
                    period.temporaryDate == targetDateStr
                batchMatches && tempMatches
            }.map { period ->
                val isCancelledByNotice = cancelledSubjectsForDate.any { subj ->
                    SubjectList.isSameCourse(period.subject, subj)
                }
                val isCancelled = period.cancelDate == targetDateStr || isCancelledByNotice
                val isOverridden = period.overrideDate == targetDateStr
                val isSub = period.substituteDate == targetDateStr && period.substituteTeacher.isNotBlank()

                period.copy(
                    isCancelled = isCancelled,
                    isDynamicOverride = isOverridden,
                    startTime = if (isOverridden && period.overrideStartTime.isNotBlank()) period.overrideStartTime else period.startTime,
                    endTime = if (isOverridden && period.overrideEndTime.isNotBlank()) period.overrideEndTime else period.endTime,
                    room = if (isOverridden && period.overrideRoom.isNotBlank()) period.overrideRoom else period.room,
                    isSubstitute = isSub,
                    substituteTeacher = if (isSub) period.substituteTeacher else ""
                )
            }.sortedBy { DateHelper.parseTimeToMinutes(it.startTime) }

            if (studentPeriods.isEmpty()) {
                briefPrefs.edit().putString(dedupKey, targetDateStr).apply()
                return Result.success()
            }

            val activePeriods = studentPeriods.filterNot { it.isCancelled }

            // Stale Notification Protection:
            // WorkManager can be delayed by Android Doze mode, battery saver, or network retries,
            // firing hours late in the afternoon. If today's classes have already started or afternoon
            // has arrived, showing a "Starts at [time in past]" morning brief is obsolete and confusing.
            if (DailyBriefSchedulePolicy.isBriefStale(targetLocalDate, nowDate, nowTime, activePeriods)) {
                briefPrefs.edit().putString(dedupKey, targetDateStr).apply()
                return Result.success()
            }

            showMidnightClassUpdateNotification(targetDayName, targetLocalDate, studentPeriods)
            briefPrefs.edit().putString(dedupKey, targetDateStr).apply()

            return Result.success()
        } catch (e: Exception) {
            return if (runAttemptCount >= 3) Result.failure() else Result.retry()
        }
    }

    private fun showMidnightClassUpdateNotification(
        dayName: String,
        targetDate: LocalDate,
        periods: List<Period>
    ) {
        val channelId = "morning_brief"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Daily Class Updates",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val dayLabel = dayName.replaceFirstChar { it.uppercase(Locale.US) }
        val dateLabel = targetDate.format(DateTimeFormatter.ofPattern("MMM dd", Locale.US))
        val activePeriods = periods.filterNot { it.isCancelled }
        val cancelledCount = periods.count { it.isCancelled }

        val title = "📅 $dayLabel ($dateLabel) Class Updates"
        val content = when {
            activePeriods.isNotEmpty() && cancelledCount > 0 ->
                "${activePeriods.size} classes scheduled, $cancelledCount cancelled. Starts at ${activePeriods.first().startTime}."
            activePeriods.isNotEmpty() ->
                "You have ${activePeriods.size} classes for $dayLabel. Starts at ${activePeriods.first().startTime}."
            else ->
                "All $cancelledCount class(es) for $dayLabel are cancelled."
        }

        val bigText = StringBuilder().apply {
            periods.forEachIndexed { index, period ->
                if (index > 0) append("\n")
                val roomText = if (period.room.isNotBlank()) " · Room ${period.room}" else ""
                when {
                    period.isCancelled ->
                        append("❌ ${period.startTime}: ${period.subject} (CANCELLED)")
                    period.isDynamicOverride ->
                        append("🔄 ${period.startTime}-${period.endTime}: ${period.subject}$roomText (Updated)")
                    period.isSubstitute ->
                        append("🔄 ${period.startTime}-${period.endTime}: ${period.subject}$roomText (Sub: ${period.substituteTeacher})")
                    else ->
                        append("• ${period.startTime}-${period.endTime}: ${period.subject}$roomText")
                }
            }
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_TAB", "timetable")
            putExtra("OPEN_DAY", dayName)
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            800,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_classmate_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText.toString()))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(800, notification)
    }
}

object DailyBriefSchedulePolicy {
    fun getTargetDate(nowDate: LocalDate = LocalDate.now(), nowTime: LocalTime = LocalTime.now()): LocalDate {
        return if (nowTime.hour >= 18) {
            nowDate.plusDays(1)
        } else {
            nowDate
        }
    }

    fun isBriefStale(
        targetDate: LocalDate,
        nowDate: LocalDate = LocalDate.now(),
        nowTime: LocalTime = LocalTime.now(),
        activePeriods: List<Period>
    ): Boolean {
        if (targetDate != nowDate) {
            // Future target (e.g. tomorrow), not stale
            return false
        }
        val nowMinutes = nowTime.hour * 60 + nowTime.minute
        if (activePeriods.isNotEmpty()) {
            val firstStartMinutes = DateHelper.parseTimeToMinutes(activePeriods.first().startTime)
            if (nowMinutes >= firstStartMinutes || nowTime.hour >= 14) {
                return true
            }
        } else {
            if (nowTime.hour >= 12) {
                return true
            }
        }
        return false
    }
}
