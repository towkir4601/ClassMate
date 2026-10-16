/*
 * DailySchedulerWorker.kt
 * Runs once a day to fetch today's timetable and schedule individual notifications
 * for each class session.
 */
package com.shuaib.classmate.workers

import android.content.Context
import androidx.work.*
import com.google.firebase.firestore.FirebaseFirestore
import com.shuaib.classmate.models.Period
import com.shuaib.classmate.repositories.AcademicCalendarRepository
import com.shuaib.classmate.utils.ClassReminderWorkCoordinator
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class DailySchedulerWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        WorkManager.getInstance(applicationContext)
            .cancelAllWorkByTag(ClassReminderWorkCoordinator.CLASS_REMINDER_TAG)
        return Result.success()
    }

    private fun scheduleNotification(period: Period, dayName: String) {
        val classTime = parseTime(period.startTime) ?: return
        val now = Calendar.getInstance()
        
        // Target time is 10 minutes before class
        val targetTime = (classTime.clone() as Calendar).apply {
            add(Calendar.MINUTE, -10)
        }

        if (targetTime.after(now)) {
            val delay = targetTime.timeInMillis - now.timeInMillis
            
            val data = workDataOf(
                "id" to period.id,
                "day" to dayName,
                "subject" to period.subject,
                "teacher" to period.teacher
            )

            val notificationWork = OneTimeWorkRequestBuilder<PeriodNotificationWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .addTag(ClassReminderWorkCoordinator.CLASS_REMINDER_TAG)
                .addTag("period_${period.id}")
                .build()

            WorkManager.getInstance(applicationContext).enqueueUniqueWork(
                ClassReminderWorkCoordinator.uniqueReminderName(period.id),
                ExistingWorkPolicy.REPLACE,
                notificationWork
            )
        }
    }

    private fun parseTime(timeStr: String): Calendar? {
        return try {
            val parts = timeStr.split(":")
            val hour = parts[0].toInt()
            val minute = parts[1].toInt()
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
            }
        } catch (e: Exception) {
            null
        }
    }
}
