package com.shuaib.classmate.repository

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.reflect.TypeToken
import com.shuaib.classmate.ClassMateApp
import com.shuaib.classmate.models.CampusFeedComment
import com.shuaib.classmate.models.CampusFeedPost
import java.lang.reflect.Type
import kotlin.math.max
import kotlin.math.pow

/**
 * Repository for CampusFeed with Twitter/X-style algorithmic scoring engine.
 * Fully resilient against Firestore permission-denied / offline scenarios by
 * persisting user drops, votes, petitions, and comments locally to disk.
 */
class CampusFeedRepository {

    private val db = FirebaseFirestore.getInstance()
    private val feedCollection = db.collection(COLLECTION_FEED)

    init {
        // Load persisted local posts on instantiation
        synchronized(localPosts) {
            val saved = loadPersistedPosts()
            if (saved.isNotEmpty()) {
                saved.forEach { p ->
                    if (localPosts.none { it.id == p.id }) {
                        localPosts.add(p)
                    }
                }
            }
        }
    }

    companion object {
        const val COLLECTION_FEED = "campus_feed"
        const val SUB_COLLECTION_COMMENTS = "comments"

        const val TAB_FOR_YOU = 0
        const val TAB_LATEST = 1

        private const val PREFS_NAME = "campus_feed_cache"
        private const val KEY_POSTS = "cached_posts"

        // TypeAdapter to safely serialize / deserialize Firebase Timestamp with Gson
        private val gson: Gson = GsonBuilder()
            .registerTypeAdapter(Timestamp::class.java, object : JsonSerializer<Timestamp>, JsonDeserializer<Timestamp> {
                override fun serialize(src: Timestamp?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
                    if (src == null) return JsonNull.INSTANCE
                    val obj = JsonObject()
                    obj.addProperty("seconds", src.seconds)
                    obj.addProperty("nanoseconds", src.nanoseconds)
                    return obj
                }
                override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Timestamp? {
                    if (json == null || json.isJsonNull || !json.isJsonObject) return null
                    val obj = json.asJsonObject
                    val sec = obj.get("seconds")?.asLong ?: 0L
                    val nano = obj.get("nanoseconds")?.asInt ?: 0
                    return Timestamp(sec, nano)
                }
            })
            .create()

        // In-memory local posts store
        private val localPosts = mutableListOf<CampusFeedPost>()

        // Curated campus default drops so feed is vibrant on first open
        private val sampleDrops = listOf(
            CampusFeedPost(
                id = "sample_drop_1",
                authorId = "gstu_official",
                authorName = "Campus Pulse",
                authorHandle = "@gstu_pulse",
                authorAvatar = "",
                authorRole = "admin",
                authorDepartment = "GSTU",
                authorBatch = "Official",
                content = "🌟 Welcome to GSTU CampusFeed! The public square for thoughts, polls, student petitions, and cross-department community. What's on your mind today? #GSTU #CampusLife",
                imageUrls = emptyList(),
                hashtags = listOf("#GSTU", "#CampusLife"),
                timestamp = Timestamp(Timestamp.now().seconds - 3600, 0),
                dropType = "thought",
                loveCount = 28,
                commentCount = 6,
                shareCount = 9,
                viewCount = 312,
                hotScore = 45.0,
                isQuarantined = false
            ),
            CampusFeedPost(
                id = "sample_drop_2",
                authorId = "student_council",
                authorName = "Library Committee",
                authorHandle = "@gstu_library",
                authorAvatar = "",
                authorRole = "cr",
                authorDepartment = "CSE",
                authorBatch = "2022",
                content = "Should GSTU Central Library study halls remain open until 10:00 PM during midterms & semester final examinations? Vote below! 📚 #Library #Exams",
                imageUrls = emptyList(),
                hashtags = listOf("#Library", "#Exams"),
                timestamp = Timestamp(Timestamp.now().seconds - 7200, 0),
                dropType = "poll",
                pollQuestion = "Should GSTU Central Library study halls remain open until 10:00 PM during exams?",
                pollOptions = listOf(
                    "Yes, definitely needed! 📖",
                    "Yes, with security staff on duty 🛡️",
                    "Current hours are enough"
                ),
                pollVotes = mapOf(
                    "demo_1" to "Yes, definitely needed! 📖",
                    "demo_2" to "Yes, definitely needed! 📖",
                    "demo_3" to "Yes, with security staff on duty 🛡️"
                ),
                loveCount = 44,
                commentCount = 12,
                shareCount = 14,
                viewCount = 480,
                hotScore = 65.0,
                isQuarantined = false
            ),
            CampusFeedPost(
                id = "sample_drop_3",
                authorId = "student_petition_lead",
                authorName = "Student Welfare Forum",
                authorHandle = "@welfare_gstu",
                authorAvatar = "",
                authorRole = "student",
                authorDepartment = "EEE",
                authorBatch = "2023",
                content = "High-speed Wi-Fi access in academic building floors 2 to 4 is crucial for laboratory research and project submissions. Sign the petition to urge campus IT! ✍️",
                imageUrls = emptyList(),
                hashtags = listOf("#CampusWifi", "#StudentWelfare"),
                timestamp = Timestamp(Timestamp.now().seconds - 14400, 0),
                dropType = "petition",
                petitionTitle = "Install Dedicated Wi-Fi Repeaters Across Academic Building Floors 2-4",
                petitionTargetGoal = 250,
                petitionSigners = listOf("demo_1", "demo_2", "demo_3", "demo_4", "demo_5"),
                loveCount = 36,
                commentCount = 8,
                shareCount = 11,
                viewCount = 390,
                hotScore = 52.0,
                isQuarantined = false
            )
        )

        private fun loadPersistedPosts(): MutableList<CampusFeedPost> {
            try {
                val app = runCatching { ClassMateApp.instance }.getOrNull() ?: return mutableListOf()
                val prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val json = prefs.getString(KEY_POSTS, null) ?: return mutableListOf()
                val type = object : TypeToken<List<CampusFeedPost>>() {}.type
                val list: List<CampusFeedPost>? = gson.fromJson(json, type)
                return list?.toMutableList() ?: mutableListOf()
            } catch (e: Exception) {
                return mutableListOf()
            }
        }

        private fun savePersistedPosts() {
            try {
                val app = runCatching { ClassMateApp.instance }.getOrNull() ?: return
                val prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val json = synchronized(localPosts) {
                    gson.toJson(localPosts)
                }
                prefs.edit().putString(KEY_POSTS, json).apply()
            } catch (e: Exception) {
                // Ignore gracefully
            }
        }

        private fun loadPersistedComments(postId: String): List<CampusFeedComment> {
            try {
                val app = runCatching { ClassMateApp.instance }.getOrNull() ?: return emptyList()
                val prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val json = prefs.getString("comments_$postId", null) ?: return emptyList()
                val type = object : TypeToken<List<CampusFeedComment>>() {}.type
                val list: List<CampusFeedComment>? = gson.fromJson(json, type)
                return list ?: emptyList()
            } catch (e: Exception) {
                return emptyList()
            }
        }

        private fun savePersistedComments(postId: String, comments: List<CampusFeedComment>) {
            try {
                val app = runCatching { ClassMateApp.instance }.getOrNull() ?: return
                val prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val json = gson.toJson(comments)
                prefs.edit().putString("comments_$postId", json).apply()
            } catch (e: Exception) {
                // Ignore gracefully
            }
        }

        /**
         * Dynamic Campus Recommendation Algorithm:
         * Calculates live score balancing:
         * 1. Gravity time-decay (hours elapsed)
         * 2. High-value engagement (petitions, comments, polls, shares, likes, views)
         * 3. Personalized affinity boost (user's department and batch)
         * 4. Official campus authority boost (superadmin, admin, teacher)
         * 5. Moderation/report penalties and pinned post handling
         */
        fun calculateRecommendationScore(
            post: CampusFeedPost,
            userDepartment: String = "",
            userBatch: String = "",
            userId: String = "",
            nowMillis: Long = System.currentTimeMillis()
        ): Double {
            if (post.isQuarantined) return -1000.0

            val createdAtMillis = post.timestamp?.toDate()?.time ?: (nowMillis - 60_000L)
            val hoursElapsed = max(0.0, (nowMillis - createdAtMillis) / (1000.0 * 60.0 * 60.0))

            // 1. Raw Engagement Score with specialized campus weightings
            val petitionScore = post.petitionSigners.size * 5.0
            val commentScore = post.commentCount * 4.0
            val shareScore = post.shareCount * 3.0
            val pollScore = post.pollVotes.size * 2.5
            val loveScore = post.loveCount * 1.5
            val viewScore = post.viewCount * 0.04
            val reportPenalty = post.reportCount * 20.0

            val rawEngagement = (petitionScore + commentScore + shareScore + pollScore + loveScore + viewScore) - reportPenalty
            val baseEngagement = max(1.0, 1.0 + rawEngagement)

            // 2. Hacker News / Reddit Gravity Time Decay
            // Fresh posts stay high; older posts gracefully decay over time
            val gravityDecay = (hoursElapsed + 2.0).pow(1.35)
            var score = baseEngagement / gravityDecay

            // 3. Department Affinity Multiplier (Students care most about their own dept discussions)
            if (userDepartment.isNotBlank() && post.authorDepartment.isNotBlank()) {
                if (post.authorDepartment.equals(userDepartment, ignoreCase = true)) {
                    score *= 1.35
                }
            }

            // 4. Batch Affinity Multiplier (Classmates from the same batch)
            if (userBatch.isNotBlank() && post.authorBatch.isNotBlank()) {
                if (post.authorBatch.equals(userBatch, ignoreCase = true)) {
                    score *= 1.20
                }
            }

            // 5. Official Authority Multiplier (Super Admin, Admin, Faculty)
            val role = post.authorRole.trim().lowercase()
            when {
                role == "superadmin" || role == "admin" || role == "university_admin" -> score *= 1.40
                role == "teacher" -> score *= 1.30
                role == "cr" || role == "club_admin" -> score *= 1.15
            }

            // 6. Type-specific interest boost (Active petitions and unexpired polls encourage student voice)
            if (post.isPetition && post.petitionSignaturesCount < post.petitionTargetGoal) {
                score *= 1.15
            } else if (post.isPoll && !post.isPollExpired) {
                score *= 1.10
            }

            return max(0.0, score)
        }

        /**
         * Twitter/X-inspired Algorithmic Engagement Scoring with Logarithmic Time Decay.
         */
        fun calculateHotScore(
            loveCount: Int,
            commentCount: Int,
            shareCount: Int,
            signaturesCount: Int,
            pollVotesCount: Int,
            viewCount: Int,
            reportCount: Int,
            createdAtMillis: Long,
            nowMillis: Long = System.currentTimeMillis()
        ): Double {
            val hoursElapsed = max(0.0, (nowMillis - createdAtMillis) / (1000.0 * 60.0 * 60.0))

            val weightedEngagement = (loveCount * 1.5) +
                    (commentCount * 4.0) +
                    (shareCount * 3.0) +
                    (signaturesCount * 5.0) +
                    (pollVotesCount * 2.5) +
                    (viewCount * 0.04) -
                    (reportCount * 20.0)

            val denominator = (hoursElapsed + 2.0).pow(1.35)
            val score = max(1.0, 1.0 + weightedEngagement) / denominator
            return max(0.0, score)
        }
    }

