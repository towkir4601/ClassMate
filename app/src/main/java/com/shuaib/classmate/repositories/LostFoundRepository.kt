package com.shuaib.classmate.repositories

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.shuaib.classmate.models.LostFoundItem

class LostFoundRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val collection = firestore.collection("lost_and_found")

    // Real production reports only; no hardcoded dummy items
    private val sampleItems = emptyList<LostFoundItem>()

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
        val fifteenDaysMs = 15L * 24 * 60 * 60 * 1000L
        val cutoffMs = nowMs - fifteenDaysMs

        collection.get()
            .addOnSuccessListener { snapshot ->
                val list = mutableListOf<LostFoundItem>()
                if (snapshot != null && !snapshot.isEmpty) {
                    for (doc in snapshot.documents) {
                        val item = doc.toObject(LostFoundItem::class.java)?.copy(id = doc.id) ?: continue
                        if (isDeletedLocally(item.id)) continue
                        val itemTime = item.timestamp?.toDate()?.time ?: 0L
                        if (itemTime in 1..cutoffMs) continue
                        list.add(item)
                    }
                }
                val sorted = list.sortedByDescending { it.timestamp?.toDate()?.time ?: 0L }
                val local = localItems.filter { !isDeletedLocally(it.id) }
                val merged = (local + sorted)
                    .filter { !isDeletedLocally(it.id) }
                    .distinctBy { it.id }
                onResult(merged.take(limit.toInt()))
            }
            .addOnFailureListener {
                val local = localItems.filter { !isDeletedLocally(it.id) }
                onResult(local.take(limit.toInt()))
            }
    }

    fun getAllActiveItems(onResult: (List<LostFoundItem>) -> Unit) {
        getApprovedItems(onResult)
    }

    /** Public view: Returns all unresolved, non-expired items (auto-approved, expires in 15 days) */
    fun getApprovedItems(onResult: (List<LostFoundItem>) -> Unit) {
        val nowMs = System.currentTimeMillis()
        val fifteenDaysMs = 15L * 24 * 60 * 60 * 1000L
        val cutoffMs = nowMs - fifteenDaysMs

        // Fetch without compound filter/order to eliminate missing composite index crashes
        collection.get()
            .addOnSuccessListener { snapshot ->
                val activeList = mutableListOf<LostFoundItem>()
                if (snapshot != null && !snapshot.isEmpty) {
                    val expiredDocs = mutableListOf<com.google.firebase.firestore.DocumentSnapshot>()
                    for (doc in snapshot.documents) {
                        val item = doc.toObject(LostFoundItem::class.java)?.copy(id = doc.id) ?: continue
                        if (isDeletedLocally(item.id)) continue
                        if (item.isResolved) continue
                        val itemTime = item.timestamp?.toDate()?.time ?: 0L
                        if (itemTime in 1..cutoffMs) {
                            expiredDocs.add(doc)
                        } else {
                            activeList.add(item)
                        }
                    }
                    // Auto delete expired items from Firestore (> 15 days old)
                    if (expiredDocs.isNotEmpty()) {
                        val batch = firestore.batch()
                        expiredDocs.forEach { batch.delete(it.reference) }
                        batch.commit()
                    }
                }
                val sortedActive = activeList.sortedByDescending { it.timestamp?.toDate()?.time ?: 0L }
                val localActive = localItems.filter { !it.isResolved && !isDeletedLocally(it.id) }
                val merged = (localActive + sortedActive)
                    .filter { !isDeletedLocally(it.id) && !it.isResolved }
                    .distinctBy { it.id }
                onResult(merged)
            }
            .addOnFailureListener {
                val localActive = localItems.filter { !it.isResolved && !isDeletedLocally(it.id) }
                onResult(localActive)
            }
    }

    /** Legacy / Admin moderation view: Returns any pending items if any */
    fun getPendingItems(onResult: (List<LostFoundItem>) -> Unit) {
        collection.get()
            .addOnSuccessListener { snapshot ->
                val pending = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(LostFoundItem::class.java)?.copy(id = doc.id)
                }?.filter { !isDeletedLocally(it.id) && !it.isResolved && it.pendingApproval }
                ?.sortedByDescending { it.timestamp?.toDate()?.time ?: 0L } ?: emptyList()
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
            approved = true,
            pendingApproval = false,
            shareToken = shareToken
        )
        // Store in local cache; visible immediately in public feed
        localItems.removeAll { it.id == toSave.id }
        localItems.add(0, toSave)

        docRef.set(toSave)
            .addOnSuccessListener { onSuccess(docRef.id) }
            .addOnFailureListener { onFailure(it) }
    }

    fun markResolved(
        itemId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        markDeletedLocally(itemId)
        synchronized(localItems) {
            val idx = localItems.indexOfFirst { it.id == itemId }
            if (idx >= 0) {
                localItems[idx] = localItems[idx].copy(isResolved = true)
            }
        }
        collection.document(itemId).update("isResolved", true)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun deleteItem(
        itemId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        markDeletedLocally(itemId)
        collection.document(itemId).delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
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
    fun markItemResolved(
        itemId: String,
        status: String,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        synchronized(localItems) {
            val idx = localItems.indexOfFirst { it.id == itemId }
            if (idx >= 0) {
                localItems[idx] = localItems[idx].copy(isResolved = true, resolvedStatus = status)
            }
        }
        val updates = mapOf(
            "isResolved" to true,
            "resolvedStatus" to status
        )
        collection.document(itemId).update(updates)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e ->
                android.util.Log.e("LostFoundRepo", "Failed to update resolved status: ${e.message}", e)
                onFailure(e)
            }
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

