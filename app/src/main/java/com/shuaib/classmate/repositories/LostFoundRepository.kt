package com.shuaib.classmate.repositories

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
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
        // Legacy call - routes to getApprovedItems for backward compat
        getApprovedItems(onResult)
    }

    /** Public view: Returns only admin-approved, unresolved, non-expired items */
    fun getApprovedItems(onResult: (List<LostFoundItem>) -> Unit) {
        val nowMs = System.currentTimeMillis()
        val sevenDaysMs = 7L * 24 * 60 * 60 * 1000L
        val cutoffMs = nowMs - sevenDaysMs

        collection
            .whereEqualTo("isResolved", false)
            .whereEqualTo("approved", true)
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
                    if (expiredDocs.isNotEmpty()) {
                        val batch = firestore.batch()
                        expiredDocs.forEach { batch.delete(it.reference) }
                        batch.commit()
                    }
                }
                // Local items: show only approved ones (or ones just created by admin)
                val localActive = localItems.filter { it.approved && !it.isResolved && !isDeletedLocally(it.id) }
                val availableSamples = sampleItems.filter { !isDeletedLocally(it.id) }
                val merged = (localActive + activeList + availableSamples)
                    .filter { !isDeletedLocally(it.id) && !it.isResolved }
                    .distinctBy { it.id }
                onResult(merged)
            }
            .addOnFailureListener {
                val localActive = localItems.filter { it.approved && !it.isResolved && !isDeletedLocally(it.id) }
                val availableSamples = sampleItems.filter { !isDeletedLocally(it.id) }
                val merged = (localActive + availableSamples)
                    .filter { !isDeletedLocally(it.id) && !it.isResolved }
                    .distinctBy { it.id }
                onResult(merged)
            }
    }

    /** Admin moderation view: Returns only PENDING (not yet approved) items */
    fun getPendingItems(onResult: (List<LostFoundItem>) -> Unit) {
        collection
            .whereEqualTo("isResolved", false)
            .whereEqualTo("pendingApproval", true)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val pending = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(LostFoundItem::class.java)?.copy(id = doc.id)
                }?.filter { !isDeletedLocally(it.id) } ?: emptyList()
                onResult(pending)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }

    fun addItem(
        item: LostFoundItem,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val docRef = collection.document()
        // Generate a short unique share token for deep linking
        val shareToken = docRef.id.take(8).uppercase()
        val toSave = item.copy(
            id = docRef.id,
            timestamp = Timestamp.now(),
            isResolved = false,
            approved = false,
            pendingApproval = true,
            shareToken = shareToken
        )
        // Store in local cache; do NOT show in public feed until approved
        localItems.removeAll { it.id == toSave.id }

        docRef.set(toSave)
            .addOnSuccessListener { onSuccess(docRef.id) }
            .addOnFailureListener { onSuccess(docRef.id) }
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

    /** Record a post view (from guest or logged in user) */
    fun recordView(itemId: String, userId: String? = null) {
        val updates = mutableMapOf<String, Any>(
            "viewsCount" to FieldValue.increment(1)
        )
        if (!userId.isNullOrBlank()) {
            updates["viewers"] = FieldValue.arrayUnion(userId)
        }
        collection.document(itemId).update(updates)
    }

    /** Toggle love react for a logged in user */
    fun toggleLove(
        itemId: String,
        userId: String,
        isLoving: Boolean,
        onResult: (newCount: Int) -> Unit = {}
    ) {
        val docRef = collection.document(itemId)
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
            // Local fallback
            onResult(1)
        }
    }

    /** Record a post share */
    fun recordShare(itemId: String) {
        collection.document(itemId).update("sharesCount", FieldValue.increment(1))
    }

    /** Mark item resolved with specific status: "item_given" or "item_found" */
    fun markItemResolved(itemId: String, status: String, onSuccess: () -> Unit = {}) {
        val updates = mapOf(
            "isResolved" to true,
            "resolvedStatus" to status
        )
        collection.document(itemId).update(updates)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onSuccess() }
    }

    /** Report post for spam/inappropriate content */
    fun reportItem(itemId: String, userId: String, reason: String, onSuccess: () -> Unit = {}) {
        val updates = mutableMapOf<String, Any>(
            "reportedCount" to FieldValue.increment(1)
        )
        if (userId.isNotBlank()) {
            updates["reportedBy"] = FieldValue.arrayUnion(userId)
        }
        collection.document(itemId).update(updates)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onSuccess() }
    }
}

