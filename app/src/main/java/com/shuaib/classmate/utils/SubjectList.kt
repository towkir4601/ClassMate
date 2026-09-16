package com.shuaib.classmate.utils

import com.google.firebase.firestore.FirebaseFirestore

object SubjectList {
    var subjects: List<Subject> = emptyList()

    fun codeFor(subjectName: String): String {
        return subjects.firstOrNull { it.name.equals(subjectName, ignoreCase = true) }?.code.orEmpty()
    }
    
    fun fetchSubjects(onComplete: (() -> Unit)? = null) {
        val db = FirebaseFirestore.getInstance()
        db.collection("subjects").get()
            .addOnSuccessListener { snapshot ->
                val list = mutableListOf<Subject>()
                for (doc in snapshot.documents) {
                    val name = doc.getString("name") ?: continue
                    val code = doc.getString("code") ?: ""
                    val type = doc.getString("type") ?: "regular" // regular, lab, other
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
}

data class Subject(
    val name: String,
    val code: String = "",
    val type: String = "regular",
    val batch: String = "",
    val id: String = ""
)
