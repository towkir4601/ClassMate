package com.shuaib.classmate.activities

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.shuaib.classmate.R
import com.shuaib.classmate.adapters.BloodDonorAdapter
import com.shuaib.classmate.adapters.BloodRequestAdapter
import com.shuaib.classmate.databinding.ActivityBloodDonorDirectoryBinding
import com.shuaib.classmate.models.BloodRequest
import com.shuaib.classmate.models.User
import com.shuaib.classmate.repositories.BloodRequestRepository

class BloodDonorDirectoryActivity : AppCompatActivity() {

    enum class BloodTab { DONORS, REQUESTS }

    private lateinit var binding: ActivityBloodDonorDirectoryBinding
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var allDonors = listOf<User>()
    private var allRequests = listOf<BloodRequest>()
    private var currentFilterBloodGroup = "All"
    private var searchQuery = ""
    private var donorListener: ListenerRegistration? = null
    private lateinit var donorAdapter: BloodDonorAdapter
    private lateinit var requestAdapter: BloodRequestAdapter

    private var currentTab = BloodTab.DONORS
    private var currentUserId: String = ""
    private var isAdmin: Boolean = false

    private val bloodGroups = arrayOf("A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-")
    private val bloodRequestRepo = BloodRequestRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBloodDonorDirectoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentUserId = auth.currentUser?.uid ?: ""

        binding.toolbar.setNavigationOnClickListener { finish() }

        // Setup Donors Adapter
        donorAdapter = BloodDonorAdapter(emptyList())
        binding.rvDonors.layoutManager = LinearLayoutManager(this)
        binding.rvDonors.adapter = donorAdapter

        // Setup Requests Adapter
        requestAdapter = BloodRequestAdapter(
            requests = emptyList(),
            currentUserId = currentUserId,
            isAdmin = isAdmin,
            onFulfillClick = { req -> confirmFulfillRequest(req) },
            onDeleteClick = { req -> confirmDeleteRequest(req) }
        )
        binding.rvBloodRequests.layoutManager = LinearLayoutManager(this)
        binding.rvBloodRequests.adapter = requestAdapter

        setupTabs()
        setupFilterChips()
        setupSearch()
        setupActionButtons()
        verifyAdminStatus()

        // Handle pre-filtering if opened from Home alert banner
        val preFilter = intent.getStringExtra("filter_blood_group")
        val openRequestsTab = intent.getBooleanExtra("open_requests_tab", false)

        if (!preFilter.isNullOrBlank() && preFilter != "All") {
            currentFilterBloodGroup = preFilter
            when (preFilter) {
                "A+" -> binding.chipAPos.isChecked = true
                "A-" -> binding.chipANeg.isChecked = true
                "B+" -> binding.chipBPos.isChecked = true
                "B-" -> binding.chipBNeg.isChecked = true
                "O+" -> binding.chipOPos.isChecked = true
                "O-" -> binding.chipONeg.isChecked = true
                "AB+" -> binding.chipAbPos.isChecked = true
                "AB-" -> binding.chipAbNeg.isChecked = true
            }
        }

        if (openRequestsTab) {
            switchTab(BloodTab.REQUESTS)
        } else {
            switchTab(BloodTab.DONORS)
        }

        binding.swipeRefresh.setOnRefreshListener {
            loadDonors()
            loadBloodRequests()
            loadActiveEmergencyBanner()
        }

