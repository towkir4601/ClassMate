// com/shuaib/classmate/models/Notice.kt
package com.shuaib.classmate.models

import com.google.firebase.Timestamp

data class Notice(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val postedBy: String = "",
    val timestamp: Timestamp? = null,
    val type: String = "notice",
    val priority: String = "normal",
    val createdBy: String = "",
    val createdByName: String = "",
    val updatedAt: Timestamp? = null,
    val deadlineAt: Timestamp? = null,
    val attachments: List<Map<String, Any>> = emptyList(),
    val discussionRoomId: String = "",
    val isPinned: Boolean = false,
    val isDeleted: Boolean = false,
    val discussionCount: Int = 0,
    
    // Type flags
    @field:JvmField val isCancel: Boolean = false,
    @field:JvmField val isSub: Boolean = false,
    @field:JvmField val isAssignment: Boolean = false,
    @field:JvmField val isClassTest: Boolean = false,
    @field:JvmField val isResource: Boolean = false,
    
    // Attachments
    val attachmentType: String = "none", // "none", "pdf", "image"
    val attachmentUrl: String = "",
    val attachmentName: String = "",
    val fileId: String = "", // Added for Telegram file_id
    
    // Assignment specific
    val assignmentId: String = "",
    val subject: String = "",
    val topic: String = "",
    val submissionDate: String = "",
    val deadlineType: String = "assignment",
    
    // Resource specific
    val pdfId: String = "",
    
    val readCount: Int = 0,
    val targetBatch: String = "all",
    val department: String = "all",
    val departmentId: String = "all",
    val authorRole: String = ""
) {
    val isSuperAdminNotice: Boolean
        get() = authorRole.equals("superadmin", ignoreCase = true) ||
                postedBy.contains("Super Admin", ignoreCase = true) ||
                createdByName.contains("Super Admin", ignoreCase = true) ||
                postedBy.contains("Vice Chancellor", ignoreCase = true) ||
                createdByName.contains("Vice Chancellor", ignoreCase = true) ||
                postedBy.contains("Registrar", ignoreCase = true) ||
                createdByName.contains("Registrar", ignoreCase = true) ||
                postedBy.equals("VC", ignoreCase = true) ||
                createdByName.equals("VC", ignoreCase = true)
    val displayType: String
        get() = when {
            isCancel -> "cancellation"
            type.isNotBlank() && type != "notice" -> type.lowercase()
            isResource -> "resource"
            isAssignment -> "deadline"
            isClassTest -> "exam"
            isSub -> "notice"
            else -> "notice"
        }

    val displayPriority: String
        get() = priority.ifBlank { "normal" }.lowercase()

    val displaySubject: String
        get() = subject.ifBlank { "General" }

    val displayAuthor: String
        get() = createdByName.ifBlank { postedBy.ifBlank { "GSTU Go" } }

    val content: String
        get() = body

    val createdAt: Timestamp?
        get() = timestamp
}
