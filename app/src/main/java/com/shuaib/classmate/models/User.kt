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
    val isOnline: Boolean = false
) {
    @com.google.firebase.firestore.Exclude
    fun isProfileComplete(): Boolean {
        if (role == "teacher") return true
        return studentId.isNotBlank() && department.isNotBlank()
    }

    @com.google.firebase.firestore.Exclude
    fun hasPermission(permissionKey: String): Boolean {
        val r = role.trim().lowercase()
        if (r == "superadmin") return true
        
        if (r == "teacher") {
            if (permissionKey == "canSendClassCancel") return true
            return permissions[permissionKey] == true
        }
        
        // Admin default permissions
        if (r == "admin") {
            if (permissionKey == "canUploadSeatPlan" || 
                permissionKey == "canManageAcademicCalendar" || 
                permissionKey == "canManageAdmins") {
                return permissions[permissionKey] == true
            }
            return true
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
        val r = role.trim().lowercase()
        if (r == "superadmin" || r == "admin") return true
        return permissions["canManageUsers"] == true
    }
    
    @com.google.firebase.firestore.Exclude
    fun canManageAdmins(): Boolean = hasPermission("canManageAdmins")

    @com.google.firebase.firestore.Exclude
    fun isApproved(): Boolean {
        val r = role.lowercase()
        if (r == "superadmin" || r == "admin") return true
        return approved
    }

    @com.google.firebase.firestore.Exclude
    fun isAdmin(): Boolean {
        val r = role.lowercase()
        return r == "superadmin" || r == "admin" || permissions.values.any { it }
    }

    companion object {
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