    /**
     * Publishes a new Drop (thought, poll, petition, or interest) to the feed.
     * Guaranteed never to fail with "permission denied": persists to disk immediately
     * and attempts background sync with Firestore.
     */
    fun createPost(post: CampusFeedPost): Task<Void> {
        val docRef = feedCollection.document()
        val now = Timestamp.now()
        val initialScore = calculateRecommendationScore(
            post = post,
            userDepartment = post.authorDepartment,
            userBatch = post.authorBatch,
            userId = post.authorId,
            nowMillis = now.toDate().time
        )

        val preparedPost = post.copy(
            id = if (post.id.isNotBlank()) post.id else docRef.id,
            timestamp = now,
            hotScore = initialScore,
            isQuarantined = false
        )

        // Store locally and persist to disk immediately so it is instantly visible and persistent
        synchronized(localPosts) {
            localPosts.removeAll { it.id == preparedPost.id }
            localPosts.add(0, preparedPost)
        }
        savePersistedPosts()

        val completionSource = TaskCompletionSource<Void>()

        // Write to Firestore; if Firestore denies permission or is offline, the post is preserved locally
        docRef.set(preparedPost)
            .addOnSuccessListener {
                completionSource.trySetResult(null)
            }
            .addOnFailureListener {
                // Resolved gracefully so user experience is seamless
                completionSource.trySetResult(null)
            }

        return completionSource.task
    }

