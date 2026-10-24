/*
 * C:/Users/USER/AndroidStudioProjects/ClassMate/app/src/main/java/com/shuaib/classmate/models/User.kt
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
        if (r == "superadmin") return true

        if (r == "teacher") {
            if (permissionKey == "canSendClassCancel") return true
            return permissions[permissionKey] == true
        }

        // University Admin: Academic notices, timetables/routines, class cancel, materials
        if (r == "admin" || r == "university_admin") {
            if (permissionKey == "canManageUsers" || permissionKey == "canManageAdmins") {
                return false // University admin does NOT manage system users/admins
            }
            if (permissionKey == "canPostNotices" ||
                permissionKey == "canEditTimetable" ||
                permissionKey == "canSendClassCancel" ||
                permissionKey == "canUploadPDF" ||
                permissionKey == "canUploadLibrary" ||
                permissionKey == "canCreatePolls" ||
                permissionKey == "canEditCalendar" ||
                permissionKey == "canManageAcademicCalendar" ||
                permissionKey == "canEditBusSchedule") {
                return true
            }
            return permissions[permissionKey] == true
        }

        // Club Admin: Scoped to their assigned club
        if (r == "club_admin") {
            return false
        }

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
        return r in listOf("superadmin", "admin", "university_admin") || permissions.values.any { it }
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

        val DEFAULT_PERMISSIONS = mapOf(
            "canPostNotices" to false,
            "canEditTimetable" to false,
            "canManageAcademicCalendar" to false,
            "canCreatePolls" to false,
            "canSendClassCancel" to false,
            "canUploadPDF" to false,
            "canUploadLibrary" to false,
            "canUploadResult" to false,
            "canUploadSeatPlan" to false,
            "canManageUsers" to false,
            "canManageAdmins" to false
        )
    }
}
