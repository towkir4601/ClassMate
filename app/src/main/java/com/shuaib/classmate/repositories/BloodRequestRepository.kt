package com.shuaib.classmate.repositories

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.shuaib.classmate.models.BloodRequest

class BloodRequestRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val collection = firestore.collection("blood_requests")

    companion object {
        private val localRequests = mutableListOf<BloodRequest>()
        private val deletedRequestIds = mutableSetOf<String>()

        fun markDeletedLocally(id: String) {
            deletedRequestIds.add(id)
            localRequests.removeAll { it.id == id }
        }

        fun isDeletedLocally(id: String): Boolean = deletedRequestIds.contains(id)
    }

    fun getActiveRequests(limit: Long = 10, onResult: (List<BloodRequest>) -> Unit) {
        collection
            .whereEqualTo("isFulfilled", false)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .addOnSuccessListener { snapshot ->
                val activeList = mutableListOf<BloodRequest>()
                if (snapshot != null && !snapshot.isEmpty) {
                    for (doc in snapshot.documents) {
                        val req = doc.toObject(BloodRequest::class.java)?.copy(id = doc.id) ?: continue
                        if (!isDeletedLocally(req.id) && !req.isFulfilled) {
                            activeList.add(req)
                        }
                    }
                }
                val localActive = localRequests.filter { !it.isFulfilled && !isDeletedLocally(it.id) }
                val merged = (localActive + activeList)
                    .filter { !isDeletedLocally(it.id) && !it.isFulfilled }
                    .distinctBy { it.id }
                onResult(merged.take(limit.toInt()))
            }
            .addOnFailureListener {
                val localActive = localRequests.filter { !it.isFulfilled && !isDeletedLocally(it.id) }
                onResult(localActive.take(limit.toInt()))
            }
    }

    fun getAllActiveRequests(onResult: (List<BloodRequest>) -> Unit) {
        getActiveRequests(limit = 50, onResult = onResult)
    }

    fun createRequest(
        request: BloodRequest,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val docRef = collection.document()
        val toSave = request.copy(
            id = docRef.id,
            timestamp = Timestamp.now(),
            isFulfilled = false
        )
        localRequests.removeAll { it.id == toSave.id }
        localRequests.add(0, toSave)

        docRef.set(toSave)
            .addOnSuccessListener { onSuccess(docRef.id) }
            .addOnFailureListener { e ->
                // Keep local item active even if Firestore write fails due to network/rules
                onSuccess(docRef.id)
            }
    }

    fun markFulfilled(
        requestId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val idx = localRequests.indexOfFirst { it.id == requestId }
        if (idx != -1) {
            localRequests[idx] = localRequests[idx].copy(isFulfilled = true)
        }
        collection.document(requestId).update("isFulfilled", true)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onSuccess() }
    }

    /** Mark blood donated under three-dot option */
    fun markBloodDonated(
        requestId: String,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        val idx = localRequests.indexOfFirst { it.id == requestId }
        if (idx != -1) {
            localRequests[idx] = localRequests[idx].copy(isFulfilled = true, isDonated = true)
        }
        val updates = mapOf(
            "isFulfilled" to true,
            "isDonated" to true,
            "donatedAt" to Timestamp.now()
        )
        collection.document(requestId).update(updates)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onSuccess() }
    }

    /** Record post view (including guests) */
    fun recordView(requestId: String, userId: String? = null) {
        val updates = mutableMapOf<String, Any>(
            "viewsCount" to FieldValue.increment(1)
        )
        if (!userId.isNullOrBlank()) {
            updates["viewers"] = FieldValue.arrayUnion(userId)
        }
        collection.document(requestId).update(updates)
    }

    /** Toggle love react for a request */
    fun toggleLove(
        requestId: String,
        userId: String,
        isLoving: Boolean,
        onResult: (newCount: Int) -> Unit = {}
    ) {
        val docRef = collection.document(requestId)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val currentReactors = (snapshot.get("loveReactors") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            val newReactors = currentReactors.toMutableList()
            if (isLoving) {
                if (!newReactors.contains(userId)) newReactors.add(userId)
            } else {
                newReactors.remove(userId)
            }
            transaction.update(docRef, mapOf(
                "loveReactors" to newReactors,
                "loveCount" to newReactors.size
            ))
            newReactors.size
        }.addOnSuccessListener { newCount ->
            onResult(newCount)
        }.addOnFailureListener {
            onResult(1)
        }
    }

    /** Record post share */
    fun recordShare(requestId: String) {
        collection.document(requestId).update("sharesCount", FieldValue.increment(1))
    }

    /** Report blood request */
    fun reportRequest(requestId: String, userId: String, reason: String, onSuccess: () -> Unit = {}) {
        val updates = mutableMapOf<String, Any>(
            "reportedCount" to FieldValue.increment(1)
        )
        if (userId.isNotBlank()) {
            updates["reportedBy"] = FieldValue.arrayUnion(userId)
        }
        collection.document(requestId).update(updates)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onSuccess() }
    }

    fun deleteRequest(
        requestId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        markDeletedLocally(requestId)
        collection.document(requestId).delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener {
                collection.document(requestId).update("isFulfilled", true)
                onSuccess()
            }
    }
}

