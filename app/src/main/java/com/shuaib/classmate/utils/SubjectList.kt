package com.shuaib.classmate.utils

import com.google.firebase.firestore.FirebaseFirestore

object SubjectList {
    var subjects: List<Subject> = emptyList()

    fun codeFor(subjectName: String): String {
        val trimmed = subjectName.trim()
        val match = subjects.firstOrNull { 
            it.name.equals(trimmed, ignoreCase = true) || 
            it.fullName.equals(trimmed, ignoreCase = true) ||
            isSameCourse(it.fullName, trimmed) ||
            (it.code.isNotBlank() && it.code.equals(trimmed, ignoreCase = true))
        }
        if (match != null && match.code.isNotBlank()) return match.code
        return extractCourseCode(trimmed)
    }
    
    fun fetchSubjects(onComplete: (() -> Unit)? = null) {
        val db = FirebaseFirestore.getInstance()
        db.collection("subjects").get()
            .addOnSuccessListener { snapshot ->
                val list = mutableListOf<Subject>()
                for (doc in snapshot.documents) {
                    val isDeleted = doc.getBoolean("isDeleted") ?: false
                    if (isDeleted) continue
                    val name = doc.getString("name") ?: continue
                    val code = doc.getString("code") ?: ""
                    val type = doc.getString("type") ?: "regular" // regular, lab, other, question_bank
                    val batch = doc.getString("batch") ?: ""
                    list.add(Subject(name, code, type, batch, doc.id))
                }
                subjects = list.sortedBy { it.name }
                onComplete?.invoke()
            }
            .addOnFailureListener {
                onComplete?.invoke()
            }
    }

    fun extractCourseCode(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return ""
        val candidate = if (trimmed.contains("-")) trimmed.substringBefore("-").trim() else trimmed
        val match = Regex("^([A-Za-z]{2,5})\\s*-?\\s*(\\d{3,4}[A-Za-z]?)$").find(candidate.replace(" ", ""))
            ?: Regex("^([A-Za-z]{2,5})\\s*-?\\s*(\\d{3,4}[A-Za-z]?)\\b").find(trimmed)
        return if (match != null) {
            (match.groupValues[1] + match.groupValues[2]).uppercase(java.util.Locale.US)
        } else {
            ""
        }
    }

    fun cleanCourseTitle(title: String): String {
        var s = title.trim()
        if (s.contains("-")) {
            s = s.substringAfter("-").trim()
        } else {
            s = s.replaceFirst(Regex("^[A-Za-z]{2,5}\\s*-?\\s*\\d{3,4}[A-Za-z]?\\s*[-:]?\\s*"), "").trim()
        }
        return s.replace(Regex("[^A-Za-z0-9 ]"), " ").replace(Regex("\\s+"), " ").trim().lowercase(java.util.Locale.US)
    }

    fun extractCourseNumber(codeOrSubject: String): String {
        val trimmed = codeOrSubject.trim()
        if (trimmed.isBlank()) return ""
        val candidate = if (trimmed.contains("-")) trimmed.substringBefore("-").trim() else trimmed
        val match = Regex("\\b\\d{3,4}\\b").find(candidate)
            ?: Regex("\\d{3,4}").find(candidate)
            ?: Regex("\\b\\d{3,4}\\b").find(trimmed)
            ?: Regex("\\d{3,4}").find(trimmed)
        return match?.value ?: ""
    }

