package com.shuaib.classmate.activities

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.shuaib.classmate.R
import com.shuaib.classmate.adapters.LostFoundAdapter
import com.shuaib.classmate.databinding.ActivityLostFoundBinding
import com.shuaib.classmate.models.LostFoundItem
import com.shuaib.classmate.utils.UserRoleHelper
import com.shuaib.classmate.repositories.LostFoundRepository
import com.shuaib.classmate.repositories.AppNotificationRepository
import com.shuaib.classmate.utils.AppPreferences

import android.net.Uri
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.shuaib.classmate.utils.CloudinaryUploader
import com.shuaib.classmate.utils.ImageCompressor
import kotlinx.coroutines.launch
import java.io.File

class LostFoundActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLostFoundBinding
    private val repo = LostFoundRepository()
    private val notificationRepo = AppNotificationRepository()
    private var allItems = listOf<LostFoundItem>()
    private var currentFilter = "all" // "all", "lost", "found"
    private var searchQuery = ""
    private lateinit var adapter: LostFoundAdapter

    private var currentUserId: String = ""
    private var currentUserName: String = "Student"
    private var isAdmin: Boolean = false
    private var isModerationMode: Boolean = false

    private var pendingCompressedFile: File? = null
    private var pendingCompressedUri: Uri? = null
    private var onPhotoPickedAction: ((Uri) -> Unit)? = null

    private val photoPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            // Immediately copy to a local cache file while content permission is 100% hot and valid
            val localCached = ImageCompressor.copyUriToLocalCache(this, uri)
            val uriToUse = if (localCached != null) Uri.fromFile(localCached) else uri
            onPhotoPickedAction?.invoke(uriToUse)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLostFoundBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUserId = currentUser?.uid ?: ""
        currentUserName = currentUser?.displayName ?: AppPreferences(this).getUserName().ifBlank { "Student" }

        isModerationMode = intent.getStringExtra("MODE") == "MODERATION"
        val savedRole = UserRoleHelper.getEffectiveRole(this)
        val permissions = UserRoleHelper.getEffectivePermissions(this, savedRole)
        isAdmin = savedRole.equals("admin", ignoreCase = true) ||
                  savedRole.equals("superadmin", ignoreCase = true) ||
                  permissions["canManageLostFound"] == true

        AppPreferences(this).getDeletedLostFoundIds().forEach { id ->
            LostFoundRepository.markDeletedLocally(id)
        }

        setupUI()
        verifyAdminStatus()
        loadData()
    }

    private fun verifyAdminStatus() {
        if (currentUserId.isBlank()) return
        if (UserRoleHelper.isTestRoleActive(this)) {
            val role = UserRoleHelper.getEffectiveRole(this)
            val permissions = UserRoleHelper.getEffectivePermissions(this, role)
            val hasLFPerm = permissions["canManageLostFound"] == true
            val newIsAdmin = role.equals("admin", true) || role.equals("superadmin", true) || hasLFPerm
            if (newIsAdmin != isAdmin) {
                isAdmin = newIsAdmin
                adapter.setUserAndRole(currentUserId, currentUserName, isAdmin)
                if (isModerationMode && !isAdmin) {
                    isModerationMode = false
                }
            }
            return
        }
        FirebaseFirestore.getInstance().collection("users").document(currentUserId).get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    val role = doc.getString("role") ?: ""
                    val isUserAdmin = role.equals("admin", ignoreCase = true) ||
                                     role.equals("superadmin", ignoreCase = true)
                    val permissions = doc.get("permissions") as? Map<*, *>
                    val hasLFPerm = permissions?.get("canManageLostFound") == true
                    val newIsAdmin = isUserAdmin || hasLFPerm
                    if (newIsAdmin != isAdmin) {
                        isAdmin = newIsAdmin
                        adapter.setUserAndRole(currentUserId, currentUserName, isAdmin)
                        // Reload with correct filter in case mode changed
                        if (isModerationMode && !isAdmin) {
                            isModerationMode = false
                        }
                    }
                }
            }
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnReportItemHeader.setOnClickListener {
            showReportDialog()
        }

        adapter = LostFoundAdapter(
            items = emptyList(),
            currentUserId = currentUserId,
            currentUserName = currentUserName,
            isAdmin = isAdmin,
            onDeleteClick = { item ->
                confirmDeleteItem(item)
            },
            onItemClick = { item ->
                // Click item
            },
            onStatusChangeClick = { item, status ->
                handleStatusChange(item, status)
            },
            onReportClick = { item ->
                handleReportPost(item)
            }
        )
        binding.rvLostFound.layoutManager = LinearLayoutManager(this)
        binding.rvLostFound.adapter = adapter

        binding.swipeRefreshLostFound.setOnRefreshListener {
            loadData()
        }

        binding.pillFilterAll.setOnClickListener { setFilter("all") }
        binding.pillFilterLost.setOnClickListener { setFilter("lost") }
        binding.pillFilterFound.setOnClickListener { setFilter("found") }

        binding.etSearchLostFound.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString()?.trim() ?: ""
                binding.btnClearSearch.visibility = if (searchQuery.isNotEmpty()) View.VISIBLE else View.GONE
                applyFilters()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnClearSearch.setOnClickListener {
            binding.etSearchLostFound.text?.clear()
        }
    }

    private fun confirmDeleteItem(item: LostFoundItem) {
        AlertDialog.Builder(this)
            .setTitle("Resolve & Remove Item")
            .setMessage("Has this item been returned to its owner? Removing this will delete this post permanently from the board.")
            .setPositiveButton("Remove & Delete") { _, _ ->
                // 1. Immediately remove from local list and refresh adapter
                allItems = allItems.filter { it.id != item.id }
                applyFilters()

                // 2. Persist deletion in preferences and repo
                LostFoundRepository.markDeletedLocally(item.id)
                AppPreferences(this).addDeletedLostFoundId(item.id)

                // 3. Delete from Firestore asynchronously
                repo.deleteItem(
                    item.id,
                    onSuccess = {
                        Toast.makeText(this, "Item removed successfully!", Toast.LENGTH_SHORT).show()
                    },
                    onFailure = {
                        Toast.makeText(this, "Item removed from board.", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setFilter(filter: String) {
        currentFilter = filter

        val selectedBg = R.drawable.bg_badge_accent_pill
        val unselectedBg = R.drawable.bg_lost_found_pill_unselected
        val textMuted = Color.parseColor("#94A3B8")
        val selectedText = Color.parseColor("#C7D2FE")

        // All pill
        binding.pillFilterAll.setBackgroundResource(if (filter == "all") selectedBg else unselectedBg)
        if (filter == "all") {
            binding.pillFilterAll.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#3B3864"))
            binding.pillFilterAll.setTextColor(selectedText)
        } else {
            binding.pillFilterAll.backgroundTintList = null
            binding.pillFilterAll.setTextColor(textMuted)
        }

        // Lost pill
        binding.pillFilterLost.setBackgroundResource(if (filter == "lost") selectedBg else unselectedBg)
        if (filter == "lost") {
            binding.pillFilterLost.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#3B3864"))
            binding.pillFilterLost.setTextColor(selectedText)
        } else {
            binding.pillFilterLost.backgroundTintList = null
            binding.pillFilterLost.setTextColor(textMuted)
        }

        // Found pill
        binding.pillFilterFound.setBackgroundResource(if (filter == "found") selectedBg else unselectedBg)
        if (filter == "found") {
            binding.pillFilterFound.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#3B3864"))
            binding.pillFilterFound.setTextColor(selectedText)
        } else {
            binding.pillFilterFound.backgroundTintList = null
            binding.pillFilterFound.setTextColor(textMuted)
        }

        applyFilters()
    }

    private fun loadData() {
        binding.swipeRefreshLostFound.isRefreshing = true
        if (isModerationMode && isAdmin) {
            // Admin moderation mode: show only PENDING items
            repo.getPendingItems { items ->
                binding.swipeRefreshLostFound.isRefreshing = false
                allItems = items
                applyFilters()
            }
        } else {
            // Normal mode: show only APPROVED (public) items
            repo.getApprovedItems { items ->
                binding.swipeRefreshLostFound.isRefreshing = false
                allItems = items
                applyFilters()
            }
        }
    }

    /** Admin approves a pending Lost & Found post — makes it publicly visible */
    fun approveItem(item: LostFoundItem) {
        val updates = mapOf(
            "approved" to true,
            "pendingApproval" to false,
            "approvedBy" to currentUserId,
            "approvedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )
        FirebaseFirestore.getInstance().collection("lost_found")
            .document(item.id)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(this, getString(R.string.lf_approved_success), Toast.LENGTH_SHORT).show()
                allItems = allItems.filter { it.id != item.id }
                applyFilters()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to approve. Try again.", Toast.LENGTH_SHORT).show()
            }
    }

    /** Admin rejects and permanently removes a pending Lost & Found post */
    fun rejectItem(item: LostFoundItem) {
        FirebaseFirestore.getInstance().collection("lost_found")
            .document(item.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, getString(R.string.lf_rejected_success), Toast.LENGTH_SHORT).show()
                allItems = allItems.filter { it.id != item.id }
                applyFilters()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to reject. Try again.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun applyFilters() {
        var filtered = allItems

        if (currentFilter == "found") {
            filtered = filtered.filter { it.isFound }
        } else if (currentFilter == "lost") {
            filtered = filtered.filter { it.isLost }
        }

        if (searchQuery.isNotBlank()) {
            val q = searchQuery.lowercase()
            filtered = filtered.filter {
                it.title.lowercase().contains(q) ||
                it.description.lowercase().contains(q) ||
                it.location.lowercase().contains(q)
            }
        }

        adapter.updateList(filtered)
        binding.tvReportedItemsCount.text = "Reported Items (${filtered.size})"
        binding.layoutEmptyState.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        binding.rvLostFound.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun showReportDialog() {
        val dialog = BottomSheetDialog(this)
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_report_lost_found, null)
        dialog.setContentView(dialogView)

        val btnClose = dialogView.findViewById<ImageButton>(R.id.btnCloseDialog)
        val tabFound = dialogView.findViewById<TextView>(R.id.tabFound)
        val tabLost = dialogView.findViewById<TextView>(R.id.tabLost)
        val etTitle = dialogView.findViewById<EditText>(R.id.etReportTitle)
        val etLocation = dialogView.findViewById<EditText>(R.id.etReportLocation)
        val etPhone = dialogView.findViewById<EditText>(R.id.etReportPhone)
        val btnPublish = dialogView.findViewById<MaterialButton>(R.id.btnPublishReport)

        val btnSelectPhoto = dialogView.findViewById<View>(R.id.btnSelectPhoto)
        val layoutPhotoPreview = dialogView.findViewById<View>(R.id.layoutPhotoPreview)
        val ivSelectedPhotoPreview = dialogView.findViewById<ImageView>(R.id.ivSelectedPhotoPreview)
        val tvCompressedSizeBadge = dialogView.findViewById<TextView>(R.id.tvCompressedSizeBadge)
        val btnRemoveSelectedPhoto = dialogView.findViewById<ImageButton>(R.id.btnRemoveSelectedPhoto)
        val pbPhotoCompressing = dialogView.findViewById<ProgressBar>(R.id.pbPhotoCompressing)

        pendingCompressedFile = null
        pendingCompressedUri = null

        btnSelectPhoto.setOnClickListener {
            photoPicker.launch("image/*")
        }

        btnRemoveSelectedPhoto.setOnClickListener {
            pendingCompressedFile?.delete()
            pendingCompressedFile = null
            pendingCompressedUri = null
            layoutPhotoPreview.visibility = View.GONE
            btnSelectPhoto.visibility = View.VISIBLE
        }

        onPhotoPickedAction = { pickedUri ->
            pbPhotoCompressing.visibility = View.VISIBLE
            btnSelectPhoto.visibility = View.GONE
            layoutPhotoPreview.visibility = View.VISIBLE
            tvCompressedSizeBadge.text = "Compressing <100KB..."

            lifecycleScope.launch {
                val compressionResult = ImageCompressor.compressImage(
                    this@LostFoundActivity,
                    pickedUri,
                    targetSizeKb = 100
                )
                pbPhotoCompressing.visibility = View.GONE

                compressionResult.onSuccess { result ->
                    pendingCompressedFile = result.file
                    pendingCompressedUri = result.uri
                    tvCompressedSizeBadge.text = "📸 ${result.sizeKb} KB (Auto-compressed)"
                    Glide.with(this@LostFoundActivity)
                        .load(result.file)
                        .centerCrop()
                        .into(ivSelectedPhotoPreview)
                }.onFailure { err ->
                    Toast.makeText(this@LostFoundActivity, "Photo compression failed: ${err.message}", Toast.LENGTH_SHORT).show()
                    layoutPhotoPreview.visibility = View.GONE
                    btnSelectPhoto.visibility = View.VISIBLE
                    pendingCompressedFile = null
                    pendingCompressedUri = null
                }
            }
        }

        dialog.setOnDismissListener {
            onPhotoPickedAction = null
        }

        var selectedType = "found"

        fun updateTabs() {
            if (selectedType == "found") {
                tabFound.setBackgroundResource(R.drawable.bg_badge_accent_pill)
                tabFound.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#2E7D32"))
                tabFound.setTextColor(Color.WHITE)

                tabLost.background = null
                tabLost.setTextColor(Color.parseColor("#94A3B8"))
            } else {
                tabLost.setBackgroundResource(R.drawable.bg_badge_accent_pill)
                tabLost.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#B91C1C"))
                tabLost.setTextColor(Color.WHITE)

                tabFound.background = null
                tabFound.setTextColor(Color.parseColor("#94A3B8"))
            }
        }

        tabFound.setOnClickListener {
            selectedType = "found"
            updateTabs()
        }

        tabLost.setOnClickListener {
            selectedType = "lost"
            updateTabs()
        }

        btnClose.setOnClickListener { dialog.dismiss() }

        btnPublish.setOnClickListener {
            val title = etTitle.text?.toString()?.trim() ?: ""
            val location = etLocation.text?.toString()?.trim() ?: ""
            val phone = etPhone.text?.toString()?.trim() ?: ""

            if (title.isBlank()) {
                etTitle.error = "Please enter item name or description"
                return@setOnClickListener
            }

            val user = FirebaseAuth.getInstance().currentUser
            if (user == null || user.uid.isBlank()) {
                Toast.makeText(this, "Please sign in to publish a report", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            fun postItem(uploadedImageUrl: String) {
                val newItem = LostFoundItem(
                    type = selectedType,
                    title = title,
                    location = location,
                    contactPhone = phone,
                    description = title,
                    reporterName = user.displayName ?: "Student",
                    reporterId = user.uid,
                    imageUrl = uploadedImageUrl
                )

                repo.addItem(newItem,
                    onSuccess = {
                        dialog.dismiss()
                        Toast.makeText(this, "Report published successfully!", Toast.LENGTH_SHORT).show()
                        loadData()
                    },
                    onFailure = { err ->
                        btnPublish.isEnabled = true
                        btnPublish.text = "Publish Report"
                        Toast.makeText(this, "Failed to publish: ${err.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            btnPublish.isEnabled = false
            val compressedUri = pendingCompressedUri
            if (compressedUri != null) {
                btnPublish.text = "Uploading Image..."
                CloudinaryUploader.uploadImage(
                    this@LostFoundActivity,
                    compressedUri,
                    folder = "lost_found_photos",
                    onSuccess = { url, _ ->
                        postItem(url)
                    },
                    onFailure = { error ->
                        Toast.makeText(this@LostFoundActivity, "Image upload failed, publishing text only: $error", Toast.LENGTH_SHORT).show()
                        postItem("")
                    }
                )
            } else {
                btnPublish.text = "Publishing..."
                postItem("")
            }
        }

        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.behavior.skipCollapsed = true
        dialog.show()
    }

    private fun handleStatusChange(item: LostFoundItem, status: String) {
        val isGiven = status == "item_given"
        val actionTitle = if (isGiven) "Item Given" else "Item Found"
        val confirmMessage = if (isGiven) {
            "Confirm that this found item has been handed over to its owner? Superadmin and admins will be notified."
        } else {
            "Confirm that this lost item has been recovered? Superadmin and admins will be notified."
        }

        AlertDialog.Builder(this)
            .setTitle(actionTitle)
            .setMessage(confirmMessage)
            .setPositiveButton("Confirm") { _, _ ->
                repo.markItemResolved(item.id, status) {
                    val notifType = if (isGiven) "item_given" else "item_found"
                    val notifTitle = if (isGiven) "🎁 Item Given / Handed Over" else "🔍 Lost Item Recovered"
                    val notifMessage = "$currentUserName marked \"${item.title}\" as ${if (isGiven) "Given to owner" else "Found / Recovered"}."

                    notificationRepo.sendNotificationToAdmins(
                        type = notifType,
                        title = notifTitle,
                        message = notifMessage,
                        postId = item.id,
                        postType = "lost_found",
                        actorId = currentUserId,
                        actorName = currentUserName
                    )

                    Toast.makeText(this, "Marked as $actionTitle! Admins notified.", Toast.LENGTH_LONG).show()
                    allItems = allItems.map { if (it.id == item.id) it.copy(isResolved = true, resolvedStatus = status) else it }
                    applyFilters()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun handleReportPost(item: LostFoundItem) {
        if (currentUserId.isBlank()) {
            Toast.makeText(this, "Please sign in to report a post", Toast.LENGTH_SHORT).show()
            return
        }
        val reasons = arrayOf("Spam or misleading", "Inappropriate content", "Item already returned / resolved", "Other violation")
        var selectedIdx = 0

        AlertDialog.Builder(this)
            .setTitle("Report Post to Admins")
            .setSingleChoiceItems(reasons, 0) { _, which ->
                selectedIdx = which
            }
            .setPositiveButton("Submit Report") { _, _ ->
                val chosenReason = reasons[selectedIdx]
                repo.reportItem(item.id, currentUserId, chosenReason) {
                    notificationRepo.sendNotificationToAdmins(
                        type = "report",
                        title = "🚨 Post Reported: Lost & Found",
                        message = "Post \"${item.title}\" reported by $currentUserName. Reason: $chosenReason",
                        postId = item.id,
                        postType = "lost_found",
                        actorId = currentUserId,
                        actorName = currentUserName,
                        extraNote = chosenReason
                    )
                    Toast.makeText(this, "Report submitted. Superadmin & admin will review it.", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}

