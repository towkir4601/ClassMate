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
import com.google.firebase.firestore.FirebaseFirestore
import com.shuaib.classmate.R
import com.shuaib.classmate.adapters.SubjectAdapter
import com.shuaib.classmate.databinding.FragmentQuestionBankBinding
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
            val args = Bundle().apply { putString("subjectName", subject.name) }
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
            

            
            db.collection("library_files")
                .whereEqualTo("isDeleted", false)
                .get()
                .addOnSuccessListener { snapshot ->
                    if (_binding == null) return@addOnSuccessListener
                    binding.swipeRefresh.isRefreshing = false
                    
                    val subjectsList = snapshot.documents.mapNotNull { doc ->
                        if (doc.getBoolean("isDeleted") == true) null
                        else doc.getString("subject")
                    }
                    
                    val pdfCounts = subjectsList.groupingBy { it }.eachCount()
                    
                    qbSubjects = SubjectList.subjects.filter { 
                        it.type == "question_bank" && (isAdmin || it.batch.isEmpty() || it.batch == currentUserBatch) 
                    }
                    adapter.updateList(qbSubjects, pdfCounts)
                    
                    binding.layoutEmpty.isVisible = qbSubjects.isEmpty()
                    binding.rvSubjects.isVisible = qbSubjects.isNotEmpty()
                }
                .addOnFailureListener {
                    if (_binding == null) return@addOnFailureListener
                    binding.swipeRefresh.isRefreshing = false
                    qbSubjects = SubjectList.subjects.filter { 
                        it.type == "question_bank" && (isAdmin || it.batch.isEmpty() || it.batch == currentUserBatch) 
                    }
                    adapter.updateList(qbSubjects, emptyMap())
                    
                    binding.layoutEmpty.isVisible = qbSubjects.isEmpty()
                    binding.rvSubjects.isVisible = qbSubjects.isNotEmpty()
                }
        }
    }

    private fun showEditSubjectDialog(subject: Subject) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_subject, null)
        val etName = dialogView.findViewById<android.widget.EditText>(R.id.etSubjectName)
        val etCode = dialogView.findViewById<android.widget.EditText>(R.id.etSubjectCode)
        val rgType = dialogView.findViewById<android.widget.RadioGroup>(R.id.rgSubjectType)

        etName.setText(subject.name)
        etCode.setText(subject.code)
        rgType.check(R.id.rbQuestionBank)

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Edit Course")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val name = etName.text.toString().trim()
                val code = etCode.text.toString().trim()
                val type = when (rgType.checkedRadioButtonId) {
                    R.id.rbLab -> "lab"
                    R.id.rbOther -> "other"
                    R.id.rbQuestionBank -> "question_bank"
                    else -> "regular"
                }

                if (name.isNotEmpty()) {
                    val updates = mapOf("name" to name, "code" to code, "type" to type)
                    db.collection("subjects").document(subject.id).update(updates)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Course updated", Toast.LENGTH_SHORT).show()
                            loadData()
                        }
                }
            }
            .setNeutralButton("Delete") { _, _ ->
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Delete Course")
                    .setMessage("Are you sure you want to delete '${subject.name}'? This will not delete the PDFs inside it, but the folder will be gone.")
                    .setPositiveButton("Delete") { _, _ ->
                        db.collection("subjects").document(subject.id).delete()
                            .addOnSuccessListener {
                                Toast.makeText(context, "Course deleted", Toast.LENGTH_SHORT).show()
                                loadData()
                            }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