    fun isSameCourse(subjectA: String, subjectB: String): Boolean {
        val a = subjectA.trim()
        val b = subjectB.trim()
        if (a.isBlank() || b.isBlank()) return false
        if (a.equals(b, ignoreCase = true)) return true

        val codeA = extractCourseCode(a)
        val codeB = extractCourseCode(b)
        if (codeA.isNotBlank() && codeB.isNotBlank() && codeA.equals(codeB, ignoreCase = true)) {
            return true
        }

        val numA = extractCourseNumber(if (codeA.isNotBlank()) codeA else a)
        val numB = extractCourseNumber(if (codeB.isNotBlank()) codeB else b)
        if (numA.isNotBlank() && numB.isNotBlank() && numA != numB) {
            return false
        }

        val titleA = cleanCourseTitle(a)
        val titleB = cleanCourseTitle(b)

        val isLabA = titleA.contains("sessional") || titleA.contains("lab")
        val isLabB = titleB.contains("sessional") || titleB.contains("lab")
        if (isLabA != isLabB && titleA.isNotBlank() && titleB.isNotBlank()) {
            return false
        }

        if (titleA.isNotBlank() && titleB.isNotBlank() && titleA == titleB) {
            return true
        }

        if (numA.isNotBlank() && numA == numB) {
            val prefixA = codeA.filter { it.isLetter() }.uppercase(java.util.Locale.US)
            val prefixB = codeB.filter { it.isLetter() }.uppercase(java.util.Locale.US)
            val prefixesCompatible = prefixA == prefixB ||
                    prefixA.isBlank() || prefixB.isBlank() ||
                    ((prefixA == "CE" || prefixA == "CSE") && (prefixB == "CE" || prefixB == "CSE")) ||
                    ((prefixA == "SWE" || prefixA == "CSE") && (prefixB == "SWE" || prefixB == "CSE"))

            if (prefixesCompatible) {
                if (titleA.isBlank() || titleB.isBlank()) return true
                if (titleA.contains(titleB) || titleB.contains(titleA)) return true
            }
        }

        if (titleA.isNotBlank() && titleB.isNotBlank()) {
            val normA = titleA.replace(" ", "")
            val normB = titleB.replace(" ", "")
            if (normA == normB || normA.trimEnd('s') == normB.trimEnd('s')) {
                return true
            }
        }

        return false
    }

    fun getSemesterForSubject(subjectFullNameOrCode: String): String {
        val trimmed = subjectFullNameOrCode.trim()
        if (trimmed.isBlank()) return ""
        val match = subjects.firstOrNull { isSameCourse(it.fullName, trimmed) || isSameCourse(it.code, trimmed) || it.name.equals(trimmed, true) }
        if (match != null && match.batch.isNotBlank() && match.batch.matches(Regex("^[1-4]-[1-2]$"))) {
            return match.batch
        }
        val code = extractCourseCode(trimmed)
        val digits = code.filter { it.isDigit() }
        if (digits.isNotEmpty()) {
            val year = digits[0]
            if (year in '1'..'4') {
                return "$year-1"
            }
        }
        return ""
    }

    fun effectiveCancelDate(notice: com.shuaib.classmate.models.Notice): String {
        if (!notice.isCancel || notice.isDeleted) return ""
        if (notice.submissionDate.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$"))) {
            return notice.submissionDate
        }
        val isoInBody = Regex("\\b(\\d{4}-\\d{2}-\\d{2})\\b").find(notice.body)?.groupValues?.get(1)
        if (!isoInBody.isNullOrBlank()) return isoInBody

        val baseDate = notice.timestamp?.toDate() ?: java.util.Date()
        val cal = java.util.Calendar.getInstance().apply { time = baseDate }
        if (notice.body.contains("tomorrow", ignoreCase = true)) {
            cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(cal.time)
    }

    fun dayOfWeekFromIso(dateStr: String): String {
        if (dateStr.isBlank()) return ""
        return runCatching {
            val date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).parse(dateStr) ?: return@runCatching ""
            val cal = java.util.Calendar.getInstance().apply { time = date }
            when (cal.get(java.util.Calendar.DAY_OF_WEEK)) {
                java.util.Calendar.SATURDAY -> "saturday"
                java.util.Calendar.SUNDAY -> "sunday"
                java.util.Calendar.MONDAY -> "monday"
                java.util.Calendar.TUESDAY -> "tuesday"
                java.util.Calendar.WEDNESDAY -> "wednesday"
                java.util.Calendar.THURSDAY -> "thursday"
                java.util.Calendar.FRIDAY -> "friday"
                else -> ""
            }
        }.getOrDefault("")
    }
}

data class Subject(
    val name: String,
    val code: String = "",
    val type: String = "regular",
    val batch: String = "",
    val id: String = ""
) {
    val fullName: String
        get() = if (code.isNotBlank()) "$code - $name" else name
}
