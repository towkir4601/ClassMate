package com.shuaib.classmate.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.shuaib.classmate.databinding.ActivityEditProfileBinding

class EditProfileActivity : AppCompatActivity() {
    private var originalBatch: String = ""


    private lateinit var binding: ActivityEditProfileBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        binding.toolbar.setNavigationOnClickListener { finish() }
        
        loadUserProfile()
        
        binding.btnSave.setOnClickListener {
            saveUserProfile()
        }
    }
    
    private fun loadUserProfile() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    binding.etName.setText(doc.getString("name") ?: "")
                    binding.etStudentId.setText(doc.getString("studentId") ?: "")
                    binding.etDepartment.setText(doc.getString("department") ?: "")
                    binding.etPhone.setText(doc.getString("phone") ?: "")
                    binding.etWhatsApp.setText(doc.getString("whatsappNumber") ?: "")
                    binding.etBatch.setText(doc.getString("batch") ?: "")
                    originalBatch = doc.getString("batch") ?: ""
                    binding.etFatherName.setText(doc.getString("fatherName") ?: "")
                    binding.etMotherName.setText(doc.getString("motherName") ?: "")
                    binding.etPresentAddress.setText(doc.getString("presentAddress") ?: "")
                    binding.etPermanentAddress.setText(doc.getString("permanentAddress") ?: "")
                    binding.etDistrict.setText(doc.getString("homeDistrict") ?: "")
                    binding.etBloodGroup.setText(doc.getString("bloodGroup") ?: "")
                    
                    if (doc.getString("role") == "teacher") {
                        (binding.etStudentId.parent as? android.view.View)?.visibility = android.view.View.GONE
                        (binding.etBatch.parent as? android.view.View)?.visibility = android.view.View.GONE
                        (binding.etFatherName.parent as? android.view.View)?.visibility = android.view.View.GONE
                        (binding.etMotherName.parent as? android.view.View)?.visibility = android.view.View.GONE
                        (binding.etPresentAddress.parent as? android.view.View)?.visibility = android.view.View.GONE
                        (binding.etPermanentAddress.parent as? android.view.View)?.visibility = android.view.View.GONE
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun saveUserProfile() {
        val uid = auth.currentUser?.uid ?: return
        
        val newBatch = binding.etBatch.text.toString().trim()
        val updates = hashMapOf<String, Any>(
            "name" to binding.etName.text.toString().trim(),
            "studentId" to binding.etStudentId.text.toString().trim(),
            "department" to binding.etDepartment.text.toString().trim(),
            "phone" to binding.etPhone.text.toString().trim(),
            "whatsappNumber" to binding.etWhatsApp.text.toString().trim(),
            "batch" to newBatch,
            "fatherName" to binding.etFatherName.text.toString().trim(),
            "motherName" to binding.etMotherName.text.toString().trim(),
            "presentAddress" to binding.etPresentAddress.text.toString().trim(),
            "permanentAddress" to binding.etPermanentAddress.text.toString().trim(),
            "homeDistrict" to binding.etDistrict.text.toString().trim(),
            "bloodGroup" to binding.etBloodGroup.text.toString().trim(),
            "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )
        
        if (newBatch != originalBatch && originalBatch.isNotEmpty()) {
            updates["approved"] = false
            android.widget.Toast.makeText(this, "Batch changed. You will need admin approval again.", android.widget.Toast.LENGTH_LONG).show()
        }
        
        binding.btnSave.isEnabled = false
        binding.btnSave.text = "Saving..."
        
        db.collection("users").document(uid)
            .set(updates, SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                binding.btnSave.isEnabled = true
                binding.btnSave.text = "Save Profile"
                Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show()
            }
    }
}
