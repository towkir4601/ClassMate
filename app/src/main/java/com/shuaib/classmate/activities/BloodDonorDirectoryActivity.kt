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

        // Handle pre-filtering if opened from Home alert banner
        val preFilter = intent.getStringExtra("filter_blood_group")
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

        // Pre-fill phone if current user has phone
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
                    Toast.makeText(this, "🚨 Urgent blood request posted!", Toast.LENGTH_LONG).show()
                    loadBloodRequests()
                    loadActiveEmergencyBanner()
                },
                onFailure = { err ->
                    btnSubmit.isEnabled = true
                    Toast.makeText(this, "Failed to post request: ${err.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }

        dialog.show()
    }

    private fun handleBloodDonated(req: BloodRequest) {
        AlertDialog.Builder(this)
            .setTitle("Blood Donated")
            .setMessage("Confirm that blood has been donated for this request? Superadmin and admins will be notified.")
            .setPositiveButton("Confirm Donated") { _, _ ->
                bloodRequestRepo.markBloodDonated(req.id)
                notificationRepo.sendNotificationToAdmins(
                    type = "blood_donated",
                    title = "🩸 Blood Donated / Fulfilled",
                    message = "$currentUserName marked blood request for ${req.bloodGroup} at ${req.hospitalName} as Donated!",
                    postId = req.id,
                    postType = "blood_request",
                    actorId = currentUserId,
                    actorName = currentUserName
                )
                Toast.makeText(this, "Marked as Blood Donated! Admins notified.", Toast.LENGTH_LONG).show()
                loadBloodRequests()
                loadActiveEmergencyBanner()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun handleReportBloodRequest(req: BloodRequest) {
        if (currentUserId.isBlank()) {
            Toast.makeText(this, "Please sign in to report a post", Toast.LENGTH_SHORT).show()
            return
        }
        val reasons = arrayOf("Spam or fake emergency", "Already fulfilled / donated", "Incorrect contact information", "Other issue")
        var selectedIdx = 0

        AlertDialog.Builder(this)
            .setTitle("Report Blood Request to Admins")
            .setSingleChoiceItems(reasons, 0) { _, which ->
                selectedIdx = which
            }
            .setPositiveButton("Submit Report") { _, _ ->
                val chosenReason = reasons[selectedIdx]
                bloodRequestRepo.reportRequest(req.id, currentUserId, chosenReason) {
                    notificationRepo.sendNotificationToAdmins(
                        type = "report",
                        title = "🚨 Post Reported: Blood Request",
                        message = "Blood request for ${req.bloodGroup} at ${req.hospitalName} reported by $currentUserName. Reason: $chosenReason",
                        postId = req.id,
                        postType = "blood_request",
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

    private fun loadBloodRequests() {
        bloodRequestRepo.getAllActiveRequests { list ->
            if (isFinishing || isDestroyed) return@getAllActiveRequests
            allRequests = list
            applyFilters()
        }
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
                    switchTab("requests")
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
                    applyFilters()
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