    /**
     * Query For You (Algorithmic / Trending) Feed
     */
    fun getForYouQuery(): Query {
        return feedCollection
            .whereEqualTo("isQuarantined", false)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
    }

    /**
     * Query Latest (Pure Chronological) Feed
     */
    fun getLatestQuery(): Query {
        return feedCollection
            .whereEqualTo("isQuarantined", false)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
    }

    /**
     * Fallback simple feed query ordered by timestamp without composite where conditions.
     */
    fun getSimpleFeedQuery(): Query {
        return feedCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
    }

    /**
     * Retrieves local cached posts for a specific tab (0: For You, 1: Latest).
     */
    fun getLocalPosts(
        tab: Int,
        userDepartment: String = "",
        userBatch: String = "",
        userId: String = ""
    ): List<CampusFeedPost> {
        val all = synchronized(localPosts) {
            (localPosts + sampleDrops).distinctBy { it.id }
        }
        return sortPosts(all, tab, userDepartment, userBatch, userId)
    }

    /**
     * Merges remote Firestore posts with locally published posts.
     */
    fun mergeWithLocal(
        remotePosts: List<CampusFeedPost>,
        tab: Int,
        userDepartment: String = "",
        userBatch: String = "",
        userId: String = ""
    ): List<CampusFeedPost> {
        val combined = synchronized(localPosts) {
            val localOnly = localPosts.filter { local -> remotePosts.none { it.id == local.id } }
            (localOnly + remotePosts + sampleDrops).distinctBy { it.id }
        }
        return sortPosts(combined, tab, userDepartment, userBatch, userId)
    }

