package com.shuaib.classmate.models

import com.google.firebase.Timestamp

data class ImportantContact(
    val id: String = "",
    val name: String = "",
    val designation: String = "",
    val departmentOrOffice: String = "",
    val phone: String = "",
    val email: String = "",
    val category: String = CATEGORY_GENERAL,
    val isEmergency: Boolean = false,
    val addedByUid: String = "",
    val createdAt: Timestamp? = null
) {
    companion object {
        const val CATEGORY_EMERGENCY = "Emergency"
        const val CATEGORY_ADMINISTRATION = "Administration"
        const val CATEGORY_FACULTY = "Faculty"
        const val CATEGORY_MEDICAL = "Medical"
        const val CATEGORY_SECURITY = "Security"
        const val CATEGORY_GENERAL = "General"
    }
}
