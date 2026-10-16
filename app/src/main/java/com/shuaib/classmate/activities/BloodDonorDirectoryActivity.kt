package com.shuaib.classmate.activities

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
import com.shuaib.classmate.databinding.ActivityBloodDonorDirectoryBinding
import com.shuaib.classmate.models.BloodRequest
import com.shuaib.classmate.models.User
import com.shuaib.classmate.repositories.BloodRequestRepository

class BloodDonorDirectoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBloodDonorDirectoryBinding
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var allDonors = listOf<User>()
    private var currentFilterBloodGroup = "All"
    private var searchQuery = ""
    private var donorListener: ListenerRegistration? = null
    private lateinit var adapter: BloodDonorAdapter

    private val bloodGroups = arrayOf("A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-")
    private val bloodRequestRepo = BloodRequestRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBloodDonorDirectoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        adapter = BloodDonorAdapter(emptyList())
        binding.rvDonors.layoutManager = LinearLayoutManager(this)
        binding.rvDonors.adapter = adapter

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
            loadDonors()
            loadActiveEmergencyBanner()
        }

        loadDonors()
        loadActiveEmergencyBanner()
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

            val request = com.shuaib.classmate.models.BloodRequest(
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
                    Toast.makeText(this, "🚨 Urgent blood request posted! Live on Campus Portal Home.", Toast.LENGTH_LONG).show()
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
                    // Tap banner to filter donors for this requested blood group
                    when (topReq.bloodGroup) {
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

        adapter.updateList(filtered)

        if (filtered.isEmpty()) {
            binding.emptyState.visibility = View.VISIBLE
            binding.rvDonors.visibility = View.GONE
        } else {
            binding.emptyState.visibility = View.GONE
            binding.rvDonors.visibility = View.VISIBLE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        donorListener?.remove()
    }
}