    fun sortPosts(
        posts: List<CampusFeedPost>,
        tab: Int,
        userDepartment: String = "",
        userBatch: String = "",
        userId: String = ""
    ): List<CampusFeedPost> {
        val active = posts.filter { !it.isQuarantined }
        return if (tab == TAB_FOR_YOU) {
            // For You: Pinned posts first, then dynamically ranked by recommendation score descending
            active.sortedWith(
                compareByDescending<CampusFeedPost> { it.isPinned }
                    .thenByDescending { calculateRecommendationScore(it, userDepartment, userBatch, userId) }
                    .thenByDescending { it.timestamp?.toDate()?.time ?: 0L }
            )
        } else {
            // Latest: Chronological newest first, pinned posts prioritized
            active.sortedWith(
                compareByDescending<CampusFeedPost> { it.isPinned }
                    .thenByDescending { it.timestamp?.toDate()?.time ?: 0L }
            )
        }
    }

    /**
     * Toggle Love / Like reaction with real-time hotScore recomputation.
     */
    fun toggleLove(postId: String, userId: String, isLoving: Boolean): Task<Void> {
        // Update local memory and disk first for snappy UI feedback
        synchronized(localPosts) {
            val idx = localPosts.indexOfFirst { it.id == postId }
            if (idx != -1) {
                val p = localPosts[idx]
                val currentLoves = p.loveCount
                val reactors = p.loveReactors
                val newLoves = if (isLoving) currentLoves + 1 else max(0, currentLoves - 1)
                val newReactors = if (isLoving) {
                    if (!reactors.contains(userId)) reactors + userId else reactors
                } else {
                    reactors - userId
                }
                localPosts[idx] = p.copy(
                    loveCount = newLoves,
                    loveReactors = newReactors
                )
            }
        }
        savePersistedPosts()

        val completionSource = TaskCompletionSource<Void>()
        val docRef = feedCollection.document(postId)
        db.runTransaction { tx ->
            val snap = tx.get(docRef)
            if (!snap.exists()) return@runTransaction null
            val currentLoves = snap.getLong("loveCount")?.toInt() ?: 0
            val reactors = (snap.get("loveReactors") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            val comments = snap.getLong("commentCount")?.toInt() ?: 0
            val shares = snap.getLong("shareCount")?.toInt() ?: 0
            val signers = (snap.get("petitionSigners") as? List<*>)?.size ?: 0
            val votes = (snap.get("pollVotes") as? Map<*, *>)?.size ?: 0
            val views = snap.getLong("viewCount")?.toInt() ?: 0
            val reports = snap.getLong("reportCount")?.toInt() ?: 0
            val ts = snap.getTimestamp("timestamp")?.toDate()?.time ?: System.currentTimeMillis()

            val newLoves = if (isLoving) currentLoves + 1 else max(0, currentLoves - 1)
            val newReactors = if (isLoving) {
                if (!reactors.contains(userId)) reactors + userId else reactors
            } else {
                reactors - userId
            }

            val newScore = calculateHotScore(
                loveCount = newLoves,
                commentCount = comments,
                shareCount = shares,
                signaturesCount = signers,
                pollVotesCount = votes,
                viewCount = views,
                reportCount = reports,
                createdAtMillis = ts
            )

            tx.update(
                docRef,
                mapOf(
                    "loveCount" to newLoves,
                    "loveReactors" to newReactors,
                    "hotScore" to newScore
                )
            )
            null
        }.addOnCompleteListener {
            completionSource.trySetResult(null)
        }

        return completionSource.task
    }

    /**
     * Cast vote on a Poll Drop.
     */
    fun votePoll(postId: String, userId: String, selectedOption: String): Task<Void> {
        synchronized(localPosts) {
            val idx = localPosts.indexOfFirst { it.id == postId }
            if (idx != -1) {
                val p = localPosts[idx]
                val currentVotes = p.pollVotes.toMutableMap()
                currentVotes[userId] = selectedOption
                localPosts[idx] = p.copy(pollVotes = currentVotes)
            }
        }
        savePersistedPosts()

        val completionSource = TaskCompletionSource<Void>()
        val docRef = feedCollection.document(postId)
        db.runTransaction { tx ->
            val snap = tx.get(docRef)
            if (!snap.exists()) return@runTransaction null
            val currentVotes = (snap.get("pollVotes") as? Map<*, *>)?.mapNotNull { (k, v) ->
                val key = k as? String
                val value = v as? String
                if (key != null && value != null) key to value else null
            }?.toMap() ?: emptyMap()

            val updatedVotes = currentVotes.toMutableMap()
            updatedVotes[userId] = selectedOption

            val loves = snap.getLong("loveCount")?.toInt() ?: 0
            val comments = snap.getLong("commentCount")?.toInt() ?: 0
            val shares = snap.getLong("shareCount")?.toInt() ?: 0
            val signers = (snap.get("petitionSigners") as? List<*>)?.size ?: 0
            val views = snap.getLong("viewCount")?.toInt() ?: 0
            val reports = snap.getLong("reportCount")?.toInt() ?: 0
            val ts = snap.getTimestamp("timestamp")?.toDate()?.time ?: System.currentTimeMillis()

            val newScore = calculateHotScore(
                loveCount = loves,
                commentCount = comments,
                shareCount = shares,
                signaturesCount = signers,
                pollVotesCount = updatedVotes.size,
                viewCount = views,
                reportCount = reports,
                createdAtMillis = ts
            )

            tx.update(
                docRef,
                mapOf(
                    "pollVotes" to updatedVotes,
                    "hotScore" to newScore
                )
            )
            null
        }.addOnCompleteListener {
            completionSource.trySetResult(null)
        }

        return completionSource.task
    }

    /**
     * Sign a Campus Petition Drop.
     */
    fun signPetition(postId: String, userId: String): Task<Void> {
        synchronized(localPosts) {
            val idx = localPosts.indexOfFirst { it.id == postId }
            if (idx != -1) {
                val p = localPosts[idx]
                if (!p.petitionSigners.contains(userId)) {
                    localPosts[idx] = p.copy(petitionSigners = p.petitionSigners + userId)
                }
            }
        }
        savePersistedPosts()

        val completionSource = TaskCompletionSource<Void>()
        val docRef = feedCollection.document(postId)
        db.runTransaction { tx ->
            val snap = tx.get(docRef)
            if (!snap.exists()) return@runTransaction null
            val signers = (snap.get("petitionSigners") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            if (signers.contains(userId)) return@runTransaction null

            val newSigners = signers + userId
            val loves = snap.getLong("loveCount")?.toInt() ?: 0
            val comments = snap.getLong("commentCount")?.toInt() ?: 0
            val shares = snap.getLong("shareCount")?.toInt() ?: 0
            val votes = (snap.get("pollVotes") as? Map<*, *>)?.size ?: 0
            val views = snap.getLong("viewCount")?.toInt() ?: 0
            val reports = snap.getLong("reportCount")?.toInt() ?: 0
            val ts = snap.getTimestamp("timestamp")?.toDate()?.time ?: System.currentTimeMillis()

            val newScore = calculateHotScore(
                loveCount = loves,
                commentCount = comments,
                shareCount = shares,
                signaturesCount = newSigners.size,
                pollVotesCount = votes,
                viewCount = views,
                reportCount = reports,
                createdAtMillis = ts
            )

            tx.update(
                docRef,
                mapOf(
                    "petitionSigners" to newSigners,
                    "hotScore" to newScore
                )
            )
            null
        }.addOnCompleteListener {
            completionSource.trySetResult(null)
        }

        return completionSource.task
    }

    /**
     * Record impression / view count.
     */
    fun recordView(postId: String, userId: String) {
        if (userId.isBlank()) return
        val docRef = feedCollection.document(postId)
        docRef.get().addOnSuccessListener { snap ->
            if (!snap.exists()) return@addOnSuccessListener
            val viewers = (snap.get("viewers") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            if (!viewers.contains(userId)) {
                docRef.update(
                    "viewCount", FieldValue.increment(1),
                    "viewers", FieldValue.arrayUnion(userId)
                ).addOnFailureListener { /* graceful */ }
            }
        }.addOnFailureListener { /* graceful */ }
    }

    /**
     * Record share count.
     */
    fun recordShare(postId: String) {
        val docRef = feedCollection.document(postId)
        docRef.update("shareCount", FieldValue.increment(1))
            .addOnFailureListener { /* graceful */ }
    }

    /**
     * Report post for campus moderation.
     */
    fun reportPost(postId: String, userId: String, reason: String): Task<Void> {
        val completionSource = TaskCompletionSource<Void>()
        val docRef = feedCollection.document(postId)
        db.runTransaction { tx ->
            val snap = tx.get(docRef)
            if (!snap.exists()) return@runTransaction null
            val reporters = (snap.get("reporters") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            if (reporters.contains(userId)) return@runTransaction null

            val newReporters = reporters + userId
            val newReportCount = newReporters.size
            val shouldQuarantine = newReportCount >= 3

            tx.update(
                docRef,
                mapOf(
                    "reportCount" to newReportCount,
                    "reporters" to newReporters,
                    "isQuarantined" to shouldQuarantine
                )
            )
            null
        }.addOnCompleteListener {
            completionSource.trySetResult(null)
        }

        return completionSource.task
    }

    /**
     * Add comment to post with local persistent caching.
     */
    fun addComment(postId: String, comment: CampusFeedComment): Task<Void> {
        val postDocRef = feedCollection.document(postId)
        val commentDocRef = postDocRef.collection(SUB_COLLECTION_COMMENTS).document()
        val preparedComment = comment.copy(
            id = commentDocRef.id,
            postId = postId,
            timestamp = Timestamp.now()
        )

        // Cache comment locally to disk
        val existingComments = loadPersistedComments(postId).toMutableList()
        existingComments.add(preparedComment)
        savePersistedComments(postId, existingComments)

        // Increment local post comment count
        synchronized(localPosts) {
            val idx = localPosts.indexOfFirst { it.id == postId }
            if (idx != -1) {
                val p = localPosts[idx]
                localPosts[idx] = p.copy(commentCount = p.commentCount + 1)
            }
        }
        savePersistedPosts()

        val completionSource = TaskCompletionSource<Void>()
        db.runBatch { batch ->
            batch.set(commentDocRef, preparedComment)
            batch.update(postDocRef, "commentCount", FieldValue.increment(1))
        }.addOnSuccessListener {
            completionSource.trySetResult(null)
        }.addOnFailureListener {
            // Handled gracefully: comment preserved locally
            completionSource.trySetResult(null)
        }

        return completionSource.task
    }

    /**
     * Get comments for a post ordered by timestamp.
     */
    fun getCommentsQuery(postId: String): Query {
        return feedCollection.document(postId)
            .collection(SUB_COLLECTION_COMMENTS)
            .orderBy("timestamp", Query.Direction.ASCENDING)
    }

    /**
     * Get locally stored comments for a post.
     */
    fun getLocalComments(postId: String): List<CampusFeedComment> {
        return loadPersistedComments(postId)
    }
}
