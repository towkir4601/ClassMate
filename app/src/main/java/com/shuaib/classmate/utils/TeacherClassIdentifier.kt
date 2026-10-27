package com.shuaib.classmate.utils

import com.shuaib.classmate.models.Period
import com.shuaib.classmate.models.User

object TeacherClassIdentifier {

    /**
     * Identifies if a given [period] belongs to [teacher].
     *
     * Multi-Tier Matching Engine:
     * - Tier 1: Direct UID match (period.teacherUid == teacher.uid)
     * - Tier 2: Direct Email match (period.teacherEmail == teacher.email)
     * - Tier 3: Teacher Code / Acronym match (e.g. "MSU", "FA", "MRK")
     * - Tier 4: Normalized Name match (strips prefixes/honorifics like Dr., Prof., Md., Engr., Mr.)
     */
    fun isTeacherClass(period: Period, teacher: User?): Boolean {
        if (teacher == null || teacher.uid.isBlank()) return false

        // Tier 1: Explicit UID match
        if (period.teacherUid.isNotBlank() && period.teacherUid == teacher.uid) {
            return true
        }

        // Tier 2: Email match
        if (period.teacherEmail.isNotBlank() && teacher.email.isNotBlank() &&
            period.teacherEmail.equals(teacher.email, ignoreCase = true)
        ) {
            return true
        }

        // Tier 3: Teacher Code / Acronym match
        if (teacher.teacherCode.isNotBlank()) {
            val code = teacher.teacherCode.trim()
            if (period.teacherCode.equals(code, ignoreCase = true)) return true
            if (period.teacher.equals(code, ignoreCase = true)) return true
        }

        // Tier 4: Normalized Name Match
        val normPeriodTeacher = normalizeName(period.teacher)
        val normTeacherName = normalizeName(teacher.name)
        val normTeacherFullName = normalizeName(teacher.fullName)

        if (normPeriodTeacher.isNotBlank()) {
            if (normTeacherName.isNotBlank() && (normPeriodTeacher == normTeacherName || normTeacherName.contains(normPeriodTeacher) || normPeriodTeacher.contains(normTeacherName))) {
                return true
            }
            if (normTeacherFullName.isNotBlank() && (normPeriodTeacher == normTeacherFullName || normTeacherFullName.contains(normPeriodTeacher) || normPeriodTeacher.contains(normTeacherFullName))) {
                return true
            }
        }

        return false
    }

    /**
     * Normalizes a teacher name by removing academic titles/honorifics and special characters.
     */
    fun normalizeName(raw: String): String {
        return raw.trim()
            .replace("(?i)^(dr\\.?|prof\\.?|professor|md\\.?|mr\\.?|ms\\.?|mrs\\.?|engr\\.?)\\s*".toRegex(), "")
            .replace("[^a-zA-Z0-9]".toRegex(), "")
            .lowercase()
    }
}
