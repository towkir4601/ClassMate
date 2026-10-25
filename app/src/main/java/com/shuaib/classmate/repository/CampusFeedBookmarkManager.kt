package com.shuaib.classmate.repository

import android.content.Context
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

object CampusFeedBookmarkManager {
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private const val PREFS_NAME = "campus_feed_bookmarks_prefs"

    private fun getPrefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isBookmarked(userId: String, postId: String, context: Context): Boolean {
        if (userId.isBlank() || postId.isBlank()) return false
        val set = getPrefs(context).getStringSet("bookmarks_$userId", emptySet()) ?: emptySet()
        return set.contains(postId)
    }

    fun toggleBookmark(
        userId: String,
        postId: String,
        context: Context,
        onComplete: ((Boolean) -> Unit)? = null
    ): Boolean {
        val currentlyBookmarked = isBookmarked(userId, postId, context)
        val targetState = !currentlyBookmarked
        setBookmarked(userId, postId, targetState, context, onComplete)
        return targetState
    }

    fun setBookmarked(
        userId: String,
        postId: String,
        bookmarked: Boolean,
        context: Context,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        if (userId.isBlank() || postId.isBlank()) {
            onComplete?.invoke(false)
            return
        }

        // 1. Instant local persistence in SharedPreferences
        val prefs = getPrefs(context)
        val currentSet = prefs.getStringSet("bookmarks_$userId", emptySet())?.toMutableSet() ?: mutableSetOf()
        if (bookmarked) {
            currentSet.add(postId)
        } else {
            currentSet.remove(postId)
        }
        prefs.edit().putStringSet("bookmarks_$userId", currentSet).apply()

        // 2. Cloud Firestore sync
        val doc = db.collection("campus_feed_bookmarks").document("${userId}_$postId")
        if (bookmarked) {
            doc.set(
                mapOf(
                    "userId" to userId,
                    "postId" to postId,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).addOnSuccessListener { onComplete?.invoke(true) }
                .addOnFailureListener { onComplete?.invoke(false) }
        } else {
            doc.delete()
                .addOnSuccessListener { onComplete?.invoke(true) }
                .addOnFailureListener { onComplete?.invoke(false) }
        }
    }

    fun fetchCloudBookmarks(userId: String, context: Context, onResult: ((Set<String>) -> Unit)? = null) {
        if (userId.isBlank()) {
            onResult?.invoke(emptySet())
            return
        }
        db.collection("campus_feed_bookmarks")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                val ids = snapshot.documents.mapNotNull { it.getString("postId") }.toSet()
                getPrefs(context).edit().putStringSet("bookmarks_$userId", ids).apply()
                onResult?.invoke(ids)
            }
            .addOnFailureListener {
                val cached = getPrefs(context).getStringSet("bookmarks_$userId", emptySet()) ?: emptySet()
                onResult?.invoke(cached)
            }
    }
}
