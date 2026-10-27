package com.shuaib.classmate.repositories

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Source
import com.shuaib.classmate.models.Period
import com.shuaib.classmate.models.RoutineSwapRequest
import com.shuaib.classmate.models.User
import com.shuaib.classmate.utils.NotificationSender
import com.shuaib.classmate.utils.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

class RoutineSwapRepository private constructor() {

    private val db = FirebaseFirestore.getInstance()
    private val swapCollection = db.collection("routine_swap_requests")

    /**
     * Sends a new class swap request to the target teacher.
     */
    fun sendSwapRequest(
        request: RoutineSwapRequest,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val docRef = if (request.id.isNotBlank()) swapCollection.document(request.id) else swapCollection.document()
        val data = hashMapOf(
            "id" to docRef.id,
            "date" to request.date,
            "day" to request.day.lowercase(),
            "department" to request.department,
            "batch" to request.batch,
            "requesterUid" to request.requesterUid,
            "requesterName" to request.requesterName,
            "requesterPeriodId" to request.requesterPeriodId,
            "requesterSubject" to request.requesterSubject,
            "requesterStartTime" to request.requesterStartTime,
            "requesterEndTime" to request.requesterEndTime,
            "requesterRoom" to request.requesterRoom,
            "targetUid" to request.targetUid,
            "targetTeacherName" to request.targetTeacherName,
            "targetPeriodId" to request.targetPeriodId,
            "targetSubject" to request.targetSubject,
            "targetStartTime" to request.targetStartTime,
            "targetEndTime" to request.targetEndTime,
            "targetRoom" to request.targetRoom,
            "note" to request.note,
            "status" to RoutineSwapRequest.STATUS_PENDING,
            "createdAt" to FieldValue.serverTimestamp()
        )

        docRef.set(data)
            .addOnSuccessListener {
                onComplete(true, null)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error sending swap request", e)
                onComplete(false, e.localizedMessage)
            }
    }

    /**
     * Observes pending swap requests targeted to the given teacher for a specific date.
     */
    fun observePendingRequestsForTeacher(
        teacherUid: String,
        date: String
    ): Flow<List<RoutineSwapRequest>> = callbackFlow {
        if (teacherUid.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener: ListenerRegistration = swapCollection
            .whereEqualTo("targetUid", teacherUid)
            .whereEqualTo("date", date)
            .whereEqualTo("status", RoutineSwapRequest.STATUS_PENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing pending requests", error)
                    return@addSnapshotListener
                }
                val requests = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(RoutineSwapRequest::class.java)
                }.orEmpty()
                trySend(requests)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Observes swap requests initiated by the given teacher for a specific date.
     */
    fun observeSentRequestsForTeacher(
        teacherUid: String,
        date: String
    ): Flow<List<RoutineSwapRequest>> = callbackFlow {
        if (teacherUid.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener: ListenerRegistration = swapCollection
            .whereEqualTo("requesterUid", teacherUid)
            .whereEqualTo("date", date)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing sent requests", error)
                    return@addSnapshotListener
                }
                val requests = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(RoutineSwapRequest::class.java)
                }.orEmpty()
                trySend(requests)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Accepts a swap request atomically via Firestore Batch Write:
     * - Period A receives Period B's start/end time (and room) for today.
     * - Period B receives Period A's start/end time (and room) for today.
     * - Swap request is marked as "accepted".
     * - Syncs local cache and triggers schedule update alerts.
     */
    fun acceptSwapRequest(
        request: RoutineSwapRequest,
        context: Context,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val normalizedDay = request.day.lowercase()
        val periodARef = db.collection("timetable").document(normalizedDay)
            .collection("periods").document(request.requesterPeriodId)
        val periodBRef = db.collection("timetable").document(normalizedDay)
            .collection("periods").document(request.targetPeriodId)
        val requestRef = swapCollection.document(request.id)

        val batch = db.batch()

        // Swap Period A to Period B's slot
        batch.update(
            periodARef,
            mapOf(
                "overrideDate" to request.date,
                "overrideStartTime" to request.targetStartTime,
                "overrideEndTime" to request.targetEndTime,
                "overrideRoom" to request.targetRoom.ifBlank { request.requesterRoom }
            )
        )

        // Swap Period B to Period A's slot
        batch.update(
            periodBRef,
            mapOf(
                "overrideDate" to request.date,
                "overrideStartTime" to request.requesterStartTime,
                "overrideEndTime" to request.requesterEndTime,
                "overrideRoom" to request.requesterRoom.ifBlank { request.targetRoom }
            )
        )

        // Mark swap request as accepted
        batch.update(
            requestRef,
            mapOf(
                "status" to RoutineSwapRequest.STATUS_ACCEPTED,
                "resolvedAt" to FieldValue.serverTimestamp()
            )
        )

        batch.commit()
            .addOnSuccessListener {
                val appContext = context.applicationContext
                CoroutineScope(Dispatchers.IO).launch {
                    runCatching {
                        TimetableRepository.getInstance(appContext)
                            .syncDayFromFirestore(normalizedDay, Source.SERVER)
                        WidgetUpdater.refresh(appContext, syncTodayTimetable = false)
                    }
                }

                // Broadcast Schedule Update Alerts
                NotificationSender.sendScheduleUpdateAlert(
                    subject = "${request.requesterSubject} ⇄ ${request.targetSubject}",
                    details = "Class times swapped for today: ${request.requesterSubject} at ${request.targetStartTime}, ${request.targetSubject} at ${request.requesterStartTime}",
                    day = normalizedDay,
                    targetBatch = request.batch.ifBlank { "all" }
                )

                onComplete(true, null)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error accepting swap request", e)
                onComplete(false, e.localizedMessage)
            }
    }

    /**
     * Rejects a swap request.
     */
    fun rejectSwapRequest(
        requestId: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        swapCollection.document(requestId)
            .update(
                mapOf(
                    "status" to RoutineSwapRequest.STATUS_REJECTED,
                    "resolvedAt" to FieldValue.serverTimestamp()
                )
            )
            .addOnSuccessListener {
                onComplete(true, null)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error rejecting swap request", e)
                onComplete(false, e.localizedMessage)
            }
    }

    /**
     * Binds/claims a routine period to the logged-in teacher in Firestore.
     */
    fun claimPeriodForTeacher(
        day: String,
        periodId: String,
        teacher: User,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val normalizedDay = day.lowercase()
        val displayName = teacher.name.ifBlank { teacher.fullName }

        db.collection("timetable").document(normalizedDay)
            .collection("periods").document(periodId)
            .update(
                mapOf(
                    "teacherUid" to teacher.uid,
                    "teacherEmail" to teacher.email,
                    "teacherCode" to teacher.teacherCode,
                    "teacher" to displayName
                )
            )
            .addOnSuccessListener {
                onComplete(true, null)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error claiming period for teacher", e)
                onComplete(false, e.localizedMessage)
            }
    }

    companion object {
        private const val TAG = "RoutineSwapRepository"
        @Volatile private var instance: RoutineSwapRepository? = null

        fun getInstance(): RoutineSwapRepository {
            return instance ?: synchronized(this) {
                instance ?: RoutineSwapRepository().also { instance = it }
            }
        }
    }
}
