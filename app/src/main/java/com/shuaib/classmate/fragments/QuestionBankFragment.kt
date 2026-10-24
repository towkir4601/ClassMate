package com.shuaib.classmate.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.shuaib.classmate.R
import com.shuaib.classmate.adapters.SubjectAdapter
import com.shuaib.classmate.databinding.FragmentQuestionBankBinding
import com.shuaib.classmate.models.PdfFile
import com.shuaib.classmate.utils.Subject
import com.shuaib.classmate.utils.SubjectList
import com.shuaib.classmate.utils.applyClickAnimation

class QuestionBankFragment : Fragment() {

    private var _binding: FragmentQuestionBankBinding? = null
    private val binding get() = _binding!!

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var adapter: SubjectAdapter

    private var qbSubjects = emptyList<Subject>()
    private var isAdmin = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentQuestionBankBinding.inflate(inflater, container, false)
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.btnBack.applyClickAnimation { findNavController().navigateUp() }
        
        adapter = SubjectAdapter(emptyList(), emptyMap()) { subject ->
            val args = Bundle().apply { 
                putString("subjectName", subject.name) 
                putBoolean("isQuestionBank", true)
            }
            findNavController().navigate(R.id.action_questionBankFragment_to_subjectPdfListFragment, args)
        }
        
        adapter.onLongClick = { subject ->
            if (isAdmin) showEditSubjectDialog(subject)
            else Toast.makeText(requireContext(), "Only admins can edit courses", Toast.LENGTH_SHORT).show()
        }

        binding.rvSubjects.layoutManager = GridLayoutManager(context, 2)
        binding.rvSubjects.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener { loadData() }

