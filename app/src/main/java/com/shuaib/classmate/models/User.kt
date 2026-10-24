/*
 * GSTU Go - User model with full Role-Based Access Control (RBAC)
 * Roles: superadmin, admin (Dept Admin), teacher, club_admin, cr, student, guest
 */
package com.shuaib.classmate.models

import com.google.firebase.Timestamp

data class User(
    val uid: String = "",
    val name: String = "",
    val fullName: String = "",
    val studentId: String = "",
    val batch: String = "",
    val semester: String = "",
    val teacherCode: String = "",
    val department: String = "",
    val email: String = "",
    val phone: String = "",
    val whatsappNumber: String = "",
    val bloodGroup: String = "",
    val homeDistrict: String = "",
    val address: String = "",
    val presentAddress: String = "",
    val permanentAddress: String = "",
    val fatherName: String = "",
    val motherName: String = "",
    val role: String = "student",
    val approved: Boolean = false,
    val photoUrl: String = "",
    val authProvider: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val oneSignalPlayerId: String = "",
    val favoriteSubjects: List<String> = emptyList(),
    val favoritePdfIds: List<String> = emptyList(),
    val permissions: Map<String, Boolean> = DEFAULT_PERMISSIONS,
    val isOnline: Boolean = false,
    val assignedClubId: String = "",
    val assignedClubName: String = "",
    val demoRole: String = "",
    val isVerified: Boolean = false
) {
    @get:com.google.firebase.firestore.Exclude
    val activeRole: String
        get() = demoRole.ifBlank { role }

    @get:com.google.firebase.firestore.Exclude
    val departmentId: String
        get() = department.trim().lowercase().replace(" ", "_").replace(".", "")

    @get:com.google.firebase.firestore.Exclude
    val batchCompositeKey: String
        get() = "${departmentId}_${batch.trim()}"

    @com.google.firebase.firestore.Exclude
    fun isProfileComplete(): Boolean {
        if (activeRole == "teacher") return true
        return studentId.isNotBlank() && department.isNotBlank()
    }

    @com.google.firebase.firestore.Exclude
    fun hasPermission(permissionKey: String): Boolean {
        val r = activeRole.trim().lowercase()

        // Superadmin has ALL permissions unconditionally
        if (r == "superadmin") return true

        // Teachers: canSendClassCancel by default, plus any explicitly granted permissions
        if (r == "teacher") {
            if (permissionKey == "canSendClassCancel") return true
            return permissions[permissionKey] == true
        }

        // Dept Admin / University Admin
        if (r == "admin" || r == "university_admin") {
            if (permissionKey == "canManageAdmins") {
                return false
            }
            if (permissionKey == "canUploadSeatPlan" ||
                permissionKey == "canManageAcademicCalendar") {
                return permissions[permissionKey] == true
            }
            if (permissionKey == "canPostNotices" ||
                permissionKey == "canEditTimetable" ||
                permissionKey == "canSendClassCancel" ||
                permissionKey == "canUploadPDF" ||
                permissionKey == "canUploadLibrary" ||
                permissionKey == "canCreatePolls" ||
                permissionKey == "canEditCalendar" ||
                permissionKey == "canManageAcademicCalendar" ||
                permissionKey == "canEditBusSchedule" ||
                permissionKey == "canManageClubEvents" ||
                permissionKey == "canManageLostFound" ||
                permissionKey == "canManageUsers") {
                return true
            }
            return permissions[permissionKey] == true
        }

        // Club Admin: can manage club events and post notices by default
        if (r == "club_admin") {
            if (permissionKey == "canManageClubEvents" || permissionKey == "canPostNotices") return true
            return permissions[permissionKey] == true
        }

        // CR / Batch Rep: can cancel class, create polls, upload batch notes, and post notices by default
        if (r == "cr") {
            return when (permissionKey) {
                "canSendClassCancel", "canCreatePolls", "canUploadLibrary", "canPostNotices" -> true
                else -> permissions[permissionKey] == true
            }
        }

        // Students, guests: only explicitly granted permissions
        return permissions[permissionKey] == true
    }

    @com.google.firebase.firestore.Exclude
    fun canPostNotices(): Boolean = hasPermission("canPostNotices")
    @com.google.firebase.firestore.Exclude
    fun canEditTimetable(): Boolean = hasPermission("canEditTimetable")
    @com.google.firebase.firestore.Exclude
    fun canManageAcademicCalendar(): Boolean = hasPermission("canManageAcademicCalendar")
    @com.google.firebase.firestore.Exclude
    fun canEditCalendar(): Boolean = canManageAcademicCalendar()
    @com.google.firebase.firestore.Exclude
    fun canEditBusSchedule(): Boolean = canEditTimetable()
    @com.google.firebase.firestore.Exclude
    fun canCreatePolls(): Boolean = hasPermission("canCreatePolls")
    @com.google.firebase.firestore.Exclude
    fun canSendClassCancel(): Boolean = hasPermission("canSendClassCancel")
    @com.google.firebase.firestore.Exclude
    fun canUploadPDF(): Boolean = hasPermission("canUploadPDF")
    @com.google.firebase.firestore.Exclude
    fun canUploadLibrary(): Boolean = hasPermission("canUploadLibrary")
    @com.google.firebase.firestore.Exclude
    fun canUploadResult(): Boolean = hasPermission("canUploadResult")
    @com.google.firebase.firestore.Exclude
    fun canUploadSeatPlan(): Boolean = hasPermission("canUploadSeatPlan")
    @com.google.firebase.firestore.Exclude
    fun canManageClubEvents(): Boolean = hasPermission("canManageClubEvents")
    @com.google.firebase.firestore.Exclude
    fun canManageLostFound(): Boolean = hasPermission("canManageLostFound")

    @com.google.firebase.firestore.Exclude
    fun canManageUsers(): Boolean {
        val r = activeRole.lowercase()
        return r == "superadmin" || (r != "admin" && r != "university_admin" && permissions["canManageUsers"] == true)
    }

    @com.google.firebase.firestore.Exclude
    fun canManageAdmins(): Boolean {
        val r = activeRole.lowercase()
        return r == "superadmin" || permissions["canManageAdmins"] == true
    }

    @com.google.firebase.firestore.Exclude
    fun isApproved(): Boolean {
        val r = activeRole.lowercase()
        if (r == "superadmin" || r == "admin" || r == "university_admin") return true
        return approved
    }

    @com.google.firebase.firestore.Exclude
    fun isAdmin(): Boolean {
        val r = activeRole.lowercase()
        return r in listOf("superadmin", "admin", "university_admin", "teacher", "club_admin", "cr") || permissions.values.any { it }
    }

    @com.google.firebase.firestore.Exclude
    fun isAnyAdmin(): Boolean = isAdmin()

    /** Human-readable display label for this user's role */
    @com.google.firebase.firestore.Exclude
    fun getRoleDisplayName(): String = when (activeRole.trim().lowercase()) {
        "superadmin"  -> "Super Admin"
        "admin"       -> "Dept Admin"
        "teacher"     -> "Teacher"
        "club_admin"  -> "Club Admin"
        "cr"          -> "Class Representative"
        "student"     -> "Student"
        "guest"       -> "Guest"
        else          -> activeRole.replaceFirstChar { it.uppercaseChar() }
    }

    @com.google.firebase.firestore.Exclude
    fun isSuperAdmin(): Boolean = activeRole.lowercase() == "superadmin"

    @com.google.firebase.firestore.Exclude
    fun isUniversityAdmin(): Boolean = activeRole.lowercase() in listOf("admin", "university_admin")

    @com.google.firebase.firestore.Exclude
    fun isClubAdmin(): Boolean = activeRole.lowercase() == "club_admin" || assignedClubId.isNotBlank()

    @com.google.firebase.firestore.Exclude
    fun isStudent(): Boolean = activeRole.lowercase() == "student"

    @com.google.firebase.firestore.Exclude
    fun hasEduMail(): Boolean {
        val em = email.trim().lowercase()
        return em.endsWith(".edu.bd") || em.endsWith("@gstu.ac.bd") || em.contains(".gstu.ac.bd")
    }

    @get:com.google.firebase.firestore.Exclude
    val isBlueVerified: Boolean
        get() = isVerified || hasEduMail() || activeRole.lowercase() in listOf("superadmin", "admin", "university_admin")

    companion object {
        fun slugifyDepartment(department: String): String {
            return department.trim().lowercase().replace(" ", "_").replace(".", "").replace("&", "")
        }

        /** All valid assignable roles in the system */
        val ALL_ROLES = listOf("superadmin", "admin", "teacher", "club_admin", "cr", "student")

        val DEFAULT_PERMISSIONS = mapOf(
            // Academic & Departmental
            "canPostNotices"            to false,
            "canEditTimetable"          to false,
            "canManageAcademicCalendar" to false,
            "canCreatePolls"            to false,
            "canSendClassCancel"        to false,
            "canUploadSeatPlan"         to false,
            "canUploadResult"           to false,
            "canUploadLibrary"          to false,
            "canUploadPDF"              to false,
            // Campus & Community
            "canManageClubEvents"       to false,
            "canManageLostFound"        to false,
            // Administrative
            "canManageUsers"            to false,
            "canManageAdmins"           to false
        )

        /** Return default permissions map matching each university role */
        fun getRoleDefaultPermissions(role: String): Map<String, Boolean> {
            val perms = DEFAULT_PERMISSIONS.toMutableMap()
            when (role.trim().lowercase()) {
                "superadmin" -> {
                    perms.keys.forEach { perms[it] = true }
                }
                "admin" -> {
                    perms["canPostNotices"] = true
                    perms["canEditTimetable"] = true
                    perms["canManageAcademicCalendar"] = true
                    perms["canCreatePolls"] = true
                    perms["canSendClassCancel"] = true
                    perms["canUploadSeatPlan"] = true
                    perms["canUploadResult"] = true
                    perms["canUploadLibrary"] = true
                    perms["canUploadPDF"] = true
                    perms["canManageClubEvents"] = true
                    perms["canManageLostFound"] = true
                    perms["canManageUsers"] = true
                    perms["canManageAdmins"] = false
                }
                "teacher" -> {
                    perms["canPostNotices"] = true
                    perms["canSendClassCancel"] = true
                    perms["canUploadLibrary"] = true
                    perms["canUploadPDF"] = true
                    perms["canCreatePolls"] = true
                }
                "club_admin" -> {
                    perms["canManageClubEvents"] = true
                    perms["canPostNotices"] = true
                }
                "cr" -> {
                    perms["canSendClassCancel"] = true
                    perms["canCreatePolls"] = true
                    perms["canUploadLibrary"] = true
                    perms["canPostNotices"] = true
                }
                "student" -> {
                    perms.keys.forEach { perms[it] = false }
                }
            }
            return perms
        }
    }
}
