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
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.shuaib.classmate.R
import com.shuaib.classmate.adapters.BloodDonorAdapter
import com.shuaib.classmate.adapters.BloodRequestAdapter
import com.shuaib.classmate.databinding.ActivityBloodDonorDirectoryBinding
import com.shuaib.classmate.models.BloodRequest
import com.shuaib.classmate.models.User
import com.shuaib.classmate.repositories.AppNotificationRepository
import com.shuaib.classmate.repositories.BloodRequestRepository
import com.shuaib.classmate.utils.AppPreferences

class BloodDonorDirectoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBloodDonorDirectoryBinding
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val bloodRequestRepo = BloodRequestRepository()
    private val notificationRepo = AppNotificationRepository()

    private var allDonors = listOf<User>()
    private var allRequests = listOf<BloodRequest>()
    private var currentTab = "requests" // "requests" or "donors"
    private var currentFilterBloodGroup = "All"
    private var searchQuery = ""
    private var donorListener: ListenerRegistration? = null

    private lateinit var donorAdapter: BloodDonorAdapter
    private lateinit var requestAdapter: BloodRequestAdapter

    private var currentUserId: String = ""
    private var currentUserName: String = "Student"
    private val bloodGroups = arrayOf("A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBloodDonorDirectoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val currentUser = auth.currentUser
        currentUserId = currentUser?.uid ?: ""
        currentUserName = currentUser?.displayName ?: AppPreferences(this).getUserName().ifBlank { "Student" }

        binding.toolbar.setNavigationOnClickListener { finish() }

        setupAdapters()
        setupTabs()
        setupFilterChips()
        setupSearch()
        setupActionButtons()

        // Handle pre-filtering if opened from Home alert banner or shortcuts
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
            switchTab("requests")
        } else {
            switchTab("donors")
        }

        binding.swipeRefresh.setOnRefreshListener {
            loadBloodRequests()
            loadDonors()
            loadActiveEmergencyBanner()
        }

        loadBloodRequests()
        loadDonors()
        loadActiveEmergencyBanner()
    }

    private fun setupAdapters() {
        donorAdapter = BloodDonorAdapter(emptyList())
        binding.rvDonors.layoutManager = LinearLayoutManager(this)
        binding.rvDonors.adapter = donorAdapter

        requestAdapter = BloodRequestAdapter(
            requests = emptyList(),
            currentUserId = currentUserId,
            currentUserName = currentUserName,
            onBloodDonatedClick = { req -> handleBloodDonated(req) },
            onReportClick = { req -> handleReportBloodRequest(req) }
        )
        binding.rvBloodRequests.layoutManager = LinearLayoutManager(this)
        binding.rvBloodRequests.adapter = requestAdapter
    }

    private fun setupTabs() {
        binding.tabBloodRequests.setOnClickListener {
            switchTab("requests")
        }
        binding.tabDonors.setOnClickListener {
            switchTab("donors")
        }
        switchTab("requests")
    }

    private fun switchTab(tab: String) {
        currentTab = tab
        if (tab == "requests") {
            binding.tabBloodRequests.setBackgroundResource(R.drawable.bg_badge_accent_pill)
            binding.tabBloodRequests.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#7F1D1D"))
            binding.tabBloodRequests.setTextColor(Color.WHITE)

            binding.tabDonors.background = null
            binding.tabDonors.setTextColor(Color.parseColor("#94A3B8"))

            binding.rvBloodRequests.visibility = View.VISIBLE
            binding.rvDonors.visibility = View.GONE
            binding.scrollBloodChips.visibility = View.GONE
        } else {
            binding.tabDonors.setBackgroundResource(R.drawable.bg_badge_accent_pill)
            binding.tabDonors.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#374151"))
            binding.tabDonors.setTextColor(Color.WHITE)

            binding.tabBloodRequests.background = null
            binding.tabBloodRequests.setTextColor(Color.parseColor("#94A3B8"))

            binding.rvDonors.visibility = View.VISIBLE
            binding.rvBloodRequests.visibility = View.GONE
            binding.scrollBloodChips.visibility = View.VISIBLE
        }
        applyFilters()
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
            applyFilters()
        }
    }

    private fun setupSearch() {
        binding.etSearchDonor.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString()?.trim()?.lowercase() ?: ""
                applyFilters()
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
    }

    private fun loadDonors() {
        binding.progressBar.visibility = View.VISIBLE
        donorListener?.remove()

        donorListener = firestore.collection("users")
            .whereNotEqualTo("bloodGroup", "")
            .addSnapshotListener { snapshot, error ->
                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false

                if (error != null) {
                    Toast.makeText(this, "Failed to load donors: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    allDonors = snapshot.documents.mapNotNull { it.toObject(User::class.java) }
                    applyFilters()
                }
            }
    }

    private fun loadBloodRequests() {
        bloodRequestRepo.getAllActiveRequests { requests ->
            if (isFinishing || isDestroyed) return@getAllActiveRequests
            binding.swipeRefresh.isRefreshing = false
            allRequests = requests
            applyFilters()
        }
    }

    private fun handleBloodDonated(req: BloodRequest) {
        AlertDialog.Builder(this)
            .setTitle("Confirm Blood Donation")
            .setMessage("Did you donate blood for this request? The requester will be notified of your noble contribution.")
            .setPositiveButton("Yes, I Donated") { _, _ ->
                bloodRequestRepo.markAsDonated(req.id, currentUserId) { success ->
                    if (success) {
                        Toast.makeText(this, "Thank you for saving a life! ❤️", Toast.LENGTH_LONG).show()

                        if (currentUserId.isNotBlank()) {
                            notificationRepo.sendNotificationToAuthor(
                                authorId = req.requesterId,
                                type = "blood_donated",
                                title = "🩸 Blood Donated!",
                                message = "$currentUserName has stepped up and donated blood for your request.",
                                postId = req.id,
                                postType = "blood_request",
                                actorId = currentUserId,
                                actorName = currentUserName
                            )
                        }
                        loadBloodRequests()
                    } else {
                        Toast.makeText(this, "Failed to update donation status", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun handleReportBloodRequest(req: BloodRequest) {
        AlertDialog.Builder(this)
            .setTitle("Report Post")
            .setMessage("Do you want to report this blood request as misleading or inappropriate?")
            .setPositiveButton("Report") { _, _ ->
                bloodRequestRepo.reportRequest(req.id, currentUserId) { success ->
                    if (success) {
                        Toast.makeText(this, "Thank you, report submitted to moderators.", Toast.LENGTH_SHORT).show()
                        loadBloodRequests()
                    } else {
                        Toast.makeText(this, "Failed to submit report", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCreateBloodRequestDialog() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_create_blood_request, null)
        dialog.setContentView(view)

        val etHospital = view.findViewById<TextInputEditText>(R.id.etBloodHospital)
        val etPatient = view.findViewById<TextInputEditText>(R.id.etBloodPatient)
        val etUrgency = view.findViewById<TextInputEditText>(R.id.etBloodUrgencyNote)
        val etPhone = view.findViewById<TextInputEditText>(R.id.etBloodContactPhone)
        val etBags = view.findViewById<TextInputEditText>(R.id.etBloodBagsNeeded)
        val btnSubmit = view.findViewById<View>(R.id.btnSubmitBloodRequest)
        val chipGroup = view.findViewById<com.google.android.material.chip.ChipGroup>(R.id.chipGroupRequestBlood)

        // Pre-fill phone if available in preferences
        val userPhone = AppPreferences(this).getUserBatch() // fallback
        if (userPhone.isNotBlank() && etPhone.text.isNullOrBlank()) {
            etPhone.setText(userPhone)
        }

        btnSubmit.setOnClickListener {
            val hospital = etHospital.text.toString().trim()
            val patient = etPatient.text.toString().trim()
            val urgency = etUrgency.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val bagsStr = etBags.text.toString().trim()
            val bags = bagsStr.toIntOrNull() ?: 1

            var selectedGroup = ""
            when (chipGroup.checkedChipId) {
                R.id.chipReqAPos -> selectedGroup = "A+"
                R.id.chipReqANeg -> selectedGroup = "A-"
                R.id.chipReqBPos -> selectedGroup = "B+"
                R.id.chipReqBNeg -> selectedGroup = "B-"
                R.id.chipReqOPos -> selectedGroup = "O+"
                R.id.chipReqONeg -> selectedGroup = "O-"
                R.id.chipReqAbPos -> selectedGroup = "AB+"
                R.id.chipReqAbNeg -> selectedGroup = "AB-"
            }

            if (selectedGroup.isBlank()) {
                Toast.makeText(this, "Please select the required blood group", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (hospital.isBlank()) {
                etHospital.error = "Hospital / Location is required"
                return@setOnClickListener
            }

            if (phone.isBlank()) {
                etPhone.error = "Contact phone number is required"
                return@setOnClickListener
            }

            val request = BloodRequest(
                bloodGroup = selectedGroup,
                hospitalName = hospital,
                patientName = patient,
                urgencyNote = urgency,
                contactPhone = phone,
                bagsNeeded = bags,
                requesterId = currentUserId,
                requesterName = currentUserName
            )

            bloodRequestRepo.createRequest(
                request,
                onSuccess = {
                    dialog.dismiss()
                    Toast.makeText(this, "🚨 Urgent blood request posted! Live across GSTU Go.", Toast.LENGTH_LONG).show()
                    loadBloodRequests()
                    switchTab("requests")
                },
                onFailure = { err ->
                    Toast.makeText(this, "Failed: ${err.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }

        dialog.show()
    }

    private fun showUpdateBloodGroupDialog() {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "Please log in to update your blood group", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Select Your Blood Group")
            .setItems(bloodGroups) { _, which ->
                val selected = bloodGroups[which]
                updateUserBloodGroup(user.uid, selected)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateUserBloodGroup(uid: String, bloodGroup: String) {
        firestore.collection("users").document(uid)
            .update("bloodGroup", bloodGroup)
            .addOnSuccessListener {
                Toast.makeText(this, "Blood group updated to $bloodGroup", Toast.LENGTH_SHORT).show()
                loadDonors()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to update: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadActiveEmergencyBanner() {
        bloodRequestRepo.getActiveRequests(1) { requests ->
            if (isFinishing || isDestroyed) return@getActiveRequests
            if (requests.isNotEmpty()) {
                val topReq = requests.first()
                binding.cardActiveEmergencyBanner.visibility = View.VISIBLE
                binding.tvBannerBloodGroup.text = topReq.bloodGroup
                binding.tvBannerTitle.text = "🚨 URGENT: ${topReq.bloodGroup} Needed"
                binding.tvBannerSubtitle.text = "${topReq.hospitalName} · ${topReq.contactPhone}"

                binding.btnBannerCall.setOnClickListener {
                    val phone = topReq.contactPhone.filter { it.isDigit() || it == '+' }
                    if (phone.length >= 5) {
                        val intent = android.content.Intent(android.content.Intent.ACTION_DIAL, android.net.Uri.parse("tel:$phone"))
                        startActivity(intent)
                    } else {
                        Toast.makeText(this, "Contact: ${topReq.contactPhone}", Toast.LENGTH_SHORT).show()
                    }
                }

                binding.cardActiveEmergencyBanner.setOnClickListener {
                    switchTab("requests")
                }
            } else {
                binding.cardActiveEmergencyBanner.visibility = View.GONE
            }
        }
    }

    private fun applyFilters() {
        if (currentTab == "requests") {
            val filtered = allRequests.filter { req ->
                if (searchQuery.isBlank()) true
                else {
                    req.bloodGroup.lowercase().contains(searchQuery) ||
                    req.hospitalName.lowercase().contains(searchQuery) ||
                    req.urgencyNote.lowercase().contains(searchQuery) ||
                    req.requesterName.lowercase().contains(searchQuery)
                }
            }
            requestAdapter.updateList(filtered)
            if (filtered.isEmpty()) {
                binding.emptyState.visibility = View.VISIBLE
                binding.tvEmptyTitle.text = "No Blood Requests Found"
                binding.tvEmptySubtitle.text = "No emergency requests matching your search. You can post an urgent request below."
                binding.rvBloodRequests.visibility = View.GONE
            } else {
                binding.emptyState.visibility = View.GONE
                binding.rvBloodRequests.visibility = View.VISIBLE
            }
        } else {
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
                binding.tvEmptyTitle.text = "No Donors Found"
                binding.tvEmptySubtitle.text = "Try selecting another blood group or update your blood group."
                binding.rvDonors.visibility = View.GONE
            } else {
                binding.emptyState.visibility = View.GONE
                binding.rvDonors.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        donorListener?.remove()
    }
}