        checkAdminAndLoad()
    }

    private var currentUserBatch = ""

    private fun checkAdminAndLoad() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val role = doc.getString("role") ?: "student"
                val permissions = doc.get("permissions") as? Map<String, Boolean> ?: emptyMap()
                currentUserBatch = doc.getString("batch") ?: ""
                isAdmin = (role == "superadmin" || role == "admin" || role == "teacher" || permissions["canUploadLibrary"] == true)
                loadData()
            }
            .addOnFailureListener {
                isAdmin = false
                loadData()
            }
    }

    private fun loadData() {
        binding.swipeRefresh.isRefreshing = true
        SubjectList.fetchSubjects {
            if (_binding == null) return@fetchSubjects

            com.google.android.gms.tasks.Tasks.whenAllSuccess<com.google.firebase.firestore.QuerySnapshot>(
                db.collection("question_bank").whereEqualTo("isDeleted", false).get(),
                db.collection("library_files").whereEqualTo("isDeleted", false).get()
            ).addOnSuccessListener { snapshots ->
                if (_binding == null) return@addOnSuccessListener
                binding.swipeRefresh.isRefreshing = false

                val qbSnapshot = snapshots[0]
                val librarySnapshot = snapshots[1]

                val userBatch = context?.let { com.shuaib.classmate.utils.AppPreferences(it).getUserBatch() } ?: ""
                val userDeptId = context?.let { com.shuaib.classmate.utils.AppPreferences(it).getUserDepartmentId() } ?: ""
                val allPdfDocs = (qbSnapshot.documents + librarySnapshot.documents)
                    .map { it.toPdfFile() }
                    .filter { it.canUserAccess(userDeptId, userBatch, isAdmin) }

                val counts = mutableMapOf<String, Int>()
                for (subject in SubjectList.subjects) {
                    val count = allPdfDocs.count { pdf ->
                        pdf.subject.equals(subject.name, ignoreCase = true) ||
                        pdf.subject.equals(subject.fullName, ignoreCase = true) ||
                        (subject.code.isNotBlank() && pdf.subject.equals(subject.code, ignoreCase = true)) ||
                        (subject.code.isNotBlank() && pdf.courseCode.equals(subject.code, ignoreCase = true)) ||
                        SubjectList.isSameCourse(pdf.subject, subject.fullName) ||
                        SubjectList.isSameCourse(pdf.subject, subject.name)
                    }
                    counts[subject.name] = count
                    counts[subject.fullName] = count
                }

                val candidateSubjects = SubjectList.subjects.filter { subject ->
                    val hasQuestions = (counts[subject.name] ?: 0) > 0
                    val isQBType = subject.type == "question_bank"
                    (hasQuestions || isQBType) && (isAdmin || subject.batch.isEmpty() || subject.batch == currentUserBatch)
                }

                val dynamicQBSubjects = allPdfDocs
                    .filter { pdf ->
                        val s = pdf.subject.trim()
                        s.isNotBlank() && SubjectList.subjects.none { subject ->
                            s.equals(subject.name, ignoreCase = true) ||
                            s.equals(subject.fullName, ignoreCase = true) ||
                            (subject.code.isNotBlank() && pdf.courseCode.equals(subject.code, ignoreCase = true)) ||
                            SubjectList.isSameCourse(s, subject.fullName)
                        }
                    }
                    .distinctBy { it.subject }
                    .map { Subject(it.subject, it.courseCode.ifBlank { "LIB0000" }, "question_bank") }

                for (dynamicSub in dynamicQBSubjects) {
                    val count = allPdfDocs.count { pdf ->
                        pdf.subject.equals(dynamicSub.name, ignoreCase = true) ||
                        pdf.subject.equals(dynamicSub.fullName, ignoreCase = true) ||
                        SubjectList.isSameCourse(pdf.subject, dynamicSub.fullName)
                    }
                    counts[dynamicSub.name] = count
                    counts[dynamicSub.fullName] = count
                }

                qbSubjects = (candidateSubjects + dynamicQBSubjects).distinctBy { it.name }
                adapter.updateList(qbSubjects, counts)
                binding.layoutEmpty.isVisible = qbSubjects.isEmpty()
                binding.rvSubjects.isVisible = qbSubjects.isNotEmpty()
            }.addOnFailureListener {
                if (_binding == null) return@addOnFailureListener
                binding.swipeRefresh.isRefreshing = false
                qbSubjects = emptyList()
                adapter.updateList(qbSubjects, emptyMap())
                binding.layoutEmpty.isVisible = true
                binding.rvSubjects.isVisible = false
            }
        }
    }

    private fun showEditSubjectDialog(subject: Subject) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_subject, null)
        val etName = dialogView.findViewById<android.widget.EditText>(R.id.etSubjectName)
        val etCode = dialogView.findViewById<android.widget.EditText>(R.id.etSubjectCode)
        val etBatch = dialogView.findViewById<android.widget.EditText>(R.id.etSubjectBatch)
        val rgType = dialogView.findViewById<android.widget.RadioGroup>(R.id.rgSubjectType)

        etName.setText(subject.name)
        etCode.setText(subject.code)
        etBatch?.setText(subject.batch)

        when (subject.type) {
            "lab" -> rgType.check(R.id.rbLab)
            "other" -> rgType.check(R.id.rbOther)
            "regular" -> rgType.check(R.id.rbRegular)
            else -> rgType.check(R.id.rbQuestionBank)
        }

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Edit Course")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val name = etName.text.toString().trim()
                val code = etCode.text.toString().trim()
                val batch = etBatch?.text?.toString()?.trim() ?: ""
                val type = when (rgType.checkedRadioButtonId) {
                    R.id.rbLab -> "lab"
                    R.id.rbOther -> "other"
                    R.id.rbQuestionBank -> "question_bank"
                    else -> "regular"
                }

                if (name.isNotEmpty()) {
                    val updates = mapOf(
                        "name" to name,
                        "code" to code,
                        "batch" to batch,
                        "type" to type
                    )
                    saveSubjectSafely(subject, updates)
                }
            }
            .setNeutralButton("Delete") { _, _ ->
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Delete Course")
                    .setMessage("Are you sure you want to delete '${subject.name}'? This will not delete the PDFs inside it, but the folder will be gone.")
                    .setPositiveButton("Delete") { _, _ ->
                        deleteSubjectSafely(subject)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveSubjectSafely(subject: Subject, updates: Map<String, Any>) {
        val onSaveSuccess: () -> Unit = {
            Toast.makeText(context, "Course updated", Toast.LENGTH_SHORT).show()
            SubjectList.fetchSubjects { loadData() }
        }

        if (subject.id.isNotBlank()) {
            db.collection("subjects").document(subject.id)
                .set(updates, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener { onSaveSuccess() }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Failed to update: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            db.collection("subjects").get()
                .addOnSuccessListener { snapshot ->
                    val matchedDoc = snapshot.documents.firstOrNull { doc ->
                        val docName = doc.getString("name") ?: ""
                        val docCode = doc.getString("code") ?: ""
                        docName.equals(subject.name, ignoreCase = true) ||
                        (subject.code.isNotBlank() && docCode.equals(subject.code, ignoreCase = true)) ||
                        SubjectList.isSameCourse(docName, subject.name)
                    }
                    if (matchedDoc != null) {
                        matchedDoc.reference.set(updates, com.google.firebase.firestore.SetOptions.merge())
                            .addOnSuccessListener { onSaveSuccess() }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Failed to update: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        db.collection("subjects").add(updates)
                            .addOnSuccessListener { onSaveSuccess() }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .addOnFailureListener {
                    db.collection("subjects").add(updates)
                        .addOnSuccessListener { onSaveSuccess() }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
        }
    }

    private fun deleteSubjectSafely(subject: Subject) {
        val onDeleteSuccess: () -> Unit = {
            Toast.makeText(context, "Course deleted", Toast.LENGTH_SHORT).show()
            SubjectList.fetchSubjects { loadData() }
        }

        if (subject.id.isNotBlank()) {
            db.collection("subjects").document(subject.id).delete()
                .addOnSuccessListener { onDeleteSuccess() }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Failed to delete: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            db.collection("subjects").get()
                .addOnSuccessListener { snapshot ->
                    val matchedDoc = snapshot.documents.firstOrNull { doc ->
                        val docName = doc.getString("name") ?: ""
                        val docCode = doc.getString("code") ?: ""
                        docName.equals(subject.name, ignoreCase = true) ||
                        (subject.code.isNotBlank() && docCode.equals(subject.code, ignoreCase = true)) ||
                        SubjectList.isSameCourse(docName, subject.name)
                    }
                    if (matchedDoc != null) {
                        matchedDoc.reference.delete()
                            .addOnSuccessListener { onDeleteSuccess() }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Failed to delete: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        onDeleteSuccess()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Failed to delete course", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun DocumentSnapshot.toPdfFile(): PdfFile = PdfFile.fromDocument(this)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
