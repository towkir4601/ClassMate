package com.shuaib.classmate.repositories

import com.google.firebase.Timestamp
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
