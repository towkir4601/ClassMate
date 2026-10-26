package com.shuaib.classmate.activities

enum class CoverType {
    ASSIGNMENT,
    LAB_REPORT
}

data class CoverPageData(
    val type: CoverType = CoverType.LAB_REPORT,
    val university: String = "GOPALGANJ SCIENCE & TECHNOLOGY UNIVERSITY,",
    val universityLocation: String = "GOPALGANJ-8100",
    val department: String = "",
    val studentDepartment: String = department,
    val teacherDepartment: String = "",
    val heading: String = if (type == CoverType.ASSIGNMENT) "Assignment On" else "Lab Report",
    val numberLabel: String = if (type == CoverType.ASSIGNMENT) "Assignment No." else "Experiment No.",
    val documentNumber: String = "",
    val titleLabel: String = if (type == CoverType.ASSIGNMENT) "Assignment Title" else "Experiment Name",
    val documentTitle: String = "",
    val courseCode: String = "",
    val courseTitle: String = "",
    val studentName: String = "",
    val studentId: String = "",
    val year: String = "",
    val semester: String = "",
    val session: String = "",
    val teacherName: String = "",
    val designation: String = "",
    val submissionDate: String = "",
    val isGroupSubmission: Boolean = false,
    val groupMembers: List<String> = emptyList(),
    val logoAssetPath: String = "University_Logo.webp"
)