        loadDonors()
        loadBloodRequests()
        loadActiveEmergencyBanner()
    }

    private fun setupTabs() {
        binding.tabDonors.setOnClickListener { switchTab(BloodTab.DONORS) }
        binding.tabRequests.setOnClickListener { switchTab(BloodTab.REQUESTS) }
    }

    private fun switchTab(tab: BloodTab) {
        currentTab = tab

        val selectedBg = R.drawable.bg_badge_accent_pill
        val selectedTint = ColorStateList.valueOf(Color.parseColor("#B91C1C"))
        val selectedTextColor = Color.WHITE
        val unselectedTextColor = Color.parseColor("#94A3B8")

        if (tab == BloodTab.DONORS) {
            // Tab Header Highlight
            binding.tabDonors.setBackgroundResource(selectedBg)
            binding.tabDonors.backgroundTintList = selectedTint
            binding.tabDonors.setTextColor(selectedTextColor)

            binding.tabRequests.background = null
            binding.tabRequests.setTextColor(unselectedTextColor)

            // Section visibility
            binding.containerDonorFilters.visibility = View.VISIBLE
            binding.containerRequestsHeader.visibility = View.GONE

            binding.rvBloodRequests.visibility = View.GONE
            binding.emptyRequestsState.visibility = View.GONE

            binding.layoutBottomDonors.visibility = View.VISIBLE
            binding.layoutBottomRequests.visibility = View.GONE

            applyDonorFilters()
        } else {
            // Tab Header Highlight
            binding.tabRequests.setBackgroundResource(selectedBg)
            binding.tabRequests.backgroundTintList = selectedTint
            binding.tabRequests.setTextColor(selectedTextColor)

            binding.tabDonors.background = null
            binding.tabDonors.setTextColor(unselectedTextColor)

            // Section visibility
            binding.containerDonorFilters.visibility = View.GONE
            binding.containerRequestsHeader.visibility = View.VISIBLE

            binding.rvDonors.visibility = View.GONE
            binding.emptyState.visibility = View.GONE

            binding.layoutBottomDonors.visibility = View.GONE
            binding.layoutBottomRequests.visibility = View.VISIBLE

            applyRequestFilters()
        }
    }

    private fun verifyAdminStatus() {
        if (currentUserId.isBlank()) return
        firestore.collection("users").document(currentUserId).get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    val role = doc.getString("role") ?: ""
                    val isUserAdmin = role.equals("admin", ignoreCase = true) || role.equals("superadmin", ignoreCase = true)
                    if (isUserAdmin != isAdmin) {
                        isAdmin = isUserAdmin
                        requestAdapter.setUserAndRole(currentUserId, isAdmin)
                    }
                }
            }
    }

    private fun setupFilterChips() {
        binding.chipGroupBlood.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull() ?: R.id.chipAll
            currentFilterBloodGroup = when (checkedId) {
                R.id.chipAPos -> "A+"
                R.id.chipANeg -> "A-"
                R.id.chipBPos -> "B+"
                R.id.chipBNeg -> "B-"
                R.id.chipOPos -> "O+"
                R.id.chipONeg -> "O-"
                R.id.chipAbPos -> "AB+"
                R.id.chipAbNeg -> "AB-"
                else -> "All"
            }
            applyDonorFilters()
        }
    }

    private fun setupSearch() {
        binding.etSearchDonor.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString()?.trim()?.lowercase() ?: ""
                applyDonorFilters()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupActionButtons() {
        binding.btnUpdateMyBlood.setOnClickListener {
            showUpdateBloodGroupDialog()
        }

        binding.btnRequestBlood.setOnClickListener {
            showCreateBloodRequestDialog()
        }

        binding.btnPostBloodRequest.setOnClickListener {
            showCreateBloodRequestDialog()
        }

        binding.btnHeaderNewPost.setOnClickListener {
            showCreateBloodRequestDialog()
        }
    }

    private fun showUpdateBloodGroupDialog() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "Please log in to update your blood group", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Select Your Blood Group")
            .setItems(bloodGroups) { _, which ->
                val selected = bloodGroups[which]
                firestore.collection("users").document(uid)
                    .update("bloodGroup", selected)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Blood group updated to $selected!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to update: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCreateBloodRequestDialog() {
        val user = auth.currentUser
        if (user == null || user.uid.isBlank()) {
            Toast.makeText(this, "Please log in to post a blood request", Toast.LENGTH_SHORT).show()
            return
        }

        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_blood_request, null)
        dialog.setContentView(dialogView)

        val btnClose = dialogView.findViewById<android.widget.ImageButton>(R.id.btnCloseBloodDialog)
        val chipGroup = dialogView.findViewById<com.google.android.material.chip.ChipGroup>(R.id.chipGroupRequestBlood)
        val etHospital = dialogView.findViewById<android.widget.EditText>(R.id.etReqHospital)
        val etBags = dialogView.findViewById<android.widget.EditText>(R.id.etReqBags)
        val etPhone = dialogView.findViewById<android.widget.EditText>(R.id.etReqPhone)
        val etNote = dialogView.findViewById<android.widget.EditText>(R.id.etReqNote)
        val btnSubmit = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSubmitBloodRequest)

        btnClose.setOnClickListener { dialog.dismiss() }

        user.phoneNumber?.let {
            if (it.isNotBlank()) etPhone.setText(it)
        }

        btnSubmit.setOnClickListener {
            val hospital = etHospital.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val bagsStr = etBags.text.toString().trim()
            val note = etNote.text.toString().trim()

            if (hospital.isBlank()) {
                etHospital.error = "Please enter hospital or location"
                return@setOnClickListener
            }

            if (phone.isBlank()) {
                etPhone.error = "Please enter contact phone number"
                return@setOnClickListener
            }

            val selectedChipId = chipGroup.checkedChipId
            val selectedBlood = when (selectedChipId) {
                R.id.chipReqAPos -> "A+"
                R.id.chipReqANeg -> "A-"
                R.id.chipReqBPos -> "B+"
                R.id.chipReqBNeg -> "B-"
                R.id.chipReqOPos -> "O+"
                R.id.chipReqONeg -> "O-"
                R.id.chipReqAbPos -> "AB+"
                R.id.chipReqAbNeg -> "AB-"
                else -> "B+"
            }

            val bags = bagsStr.toIntOrNull() ?: 1

            btnSubmit.isEnabled = false

            val request = BloodRequest(
                bloodGroup = selectedBlood,
                hospitalName = hospital,
                contactPhone = phone,
                bagsNeeded = bags,
                urgencyNote = note,
                requesterId = user.uid,
                requesterName = user.displayName ?: "Student"
            )

            bloodRequestRepo.createRequest(
                request,
                onSuccess = {
                    dialog.dismiss()
                    Toast.makeText(this, "🚨 Urgent blood request posted! Live across GSTU Go.", Toast.LENGTH_LONG).show()
                    loadBloodRequests()
                    loadActiveEmergencyBanner()
                    switchTab(BloodTab.REQUESTS)
                },
                onFailure = { err ->
                    btnSubmit.isEnabled = true
                    Toast.makeText(this, "Failed to post request: ${err.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }

        dialog.show()
    }

    private fun loadActiveEmergencyBanner() {
        bloodRequestRepo.getActiveRequests(1) { requests ->
            if (isFinishing || isDestroyed) return@getActiveRequests
            val topReq = requests.firstOrNull()
            if (topReq != null) {
                binding.cardActiveEmergencyBanner.visibility = View.VISIBLE
                val bagCountText = if (topReq.bagsNeeded > 1) " (${topReq.bagsNeeded} bags)" else ""
                binding.tvActiveEmergencyTitle.text = "🚨 Urgent ${topReq.bloodGroup} Blood Needed$bagCountText"
                val extraNote = if (topReq.urgencyNote.isNotBlank()) " · ${topReq.urgencyNote}" else ""
                binding.tvActiveEmergencySubtitle.text = "${topReq.hospitalName}$extraNote · Contact: ${topReq.contactPhone}"

                binding.btnCallActiveEmergency.setOnClickListener {
                    val digits = topReq.contactPhone.filter { ch -> ch.isDigit() || ch == '+' }
                    if (digits.length >= 5) {
                        val dialIntent = android.content.Intent(android.content.Intent.ACTION_DIAL, android.net.Uri.parse("tel:$digits"))
                        startActivity(dialIntent)
                    } else {
                        Toast.makeText(this, "Contact: ${topReq.contactPhone}", Toast.LENGTH_SHORT).show()
                    }
                }

                binding.cardActiveEmergencyBanner.setOnClickListener {
                    // Tap banner to switch to requests tab or filter
                    switchTab(BloodTab.REQUESTS)
                }
            } else {
                binding.cardActiveEmergencyBanner.visibility = View.GONE
            }
        }
    }

    private fun loadDonors() {
        binding.progressBar.visibility = View.VISIBLE
        donorListener?.remove()

        donorListener = firestore.collection("users")
            .addSnapshotListener { snapshot, error ->
                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false

                if (error != null) {
                    Toast.makeText(this, "Failed to load donors", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    allDonors = snapshot.documents.mapNotNull { doc ->
                        val u = doc.toObject(User::class.java)?.copy(uid = doc.id)
                        if (u != null && u.bloodGroup.isNotBlank() && u.bloodGroup != "Unknown") {
                            u
                        } else {
                            null
                        }
                    }
                    if (currentTab == BloodTab.DONORS) {
                        applyDonorFilters()
                    }
                }
            }
    }

    private fun loadBloodRequests() {
        bloodRequestRepo.getAllActiveRequests { requests ->
            if (isFinishing || isDestroyed) return@getAllActiveRequests
            binding.swipeRefresh.isRefreshing = false
            allRequests = requests
            binding.tvRequestsCount.text = "Urgent Blood Requests (${requests.size})"
            if (currentTab == BloodTab.REQUESTS) {
                applyRequestFilters()
            }
        }
    }

    private fun applyDonorFilters() {
        val filtered = allDonors.filter { user ->
            val matchesBlood = if (currentFilterBloodGroup == "All") {
                true
            } else {
                user.bloodGroup.equals(currentFilterBloodGroup, ignoreCase = true)
            }

            val matchesSearch = if (searchQuery.isBlank()) {
                true
            } else {
                user.name.lowercase().contains(searchQuery) ||
                user.fullName.lowercase().contains(searchQuery) ||
                user.department.lowercase().contains(searchQuery) ||
                user.batch.lowercase().contains(searchQuery) ||
                user.homeDistrict.lowercase().contains(searchQuery)
            }

            matchesBlood && matchesSearch
        }

        donorAdapter.updateList(filtered)

        if (filtered.isEmpty()) {
            binding.emptyState.visibility = View.VISIBLE
            binding.rvDonors.visibility = View.GONE
        } else {
            binding.emptyState.visibility = View.GONE
            binding.rvDonors.visibility = View.VISIBLE
        }
    }

    private fun applyRequestFilters() {
        requestAdapter.updateList(allRequests)

        if (allRequests.isEmpty()) {
            binding.emptyRequestsState.visibility = View.VISIBLE
            binding.rvBloodRequests.visibility = View.GONE
        } else {
            binding.emptyRequestsState.visibility = View.GONE
            binding.rvBloodRequests.visibility = View.VISIBLE
        }
    }

    private fun confirmFulfillRequest(req: BloodRequest) {
        AlertDialog.Builder(this)
            .setTitle("Mark as Fulfilled")
            .setMessage("Was a blood donor found for ${req.bloodGroup} blood at ${req.hospitalName}? Marking as fulfilled will remove this request from the active board.")
            .setPositiveButton("Yes, Blood Found") { _, _ ->
                allRequests = allRequests.filter { it.id != req.id }
                applyRequestFilters()
                bloodRequestRepo.markFulfilled(
                    req.id,
                    onSuccess = {
                        Toast.makeText(this, "Request marked as fulfilled!", Toast.LENGTH_SHORT).show()
                        loadBloodRequests()
                        loadActiveEmergencyBanner()
                    },
                    onFailure = {
                        Toast.makeText(this, "Request fulfilled.", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDeleteRequest(req: BloodRequest) {
        AlertDialog.Builder(this)
            .setTitle("Delete Blood Request")
            .setMessage("Do you want to permanently delete this blood request?")
            .setPositiveButton("Delete") { _, _ ->
                allRequests = allRequests.filter { it.id != req.id }
                applyRequestFilters()
                bloodRequestRepo.deleteRequest(
                    req.id,
                    onSuccess = {
                        Toast.makeText(this, "Request deleted successfully.", Toast.LENGTH_SHORT).show()
                        loadBloodRequests()
                        loadActiveEmergencyBanner()
                    },
                    onFailure = {
                        Toast.makeText(this, "Request removed.", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        donorListener?.remove()
    }
}
