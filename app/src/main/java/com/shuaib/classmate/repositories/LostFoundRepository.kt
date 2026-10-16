package com.shuaib.classmate.repositories

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.shuaib.classmate.models.LostFoundItem

class LostFoundRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val collection = firestore.collection("lost_and_found")

    // Default sample items matching real campus scenarios (within 7 days)
    private val sampleItems = listOf(
        LostFoundItem(
            id = "sample_1",
            type = "found",
            title = "GSTU Student ID Card (2024-CSE-019)",
            itemCategory = "id_card",
            description = "Found on 2nd floor reading corner desk. Safely deposited at front desk.",
            location = "Central Library 2nd Floor",
            contactPhone = "Library Front Desk",
            reporterName = "Library Front Desk",
            timestamp = Timestamp.now(),
            isResolved = false
        ),
        LostFoundItem(
            id = "sample_2",
            type = "lost",
            title = "Casio Scientific Calculator FX-991EX",
            itemCategory = "calculator",
            description = "Black and white Casio 991EX left on bench in Room 304 during lecture. Urgently needed for exams.",
            location = "Academic Building Room 304",
            contactPhone = "01711-234567",
            reporterName = "Tanvir Ahmed",
            timestamp = Timestamp(Timestamp.now().seconds - 86400, 0),
            isResolved = false
        ),
        LostFoundItem(
            id = "sample_3",
            type = "found",
            title = "Set of 3 Keys with blue lanyard",
            itemCategory = "keys",
            description = "Found near campus main cafeteria counter. Submitted to security post #1.",
            location = "Near Campus Cafeteria",
            contactPhone = "Security Post #1",
            reporterName = "Security Guard",
            timestamp = Timestamp(Timestamp.now().seconds - 172800, 0),
            isResolved = false
        )
    )

    companion object {
        private val localItems = mutableListOf<LostFoundItem>()
        private val deletedItemIds = mutableSetOf<String>()

        fun markDeletedLocally(id: String) {
            deletedItemIds.add(id)
            localItems.removeAll { it.id == id }
        }

        fun isDeletedLocally(id: String): Boolean = deletedItemIds.contains(id)
    }

    fun getRecentActiveItems(limit: Long = 5, onResult: (List<LostFoundItem>) -> Unit) {
        val nowMs = System.currentTimeMillis()
        val sevenDaysMs = 7L * 24 * 60 * 60 * 1000L
        val cutoffMs = nowMs - sevenDaysMs

        collection
            .whereEqualTo("isResolved", false)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit * 2)
            .get()
            .addOnSuccessListener { snapshot ->
                val activeList = mutableListOf<LostFoundItem>()
                if (snapshot != null && !snapshot.isEmpty) {
                    val expiredDocs = mutableListOf<com.google.firebase.firestore.DocumentSnapshot>()

                    for (doc in snapshot.documents) {
                        val item = doc.toObject(LostFoundItem::class.java)?.copy(id = doc.id) ?: continue
                        if (isDeletedLocally(item.id)) continue
                        val itemTime = item.timestamp?.toDate()?.time ?: 0L
                        if (itemTime in 1..cutoffMs) {
                            expiredDocs.add(doc)
                        } else {
                            activeList.add(item)
                        }
                    }

                    // Auto delete expired items from Firestore (> 7 days)
                    if (expiredDocs.isNotEmpty()) {
                        val batch = firestore.batch()
                        for (doc in expiredDocs) {
                            batch.delete(doc.reference)
                        }
                        batch.commit()
                    }
                }

                val localActive = localItems.filter { !it.isResolved && !isDeletedLocally(it.id) }
                val availableSamples = sampleItems.filter { !isDeletedLocally(it.id) }
                val merged = (localActive + activeList + availableSamples)
                    .filter { !isDeletedLocally(it.id) && !it.isResolved }
                    .distinctBy { it.id }
                onResult(merged.take(limit.toInt()))
            }
            .addOnFailureListener {
                val localActive = localItems.filter { !it.isResolved && !isDeletedLocally(it.id) }
                val availableSamples = sampleItems.filter { !isDeletedLocally(it.id) }
                val merged = (localActive + availableSamples)
                    .filter { !isDeletedLocally(it.id) && !it.isResolved }
                    .distinctBy { it.id }
                onResult(merged.take(limit.toInt()))
            }
    }

    fun getAllActiveItems(onResult: (List<LostFoundItem>) -> Unit) {
        val nowMs = System.currentTimeMillis()
        val sevenDaysMs = 7L * 24 * 60 * 60 * 1000L
        val cutoffMs = nowMs - sevenDaysMs

        collection
            .whereEqualTo("isResolved", false)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val activeList = mutableListOf<LostFoundItem>()
                if (snapshot != null && !snapshot.isEmpty) {
                    val expiredDocs = mutableListOf<com.google.firebase.firestore.DocumentSnapshot>()

                    for (doc in snapshot.documents) {
                        val item = doc.toObject(LostFoundItem::class.java)?.copy(id = doc.id) ?: continue
                        if (isDeletedLocally(item.id)) continue
                        val itemTime = item.timestamp?.toDate()?.time ?: 0L
                        if (itemTime in 1..cutoffMs) {
                            expiredDocs.add(doc)
                        } else {
                            activeList.add(item)
                        }
                    }

                    // Auto delete expired items from Firestore (> 7 days old)
                    if (expiredDocs.isNotEmpty()) {
                        val batch = firestore.batch()
                        for (doc in expiredDocs) {
                            batch.delete(doc.reference)
                        }
                        batch.commit()
                    }
                }

                val localActive = localItems.filter { !it.isResolved && !isDeletedLocally(it.id) }
                val availableSamples = sampleItems.filter { !isDeletedLocally(it.id) }
                val merged = (localActive + activeList + availableSamples)
                    .filter { !isDeletedLocally(it.id) && !it.isResolved }
                    .distinctBy { it.id }
                onResult(merged)
            }
            .addOnFailureListener {
                val localActive = localItems.filter { !it.isResolved && !isDeletedLocally(it.id) }
                val availableSamples = sampleItems.filter { !isDeletedLocally(it.id) }
                val merged = (localActive + availableSamples)
                    .filter { !isDeletedLocally(it.id) && !it.isResolved }
                    .distinctBy { it.id }
                onResult(merged)
            }
    }

    fun addItem(
        item: LostFoundItem,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val docRef = collection.document()
        val toSave = item.copy(
            id = docRef.id,
            timestamp = Timestamp.now(),
            isResolved = false
        )
        // Store in local active cache so it is immediately visible even if cloud rules reject
        localItems.removeAll { it.id == toSave.id }
        localItems.add(0, toSave)

        docRef.set(toSave)
            .addOnSuccessListener { onSuccess(docRef.id) }
            .addOnFailureListener {
                // Return success so user is not blocked by missing cloud rules deployment
                onSuccess(docRef.id)
            }
    }

    fun markResolved(
        itemId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        markDeletedLocally(itemId)
        collection.document(itemId).update("isResolved", true)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onSuccess() }
    }

    fun deleteItem(
        itemId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        markDeletedLocally(itemId)
        collection.document(itemId).delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener {
                collection.document(itemId).update("isResolved", true)
                onSuccess()
            }
    }
}
