package com.shuaib.classmate.models

data class CampusEvent(
    val id: String = "",
    val clubId: String = "",
    val title: String = "",
    val organizer: String = "",
    val date: String = "", // e.g. "2026-10-20"
    val time: String = "", // e.g. "10:00 AM"
    val dateTime: String = "", // display string: "Oct 20, 2026 • 10:00 AM"
    val eventDateMillis: Long = 0L,
    val location: String = "",
    val category: String = "Campus",
    val status: String = "REGISTRATION OPEN", // "REGISTRATION OPEN" or "CLOSED"
    val isRegistrationOpen: Boolean = true,
    var isRegistered: Boolean = false,
    var registrationStatus: String = "", // "" or "pending" or "confirmed"
    val description: String = "",
    val creatorId: String = "",
    val creatorRole: String = "",
    val registeredUserIds: List<String> = emptyList(),
    val createdAt: Long = 0L
)

data class CampusClub(
    val id: String = "",
    val name: String = "",
    val subtitle: String = "",
    val membersText: String = "",
    val iconRes: Int = 0,
    val iconTint: Int = 0,
    val bgTint: Int = 0,
    val category: String = "",
    val membersCount: Int = 0,
    val president: String = "",
    val presidentId: String = "",
    val presidentName: String = "",
    val gsId: String = "",
    val gsName: String = "",
    val agsId: String = "",
    val agsName: String = "",
    val memberUids: List<String> = emptyList(),
    val description: String = "",
    val contactEmail: String = "",
    var isMember: Boolean = false,
    val createdAt: Long = 0L
) {
    fun isExecutive(userId: String): Boolean {
        if (userId.isBlank()) return false
        return presidentId == userId || gsId == userId || agsId == userId
    }

    fun getUserRole(userId: String, isSuperAdmin: Boolean = false): String {
        if (isSuperAdmin) return "Superadmin"
        if (userId.isBlank()) return "Visitor"
        if (presidentId == userId) return "President"
        if (gsId == userId) return "General Secretary (GS)"
        if (agsId == userId) return "Assistant General Secretary (AGS)"
        if (memberUids.contains(userId)) return "Member"
        return "Visitor"
    }

    fun getRealMemberCountText(): String {
        val count = memberUids.size
        return if (count == 1) "1 Member" else "$count Members"
    }
}

data class EventRegistration(
    val id: String = "",
    val eventId: String = "",
    val eventTitle: String = "",
    val clubId: String = "",
    val clubName: String = "",
    val userId: String = "",
    val fullName: String = "",
    val department: String = "",
    val studentId: String = "",
    val email: String = "",
    val phone: String = "",
    val status: String = "pending", // "pending" or "confirmed"
    val timestamp: Long = 0L
)

data class ClubMembershipRequest(
    val id: String = "",
    val clubId: String = "",
    val clubName: String = "",
    val userId: String = "",
    val fullName: String = "",
    val department: String = "",
    val studentId: String = "",
    val email: String = "",
    val phone: String = "",
    val status: String = "pending", // "pending" or "approved"
    val timestamp: Long = 0L
)
