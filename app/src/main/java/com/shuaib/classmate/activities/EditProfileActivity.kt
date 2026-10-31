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
        
        val openDeptPicker = {
            com.shuaib.classmate.ui.DepartmentPickerBottomSheet.show(
                supportFragmentManager,
                "Select Department",
                binding.etDepartment.text.toString().trim()
            ) { chosen ->
                binding.etDepartment.setText(chosen)
            }
        }
        binding.etDepartment.isFocusable = false
        binding.etDepartment.isFocusableInTouchMode = false
        binding.etDepartment.isCursorVisible = false
        binding.etDepartment.isClickable = true
        binding.etDepartment.setOnClickListener { openDeptPicker() }
        binding.tilDepartment.setEndIconOnClickListener { openDeptPicker() }
        com.shuaib.classmate.utils.PhoneNumberUtils.attachPhonePrefixWatcher(binding.etPhone)
        com.shuaib.classmate.utils.PhoneNumberUtils.attachPhonePrefixWatcher(binding.etWhatsApp)

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
                    val batchObj = doc.get("batch")
                    val batchStr = when (batchObj) {
                        is String -> batchObj
                        is Number -> batchObj.toString()
                        else -> ""
                    }
                    binding.etBatch.setText(batchStr)
                    binding.etSemester.setText(doc.getString("semester") ?: "")
                    binding.etTeacherCode.setText(doc.getString("teacherCode") ?: "")
                    originalBatch = batchStr
                    
                    // Lock batch field if already set — prevent batch change fraud
                    if (originalBatch.isNotBlank()) {
                        binding.etBatch.isEnabled = false
                        binding.etBatch.alpha = 0.5f
                        binding.etBatch.hint = "Batch cannot be changed"
                    }
                    binding.etFatherName.setText(doc.getString("fatherName") ?: "")
                    binding.etMotherName.setText(doc.getString("motherName") ?: "")
                    binding.etPresentAddress.setText(doc.getString("presentAddress") ?: "")
                    binding.etPermanentAddress.setText(doc.getString("permanentAddress") ?: "")
                    binding.etDistrict.setText(doc.getString("homeDistrict") ?: "")
                    binding.etBloodGroup.setText(doc.getString("bloodGroup") ?: "")
                    
                    if (doc.getString("role") == "teacher") {
                        (binding.etStudentId.parent as? android.view.View)?.visibility = android.view.View.GONE
                        (binding.etBatch.parent as? android.view.View)?.visibility = android.view.View.GONE
                        (binding.etSemester.parent as? android.view.View)?.visibility = android.view.View.GONE
                        binding.tilTeacherCode.visibility = android.view.View.VISIBLE
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
        
        val rawBatch = binding.etBatch.text.toString().trim()
        val batchDigits = rawBatch.filter { it.isDigit() }
        val newBatch = if (batchDigits.isNotBlank()) batchDigits else rawBatch
        val newSemester = binding.etSemester.text.toString().trim()
        val newTeacherCode = binding.etTeacherCode.text.toString().trim().uppercase()
        val chosenDept = binding.etDepartment.text.toString().trim()
        val updates = hashMapOf<String, Any>(
            "uid" to uid,
            "name" to binding.etName.text.toString().trim(),
            "studentId" to binding.etStudentId.text.toString().trim(),
            "department" to chosenDept,
            "departmentId" to com.shuaib.classmate.models.User.slugifyDepartment(chosenDept),
            "phone" to com.shuaib.classmate.utils.PhoneNumberUtils.normalize(binding.etPhone.text.toString()),
            "whatsappNumber" to com.shuaib.classmate.utils.PhoneNumberUtils.normalize(binding.etWhatsApp.text.toString()),
            "semester" to newSemester,
            "teacherCode" to newTeacherCode,
            "fatherName" to binding.etFatherName.text.toString().trim(),
            "motherName" to binding.etMotherName.text.toString().trim(),
            "presentAddress" to binding.etPresentAddress.text.toString().trim(),
            "permanentAddress" to binding.etPermanentAddress.text.toString().trim(),
            "homeDistrict" to binding.etDistrict.text.toString().trim(),
            "bloodGroup" to binding.etBloodGroup.text.toString().trim(),
            "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )
        
        // Only allow setting batch if it was never set before
        if (originalBatch.isBlank() && newBatch.isNotBlank()) {
            updates["batch"] = newBatch
        }
        
        binding.btnSave.isEnabled = false
        binding.btnSave.text = "Saving..."
        
        val handleSuccess = {
            try {
                com.onesignal.OneSignal.User.addTag("semester", newSemester)
                val prefs = com.shuaib.classmate.utils.AppPreferences(this)
                prefs.setUserSemester(newSemester)
                prefs.setTeacherCode(newTeacherCode)
            } catch (e: Exception) {
                // ignore
            }

            if (originalBatch.isBlank() && newBatch.isNotBlank()) {
                try {
                    com.onesignal.OneSignal.User.addTag("batch", newBatch)
                    com.shuaib.classmate.utils.AppPreferences(this).setUserBatch(newBatch)
                } catch (e: Exception) {
                    // ignore
                }
            }
            Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
            finish()
        }

        db.collection("users").document(uid)
            .update(updates)
            .addOnSuccessListener { handleSuccess() }
            .addOnFailureListener {
                db.collection("users").document(uid)
                    .set(updates, SetOptions.merge())
                    .addOnSuccessListener { handleSuccess() }
                    .addOnFailureListener { setErr ->
                        binding.btnSave.isEnabled = true
                        binding.btnSave.text = "Save Profile"
                        Toast.makeText(this, "Failed to update profile: ${setErr.message}", Toast.LENGTH_SHORT).show()
                    }
            }
    }
}
