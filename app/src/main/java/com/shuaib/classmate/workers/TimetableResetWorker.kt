// com/shuaib/classmate/workers/TimetableResetWorker.kt
package com.shuaib.classmate.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class TimetableResetWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val db = FirebaseFirestore.getInstance()
        val todayDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        val days = listOf(
            "saturday", "sunday", "monday",
            "tuesday", "wednesday", "thursday", "friday"
        )
        runCatching {
            days.forEach { day ->
                val snapshot = db.collection("timetable")
                    .document(day)
                    .collection("periods")
                    .whereEqualTo("isCancelled", true)
                    .get()
                    .await()

                snapshot.documents.forEach { doc ->
                    val cancelDate = doc.getString("cancelDate").orEmpty()
                    if (cancelDate.isBlank() || cancelDate < todayDate) {
                        doc.reference.update(
                            mapOf(
                                "isCancelled" to false,
                                "cancelDate" to ""
                            )
                        ).await()
                    }
                }

                val subSnapshot = db.collection("timetable")
                    .document(day)
                    .collection("periods")
                    .whereEqualTo("isSubstitute", true)
                    .get()
                    .await()

                subSnapshot.documents.forEach { doc ->
                    val subDate = doc.getString("substituteDate").orEmpty()
                    if (subDate.isBlank() || subDate < todayDate) {
                        doc.reference.update(
                            mapOf(
                                "isSubstitute" to false,
                                "substituteTeacher" to "",
                                "substituteDate" to ""
                            )
                        ).await()
                    }
                }
            }
        }
        return Result.success()
    }
}
