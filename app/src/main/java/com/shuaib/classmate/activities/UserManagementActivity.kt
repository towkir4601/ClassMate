/*
 * C:/Users/USER/AndroidStudioProjects/ClassMate/app/src/main/java/com/shuaib/classmate/activities/UserManagementActivity.kt
 */
package com.shuaib.classmate.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.shuaib.classmate.R
import com.shuaib.classmate.adapters.UserAdapter
import com.shuaib.classmate.databinding.ActivityUserManagementBinding
import com.shuaib.classmate.models.User

class UserManagementActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var pendingAdapter: UserAdapter
    private lateinit var approvedAdapter: UserAdapter
    private lateinit var binding: ActivityUserManagementBinding
    private val pendingList = mutableListOf<User>()
    private val approvedList = mutableListOf<User>()
    private var currentUser: User? = null

    private fun parseUserSafely(doc: com.google.firebase.firestore.DocumentSnapshot): User? {
        if (!doc.exists()) return null
        return try {
            User(
                uid = doc.id,
                name = doc.getString("name") ?: "",
                fullName = doc.getString("fullName") ?: "",
                studentId = doc.getString("studentId") ?: "",
                department = doc.getString("department") ?: "",
                email = doc.getString("email") ?: "",
                phone = doc.getString("phone") ?: "",
                bloodGroup = doc.getString("bloodGroup") ?: "",
                homeDistrict = doc.getString("homeDistrict") ?: "",
                address = doc.getString("address") ?: "",
                role = doc.getString("role") ?: "student",
                approved = doc.getBoolean("approved") ?: false,
                photoUrl = doc.getString("photoUrl") ?: "",
                authProvider = doc.getString("authProvider") ?: "",
                createdAt = doc.getTimestamp("createdAt"),
                updatedAt = doc.getTimestamp("updatedAt"),
                oneSignalPlayerId = doc.getString("oneSignalPlayerId") ?: "",
                permissions = (doc.get("permissions") as? Map<String, Boolean>) ?: User.DEFAULT_PERMISSIONS
            )
        } catch (e: Exception) {
            null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        val mode = intent.getStringExtra("MODE") ?: "BOTH"
        if (mode == "PENDING") {
            binding.toolbar.title = "Pending Approvals"
        } else if (mode == "APPROVED") {
            binding.toolbar.title = "Manage Users"
        }

        binding.toolbar.setNavigationOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        firestore = FirebaseFirestore.getInstance()
        setupRecyclerView()
        setupBatchFilter()
        fetchCurrentUser()
    }

    private fun fetchCurrentUser() {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        binding.progressBar.visibility = View.VISIBLE
        firestore.collection("users").document(currentUid).get()
            .addOnSuccessListener { doc ->
                binding.progressBar.visibility = View.GONE
                val user = parseUserSafely(doc)
                currentUser = user
                if (user != null && user.canManageUsers()) {
                    fetchAllUsers()
                } else {
                    Toast.makeText(this, "Access restricted to Admins only.", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Log.e("UserManagement", "Error fetching current user info", e)
                Toast.makeText(this, "Error verifying authorization.", Toast.LENGTH_SHORT).show()
                finish()
            }
    }


    private fun setupBatchFilter() {
        val mode = intent.getStringExtra("MODE") ?: "BOTH"
        if (mode == "PENDING") {
            binding.tilBatchFilter.visibility = android.view.View.GONE
            return
        }
        binding.dropdownBatchFilter.setText("All Batches", false)
        binding.dropdownBatchFilter.setOnItemClickListener { _, _, _, _ ->
            updateUIVisibility()
        }
    }

    private fun updateBatchFilterDropdown() {
        val allUsers = pendingList + approvedList
        val availableBatches = allUsers.mapNotNull { it.batch.trim().takeIf { b -> b.isNotBlank() } }
            .distinct()
            .sortedByDescending { it.toIntOrNull() ?: 0 }
            
        val batches = mutableListOf("All Batches")
        batches.addAll(availableBatches)
        
        val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, batches)
        binding.dropdownBatchFilter.setAdapter(adapter)
    }

    private fun setupRecyclerView() {
        val rootDecorView = window.decorView.findViewById<ViewGroup>(android.R.id.content)
        val onUserClick = { user: User ->
            startActivity(
                Intent(this, UserDetailActivity::class.java)
                    .putExtra(UserDetailActivity.EXTRA_USER_ID, user.uid)
            )
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
        val onUserLongClick = { targetUser: User ->
            showUserManagementOptions(targetUser)
        }

        pendingAdapter = UserAdapter(pendingList, rootDecorView, onUserClick)
        pendingAdapter.onUserLongClick = onUserLongClick
        
        approvedAdapter = UserAdapter(approvedList, rootDecorView, onUserClick)
        approvedAdapter.onUserLongClick = onUserLongClick

        binding.rvPendingUsers.apply {
            layoutManager = LinearLayoutManager(this@UserManagementActivity)
            adapter = pendingAdapter
        }
        binding.rvApprovedUsers.apply {
            layoutManager = LinearLayoutManager(this@UserManagementActivity)
            adapter = approvedAdapter
        }
    }

    private fun showUserManagementOptions(targetUser: User) {
        val currUser = currentUser
        if (currUser == null) {
            Toast.makeText(this, "Still loading your admin profile. Please try again.", Toast.LENGTH_SHORT).show()
            return
        }

        if (currUser.uid == targetUser.uid) {
            Toast.makeText(this, "You cannot modify or delete your own account.", Toast.LENGTH_SHORT).show()
            return
        }

        if (targetUser.role == "superadmin" && currUser.role != "superadmin") {
            Toast.makeText(this, "Only Super Admins can manage other Super Admins.", Toast.LENGTH_SHORT).show()
            return
        }

        val optionsList = mutableListOf<String>()
        val isSuperadmin = currUser.role == "superadmin"

        // Approve/Reject option
        if (isSuperadmin || currUser.canManageUsers()) {
            if (targetUser.approved) {
                optionsList.add("Reject User (Remove Approval)")
            } else {
                optionsList.add("Approve User")
            }
        }

        if (isSuperadmin || currUser.canManageAdmins()) {
            optionsList.add("Change Role")
        }

        val canDelete = isSuperadmin || (currUser.canManageUsers() && targetUser.role != "admin" && targetUser.role != "superadmin")
        if (canDelete) {
            optionsList.add("Delete User")
        }

        if (optionsList.isEmpty()) {
            Toast.makeText(this, "You do not have permission to manage this user.", Toast.LENGTH_SHORT).show()
            return
        }

        val options = optionsList.toTypedArray()

        MaterialAlertDialogBuilder(this, R.style.Theme_ClassMate_Dialog)
            .setTitle("Manage ${targetUser.fullName.ifBlank { targetUser.name }}")
            .setItems(options) { _, which ->
                when (options[which]) {
                    "Approve User" -> approveUser(targetUser, true)
                    "Reject User (Remove Approval)" -> approveUser(targetUser, false)
                    "Change Role" -> showChangeRoleDialog(targetUser)
                    "Delete User" -> showDeleteUserConfirmation(targetUser)
                }
            }
            .show()
    }

    private fun approveUser(targetUser: User, approve: Boolean) {
        binding.progressBar.visibility = View.VISIBLE
        val action = if (approve) "approved" else "rejected"

        firestore.collection("users").document(targetUser.uid)
            .update("approved", approve)
            .addOnSuccessListener {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "User ${action} successfully!", Toast.LENGTH_SHORT).show()

            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Failed to $action user: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showChangeRoleDialog(targetUser: User) {
        val currUser = currentUser ?: return
        val isSuperadmin = currUser.role == "superadmin"

        if ((targetUser.role == "admin" || targetUser.role == "superadmin") && !isSuperadmin) {
            Toast.makeText(this, "Only Super Admins can change roles for Admins and Super Admins.", Toast.LENGTH_SHORT).show()
            return
        }

        val roles: Array<CharSequence>
        val roleValues: Array<String>

        if (isSuperadmin) {
            roles = arrayOf("Student", "Admin", "Super Admin")
            roleValues = arrayOf("student", "admin", "superadmin")
        } else {
            roles = arrayOf("Student", "Admin")
            roleValues = arrayOf("student", "admin")
        }

        val currentRoleIndex = roleValues.indexOf(targetUser.role.lowercase()).let { if (it == -1) 0 else it }

        MaterialAlertDialogBuilder(this, R.style.Theme_ClassMate_Dialog)
            .setTitle("Change Role for ${targetUser.fullName.ifBlank { targetUser.name }}")
            .setSingleChoiceItems(roles, currentRoleIndex) { dialog, which ->
                dialog.dismiss()
                val newRole = roleValues[which]
                updateUserRole(targetUser, newRole)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateUserRole(targetUser: User, newRole: String) {
        binding.progressBar.visibility = View.VISIBLE

        val newPermissions = when (newRole) {
            "superadmin" -> User.DEFAULT_PERMISSIONS.mapValues { true }.toMutableMap()
            "admin" -> User.DEFAULT_PERMISSIONS.mapValues { 
                it.key != "canManageUsers" && 
                it.key != "canManageAdmins" && 
                it.key != "canUploadSeatPlan" && 
                it.key != "canManageAcademicCalendar" 
            }.toMutableMap()
            else -> User.DEFAULT_PERMISSIONS.mapValues { false }.toMutableMap()
        }

        val updates = mapOf(
            "role" to newRole,
            "permissions" to newPermissions
        )

        firestore.collection("users").document(targetUser.uid)
            .update(updates)
            .addOnSuccessListener {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Role updated to ${newRole.uppercase()} and permissions updated", Toast.LENGTH_SHORT).show()

            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Failed to update role: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showDeleteUserConfirmation(targetUser: User) {
        MaterialAlertDialogBuilder(this, R.style.Theme_ClassMate_Dialog)
            .setTitle("Delete User")
            .setMessage("Are you sure you want to delete ${targetUser.fullName.ifBlank { targetUser.name }}? This will permanently remove them from the database and friends list.")
            .setPositiveButton("Delete") { _, _ ->
                deleteUser(targetUser)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteUser(targetUser: User) {
        binding.progressBar.visibility = View.VISIBLE

        firestore.collection("users").document(targetUser.uid)
            .delete()
            .addOnSuccessListener {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "User deleted successfully", Toast.LENGTH_SHORT).show()

            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Failed to delete user: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchAllUsers() {
        binding.progressBar.visibility = View.VISIBLE
        firestore.collection("users")
            .addSnapshotListener { snapshot, e ->
                binding.progressBar.visibility = View.GONE
                if (e != null) {
                    Log.w("UserManagement", "Listen failed.", e)
                    return@addSnapshotListener
                }
                pendingList.clear()
                approvedList.clear()
                snapshot?.documents?.forEach { doc ->
                    val user = parseUserSafely(doc)
                    if (user != null) {
                        if (user.approved) {
                            approvedList.add(user)
                        } else {
                            pendingList.add(user)
                        }
                    }
                }
                
                // Sort both lists, newest first
                pendingList.sortByDescending { it.createdAt?.seconds ?: 0L }
                approvedList.sortByDescending { it.createdAt?.seconds ?: 0L }
                
                updateBatchFilterDropdown()
                updateUIVisibility()
            }
    }

    private fun updateUIVisibility() {
        val mode = intent.getStringExtra("MODE") ?: "BOTH"
        val filterBatch = binding.dropdownBatchFilter.text.toString()
        
        val filteredPending = if (filterBatch.isNotBlank() && filterBatch != "All Batches") {
            pendingList.filter { it.batch == filterBatch }
        } else pendingList
        
        val filteredApproved = if (filterBatch.isNotBlank() && filterBatch != "All Batches") {
            approvedList.filter { it.batch == filterBatch }
        } else approvedList
        
        pendingAdapter.updateList(filteredPending)
        approvedAdapter.updateList(filteredApproved)

        if (mode == "PENDING" || mode == "BOTH") {
            binding.tvPendingTitle.visibility = if (filteredPending.isNotEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            binding.rvPendingUsers.visibility = if (filteredPending.isNotEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        } else {
            binding.tvPendingTitle.visibility = android.view.View.GONE
            binding.rvPendingUsers.visibility = android.view.View.GONE
        }

        if (mode == "APPROVED" || mode == "BOTH") {
            binding.tvApprovedTitle.visibility = if (filteredApproved.isNotEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            binding.rvApprovedUsers.visibility = if (filteredApproved.isNotEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        } else {
            binding.tvApprovedTitle.visibility = android.view.View.GONE
            binding.rvApprovedUsers.visibility = android.view.View.GONE
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}
