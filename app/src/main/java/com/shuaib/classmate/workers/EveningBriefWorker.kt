package com.shuaib.classmate.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.shuaib.classmate.R
import com.shuaib.classmate.models.Assignment
import com.shuaib.classmate.models.Period
import com.shuaib.classmate.repositories.AcademicCalendarRepository
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.text.SimpleDateFormat
import java.util.*

class EveningBriefWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Replaced by the 12:00 AM midnight daily class update notification for students.
        return Result.success()
    }

    private fun showBriefNotification(periods: List<Period>) {
        val channelId = "evening_brief"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Evening Briefing", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val title = "Tomorrow's Schedule 📅"
        val content = "You have ${periods.size} classes tomorrow. Starts at ${periods.first().startTime}."

        val bigText = java.lang.StringBuilder().apply {
            append("Classes for tomorrow:\n")
            periods.forEach { append("- ${it.startTime}: ${it.subject}\n") }
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_classmate_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText.toString()))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(801, notification)
    }
}
