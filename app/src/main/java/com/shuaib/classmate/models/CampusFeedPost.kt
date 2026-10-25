package com.shuaib.classmate.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * GSTU Go - CampusFeed Post model.
 * Powers the Twitter/X-style algorithmic campus square.
 * Supports Drops: "thought", "poll", "petition", "interest".
 */
@IgnoreExtraProperties
data class CampusFeedPost(
    val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorHandle: String = "",
    val authorAvatar: String = "",
    val authorRole: String = "student",       // "student", "cr", "teacher", "admin", "superadmin"
    val authorDepartment: String = "",
    val authorBatch: String = "",
    val content: String = "",
    val imageUrls: List<String> = emptyList(),
    val hashtags: List<String> = emptyList(),
    val timestamp: Timestamp? = null,

    // Drop Type: "thought", "poll", "petition", "interest"
    val dropType: String = "thought",

    // --- Poll Drop Payload ---
    val pollQuestion: String = "",
    val pollOptions: List<String> = emptyList(),
    val pollVotes: Map<String, String> = emptyMap(), // userId -> selectedOption
    val pollExpiresAt: Timestamp? = null,

    // --- Petition Drop Payload ---
    val petitionTitle: String = "",
    val petitionAddressedTo: String = "",
    val petitionTargetGoal: Int = 100,
    val petitionSigners: List<String> = emptyList(), // userIds who signed

    // --- Interest Drop Payload ---
    val interestCategory: String = "",               // e.g. "Robotics", "Web Dev", "Competitive Programming"
    val interestGroupLink: String = "",

    // --- Engagement Counters ---
    val loveCount: Int = 0,
    val loveReactors: List<String> = emptyList(),
    val commentCount: Int = 0,
    val shareCount: Int = 0,
    val viewCount: Int = 0,
    val viewers: List<String> = emptyList(),

    // --- Algorithmic Ranking & Moderation ---
    val hotScore: Double = 0.0,
    val reportCount: Int = 0,
    val reporters: List<String> = emptyList(),
    val isQuarantined: Boolean = false,
    val isPinned: Boolean = false
) {
    // --- Helper Methods ---
    val isPoll: Boolean get() = dropType.equals("poll", ignoreCase = true)
    val isPetition: Boolean get() = dropType.equals("petition", ignoreCase = true)
    val isInterest: Boolean get() = dropType.equals("interest", ignoreCase = true)

    fun hasLoved(userId: String): Boolean =
        userId.isNotBlank() && loveReactors.contains(userId)

    fun hasVoted(userId: String): Boolean =
        userId.isNotBlank() && pollVotes.containsKey(userId)

    fun userSelectedOption(userId: String): String? =
        pollVotes[userId]

    fun votesForOption(option: String): Int =
        pollVotes.values.count { it == option }

    val totalPollVotes: Int get() = pollVotes.size

    fun pollOptionPercentage(option: String): Int {
        val total = totalPollVotes
        if (total == 0) return 0
        return ((votesForOption(option) * 100) / total).coerceIn(0, 100)
    }

    val isPollExpired: Boolean
        get() {
            val exp = pollExpiresAt ?: return false
            return System.currentTimeMillis() > exp.toDate().time
        }

    fun hasSigned(userId: String): Boolean =
        userId.isNotBlank() && petitionSigners.contains(userId)

    val petitionSignaturesCount: Int get() = petitionSigners.size

    val petitionProgressPercentage: Int
        get() {
            if (petitionTargetGoal <= 0) return 100
            return ((petitionSignaturesCount * 100) / petitionTargetGoal).coerceIn(0, 100)
        }
}
