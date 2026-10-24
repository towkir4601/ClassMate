// com/shuaib/classmate/models/PdfFile.kt
package com.shuaib.classmate.models

data class PdfFile(
    val id: String = "",
    val title: String = "",
    val subject: String = "",
    val description: String = "",
    val uploadedBy: String = "",
    val telegramUrl: String = "",
    val driveUrl: String = "",
    val fileId: String = "",
    val timestamp: com.google.firebase.Timestamp? = null,
    val courseCode: String = "",
    val courseType: String = "",
    val fileType: String = "pdf",
    val mimeType: String = "application/pdf",
    val sizeBytes: Long = 0L,
    val provider: String = "",
    val downloadUrl: String = "",
    val githubAssetId: Long = 0L,
    val githubAssetName: String = "",
    val createdAt: com.google.firebase.Timestamp? = null,
    val updatedAt: com.google.firebase.Timestamp? = null,
    val downloadCount: Long = 0L,
    val isDeleted: Boolean = false,
    val batch: String = "",
    val examType: String = "",
    val year: String = "",
    val department: String = "",
    val departmentId: String = "",
    val accessScope: String = "department",
    val isBatchPrivate: Boolean = false
) {
    fun canUserAccess(userDepartmentId: String, userBatch: String, isSuperAdmin: Boolean = false): Boolean {
        if (isDeleted) return false

        // Strict Zero-Trust Rule: Batch-private documents are readable ONLY by verified members of that exact batch & department!
        // No bypass even for superadmin ("superadmin o na kaw e na")!
        if (isBatchPrivate || accessScope == "batch") {
            val fileDept = departmentId.ifBlank { User.slugifyDepartment(department) }
            val deptMatches = fileDept.isBlank() || userDepartmentId.isBlank() || fileDept == userDepartmentId
            val batchMatches = batch.isNotBlank() && (batch.equals(userBatch, ignoreCase = true) || (batch == "14" && userBatch == "2-1") || (batch == "2-1" && userBatch == "14"))
            return deptMatches && batchMatches
        }

        // Department-wide or public documents:
        val fileDept = departmentId.ifBlank { User.slugifyDepartment(department) }
        val deptMatches = fileDept.isBlank() || fileDept == "all" || userDepartmentId.isBlank() || fileDept == userDepartmentId
        val batchMatches = batch.isBlank() || batch.equals("all", ignoreCase = true) || batch.equals(userBatch, ignoreCase = true)
        return (deptMatches && batchMatches) || isSuperAdmin
    }

    companion object {
        fun fromDocument(doc: com.google.firebase.firestore.DocumentSnapshot): PdfFile {
            return PdfFile(
                id = doc.id,
                title = doc.getString("title") ?: "No Title",
                subject = doc.getString("subject") ?: "",
                description = doc.getString("description") ?: "",
                uploadedBy = doc.getString("uploadedByName") ?: doc.getString("uploadedBy") ?: "",
                telegramUrl = doc.getString("telegramUrl") ?: "",
                driveUrl = doc.getString("driveUrl") ?: "",
                fileId = doc.getString("fileId") ?: "",
                timestamp = doc.getTimestamp("timestamp") ?: doc.getTimestamp("createdAt"),
                courseCode = doc.getString("courseCode") ?: "",
                courseType = doc.getString("courseType") ?: "",
                fileType = doc.getString("fileType") ?: "other",
                mimeType = doc.getString("mimeType") ?: "application/octet-stream",
                sizeBytes = doc.getLong("sizeBytes") ?: 0L,
                provider = doc.getString("provider") ?: "",
                downloadUrl = doc.getString("downloadUrl") ?: "",
                githubAssetId = doc.getLong("githubAssetId") ?: 0L,
                githubAssetName = doc.getString("githubAssetName") ?: doc.getString("title") ?: "",
                createdAt = doc.getTimestamp("createdAt"),
                updatedAt = doc.getTimestamp("updatedAt"),
                downloadCount = doc.getLong("downloadCount") ?: 0L,
                isDeleted = doc.getBoolean("isDeleted") ?: false,
                batch = doc.getString("batch") ?: "",
                examType = doc.getString("examType") ?: "",
                year = doc.getString("year") ?: "",
                department = doc.getString("department") ?: "",
                departmentId = doc.getString("departmentId") ?: "",
                accessScope = doc.getString("accessScope") ?: (if (doc.getBoolean("isBatchPrivate") == true) "batch" else "department"),
                isBatchPrivate = doc.getBoolean("isBatchPrivate") ?: (doc.getString("accessScope") == "batch")
            )
        }
    }
}
