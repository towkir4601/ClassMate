package com.shuaib.classmate.activities

import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
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
import com.shuaib.classmate.adapters.CampusClubAdapter
import com.shuaib.classmate.adapters.CampusEventAdapter
import com.shuaib.classmate.adapters.ClubMembershipRequestAdapter
import com.shuaib.classmate.adapters.EventRegistrationAdapter
import com.shuaib.classmate.databinding.ActivityEventsClubsBinding
import com.shuaib.classmate.models.CampusClub
import com.shuaib.classmate.models.CampusEvent
import com.shuaib.classmate.models.ClubMembershipRequest
import com.shuaib.classmate.models.EventRegistration
import com.shuaib.classmate.repositories.EventsClubsRepository
import com.shuaib.classmate.utils.AppPreferences
import com.shuaib.classmate.utils.applyComposerBounceAnimation
import com.shuaib.classmate.utils.applyFloatingPostAnimation
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class EventsClubsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEventsClubsBinding
    private val repo = EventsClubsRepository()

    private var currentTab = "events" // "events", "clubs", "requests"
    private var currentRequestsSubTab = "club" // "club" or "event"

    private lateinit var eventAdapter: CampusEventAdapter
    private lateinit var clubAdapter: CampusClubAdapter
    private lateinit var eventRegAdapter: EventRegistrationAdapter
    private lateinit var clubMembershipAdapter: ClubMembershipRequestAdapter

    private var currentUserId: String = ""
    private var isSuperAdmin: Boolean = false
    private var currentUserName: String = ""
    private var currentUserDept: String = ""
    private var currentUserStudentId: String = ""
    private var currentUserEmail: String = ""
    private var currentUserPhone: String = ""

    private var allEvents = mutableListOf<CampusEvent>()
    private var allClubs = mutableListOf<CampusClub>()
    private var allEventRegistrations = mutableListOf<EventRegistration>()
    private var allClubMembershipRequests = mutableListOf<ClubMembershipRequest>()
    private var userEventRegistrations = mutableListOf<EventRegistration>()
    private var userClubRequests = mutableListOf<ClubMembershipRequest>()
    private var executiveClubIds = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEventsClubsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUserId = currentUser?.uid ?: ""
        currentUserEmail = currentUser?.email ?: ""
        currentUserName = currentUser?.displayName ?: ""

        val savedRole = AppPreferences(this).getDemoRole().ifBlank { AppPreferences(this).getUserRole() }
        isSuperAdmin = savedRole.equals("superadmin", ignoreCase = true)

        setupUI()
        fetchUserProfile()
        loadData()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { finish() }

        eventAdapter = CampusEventAdapter(
            items = emptyList(),
            currentUserId = currentUserId,
            isSuperAdmin = isSuperAdmin,
            executiveClubIds = executiveClubIds,
            onActionClick = { event -> handleEventAction(event) },
            onDeleteClick = { event -> confirmDeleteEvent(event) }
        )

        clubAdapter = CampusClubAdapter(
            items = emptyList(),
            currentUserId = currentUserId,
            isSuperAdmin = isSuperAdmin,
            onClubClick = { club -> handleClubCardClick(club) },
            onDeleteClick = { club -> confirmDeleteClub(club) }
        )

        eventRegAdapter = EventRegistrationAdapter(
            items = emptyList(),
            onConfirmClick = { reg -> confirmEventRegistration(reg) }
        )

        clubMembershipAdapter = ClubMembershipRequestAdapter(
            items = emptyList(),
            onApproveClick = { req -> approveClubMembership(req) }
        )

        binding.rvEventsClubs.layoutManager = LinearLayoutManager(this)
        binding.rvEventsClubs.adapter = eventAdapter

        binding.tabEvents.setOnClickListener { selectTab("events") }
        binding.tabClubs.setOnClickListener { selectTab("clubs") }
        binding.tabRequests.setOnClickListener { selectTab("requests") }

        binding.pillSubClubRequests.setOnClickListener { selectRequestsSubTab("club") }
        binding.pillSubEventRequests.setOnClickListener { selectRequestsSubTab("event") }

        binding.btnAddNew.setOnClickListener { view ->
            view.applyComposerBounceAnimation {
                if (currentTab == "events") {
                    showCreateEventDialog()
                } else if (currentTab == "clubs") {
                    showCreateClubDialog()
                }
            }
        }

        binding.swipeRefreshEventsClubs.setOnRefreshListener {
            loadData()
        }
    }

    private fun fetchUserProfile() {
        if (currentUserId.isBlank()) return
        FirebaseFirestore.getInstance().collection("users").document(currentUserId).get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    val role = doc.getString("role") ?: ""
                    val localDemo = AppPreferences(this).getDemoRole()
                    val firestoreDemo = doc.getString("demoRole") ?: ""
                    val effectiveRole = localDemo.ifBlank { firestoreDemo }.ifBlank { role }
                    isSuperAdmin = effectiveRole.equals("superadmin", ignoreCase = true)
                    val name = doc.getString("fullName") ?: doc.getString("name") ?: ""
                    if (name.isNotBlank()) currentUserName = name
                    currentUserDept = doc.getString("department") ?: ""
                    currentUserStudentId = doc.getString("studentId") ?: ""
                    currentUserPhone = doc.getString("phone") ?: ""
                    val email = doc.getString("email") ?: ""
                    if (email.isNotBlank()) currentUserEmail = email

                    updateRoleIndicator()
                    updateActionButtons()
                }
            }
    }

    private fun updateRoleIndicator() {
        if (isSuperAdmin) {
            binding.tvEventsRoleIndicator.text = "Role: Superadmin"
            binding.tvEventsRoleIndicator.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#4C1D95"))
            binding.tvEventsRoleIndicator.setTextColor(Color.parseColor("#E9D5FF"))
        } else if (executiveClubIds.isNotEmpty()) {
            binding.tvEventsRoleIndicator.text = "Role: Club Executive"
            binding.tvEventsRoleIndicator.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#14532D"))
            binding.tvEventsRoleIndicator.setTextColor(Color.parseColor("#BBF7D0"))
        } else {
            binding.tvEventsRoleIndicator.text = "Role: Student"
            binding.tvEventsRoleIndicator.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#1E293B"))
            binding.tvEventsRoleIndicator.setTextColor(Color.parseColor("#94A3B8"))
        }
    }

    private fun loadData() {
        binding.swipeRefreshEventsClubs.isRefreshing = true

        // 1. Load clubs
        repo.getClubs(isSuperAdmin) { clubs ->
            allClubs = clubs.toMutableList()

            // Calculate executive clubs (Superadmin is president of all clubs)
            executiveClubIds = if (isSuperAdmin) {
                allClubs.map { it.id }.toMutableList()
            } else {
                allClubs.filter { it.isExecutive(currentUserId) }.map { it.id }.toMutableList()
            }
            updateRoleIndicator()

            // 2. Load user's club membership requests
            repo.getUserClubRequests(currentUserId) { clubReqs ->
                userClubRequests = clubReqs.toMutableList()

                // 3. Load user's event registrations
                repo.getUserRegistrations(currentUserId) { uRegs ->
                    userEventRegistrations = uRegs.toMutableList()

                    // 4. Load events
                    repo.getEvents(isSuperAdmin) { events ->
                        binding.swipeRefreshEventsClubs.isRefreshing = false

                        allEvents = events.map { event ->
                            val matchedReg = userEventRegistrations.firstOrNull { it.eventId == event.id }
                            if (matchedReg != null) {
                                event.copy(isRegistered = true, registrationStatus = matchedReg.status)
                            } else if (event.registeredUserIds.contains(currentUserId)) {
                                event.copy(isRegistered = true, registrationStatus = "confirmed")
                            } else {
                                event
                            }
                        }.toMutableList()

                        // 5. Load executive reviews
                        if (isSuperAdmin || executiveClubIds.isNotEmpty()) {
                            loadExecutiveRequests()
                        } else {
                            binding.tvPendingCountBadge.visibility = View.GONE
                            renderCurrentTab()
                        }
                    }
                }
            }
        }
    }

    private fun loadExecutiveRequests() {
        repo.getClubMembershipRequestsForReview(executiveClubIds, isSuperAdmin) { clubRequests ->
            allClubMembershipRequests = clubRequests.toMutableList()

            repo.getRegistrationsForReview(executiveClubIds, isSuperAdmin) { eventRequests ->
                allEventRegistrations = eventRequests.toMutableList()

                val pendingClubCount = allClubMembershipRequests.count { it.status.equals("pending", ignoreCase = true) }
                val pendingEventCount = allEventRegistrations.count { it.status.equals("pending", ignoreCase = true) }
                val totalPending = pendingClubCount + pendingEventCount

                if (totalPending > 0) {
                    binding.tvPendingCountBadge.visibility = View.VISIBLE
                    binding.tvPendingCountBadge.text = totalPending.toString()
                } else {
                    binding.tvPendingCountBadge.visibility = View.GONE
                }

                binding.pillSubClubRequests.text = "Club Memberships ($pendingClubCount)"
                binding.pillSubEventRequests.text = "Event Registrations ($pendingEventCount)"

                renderCurrentTab()
            }
        }
    }

    private fun selectTab(tab: String) {
        currentTab = tab
        renderCurrentTab()
    }

    private fun selectRequestsSubTab(subTab: String) {
        currentRequestsSubTab = subTab
        renderCurrentTab()
    }

    private fun renderCurrentTab() {
        val mutedText = Color.parseColor("#94A3B8")
        val selectedText = Color.parseColor("#C7D2FE")
        val selectedTint = ColorStateList.valueOf(Color.parseColor("#3B3864"))

        binding.tabEvents.backgroundTintList = null
        binding.tvTabEvents.setTextColor(mutedText)
        binding.ivTabEvents.imageTintList = ColorStateList.valueOf(mutedText)

        binding.tabClubs.backgroundTintList = null
        binding.tvTabClubs.setTextColor(mutedText)
        binding.ivTabClubs.imageTintList = ColorStateList.valueOf(mutedText)

        binding.tabRequests.backgroundTintList = null
        binding.tvTabRequests.setTextColor(mutedText)
        binding.ivTabRequests.imageTintList = ColorStateList.valueOf(mutedText)

        binding.layoutRequestsSubTabs.visibility = View.GONE
        updateActionButtons()

        when (currentTab) {
            "events" -> {
                binding.tabEvents.backgroundTintList = selectedTint
                binding.tvTabEvents.setTextColor(selectedText)
                binding.ivTabEvents.imageTintList = ColorStateList.valueOf(selectedText)

                binding.tvEventsSectionHeader.text = "Upcoming Events (${allEvents.size})"
                eventAdapter.updateList(allEvents, currentUserId, isSuperAdmin, executiveClubIds)
                binding.rvEventsClubs.adapter = eventAdapter

                if (allEvents.isEmpty()) {
                    showEmptyState("No Upcoming Events", "Check back soon for upcoming campus competitions and workshops.")
                } else {
                    hideEmptyState()
                }
            }
            "clubs" -> {
                binding.tabClubs.backgroundTintList = selectedTint
                binding.tvTabClubs.setTextColor(selectedText)
                binding.ivTabClubs.imageTintList = ColorStateList.valueOf(selectedText)

                binding.tvEventsSectionHeader.text = "Registered Clubs (${allClubs.size})"
                clubAdapter.updateList(allClubs, currentUserId, isSuperAdmin)
                binding.rvEventsClubs.adapter = clubAdapter

                if (allClubs.isEmpty()) {
                    showEmptyState("No Clubs Registered", "Registered student organizations will appear here.")
                } else {
                    hideEmptyState()
                }
            }
            "requests" -> {
                binding.tabRequests.backgroundTintList = selectedTint
                binding.tvTabRequests.setTextColor(selectedText)
                binding.ivTabRequests.imageTintList = ColorStateList.valueOf(selectedText)

                val isExec = isSuperAdmin || executiveClubIds.isNotEmpty()
                if (!isExec) {
                    binding.tvEventsSectionHeader.text = "Requests"
                    binding.rvEventsClubs.adapter = null
                    showEmptyState(
                        "Executive Access Only",
                        "Only Club Executives (President, GS, AGS) and Admins can view and approve student membership and event registration requests."
                    )
                } else {
                    binding.layoutRequestsSubTabs.visibility = View.VISIBLE
                    renderRequestsSubTab()
                }
            }
        }
    }

    private fun renderRequestsSubTab() {
        val mutedText = Color.parseColor("#94A3B8")
        val selectedText = Color.parseColor("#C7D2FE")
        val selectedBg = Color.parseColor("#3B3864")

        if (currentRequestsSubTab == "club") {
            binding.pillSubClubRequests.backgroundTintList = ColorStateList.valueOf(selectedBg)
            binding.pillSubClubRequests.setTextColor(selectedText)
            binding.pillSubEventRequests.backgroundTintList = null
            binding.pillSubEventRequests.setTextColor(mutedText)

            binding.tvEventsSectionHeader.text = "Club Join Applications (${allClubMembershipRequests.size})"
            clubMembershipAdapter.updateList(allClubMembershipRequests)
            binding.rvEventsClubs.adapter = clubMembershipAdapter

            if (allClubMembershipRequests.isEmpty()) {
                showEmptyState("No Club Join Requests", "Student membership applications for your clubs will appear here.")
            } else {
                hideEmptyState()
            }
        } else {
            binding.pillSubEventRequests.backgroundTintList = ColorStateList.valueOf(selectedBg)
            binding.pillSubEventRequests.setTextColor(selectedText)
            binding.pillSubClubRequests.backgroundTintList = null
            binding.pillSubClubRequests.setTextColor(mutedText)

            binding.tvEventsSectionHeader.text = "Event Registrations (${allEventRegistrations.size})"
            eventRegAdapter.updateList(allEventRegistrations)
            binding.rvEventsClubs.adapter = eventRegAdapter

            if (allEventRegistrations.isEmpty()) {
                showEmptyState("No Event Registrations", "Registrations for your campus events will appear here.")
            } else {
                hideEmptyState()
            }
        }
    }

    private fun updateActionButtons() {
        when (currentTab) {
            "events" -> {
                val canCreateEvent = isSuperAdmin || executiveClubIds.isNotEmpty()
                if (canCreateEvent) {
                    binding.btnAddNew.visibility = View.VISIBLE
                    binding.btnAddNew.text = "+ Event"
                } else {
                    binding.btnAddNew.visibility = View.GONE
                }
            }
            "clubs" -> {
                if (isSuperAdmin) {
                    binding.btnAddNew.visibility = View.VISIBLE
                    binding.btnAddNew.text = "+ Club"
                } else {
                    binding.btnAddNew.visibility = View.GONE
                }
            }
            "requests" -> {
                binding.btnAddNew.visibility = View.GONE
            }
        }
    }

    private fun showEmptyState(title: String, subtitle: String) {
        binding.llEmptyState.visibility = View.VISIBLE
        binding.tvEmptyTitle.text = title
        binding.tvEmptySubtitle.text = subtitle
    }

    private fun hideEmptyState() {
        binding.llEmptyState.visibility = View.GONE
    }

    // ==========================================
    // CLUB JOIN FLOW & DETAILS
    // ==========================================

    private fun handleClubCardClick(club: CampusClub) {
        val isMember = club.memberUids.contains(currentUserId)
        val hasPending = userClubRequests.any { it.clubId == club.id && it.status.equals("pending", ignoreCase = true) }

        if (!isMember) {
            if (hasPending) {
                AlertDialog.Builder(this)
                    .setTitle(club.name)
                    .setMessage("⏳ Membership Application Pending\n\nYour application has been received and is currently awaiting review by the club executives (President, GS, AGS).")
                    .setPositiveButton("OK", null)
                    .show()
            } else {
                // Show Student Membership Application Dialog
                showClubJoinApplicationDialog(club)
            }
        } else {
            // Already a member -> Show Club Details & Role Management
            showClubDetailsDialog(club)
        }
    }

    private fun showClubJoinApplicationDialog(club: CampusClub) {
        val dialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_club_join_request, null)
        dialog.setContentView(view)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.behavior.skipCollapsed = true

        val tvClubName = view.findViewById<TextView>(R.id.tvJoinClubTargetName)
        val etFullName = view.findViewById<EditText>(R.id.etClubJoinFullName)
        val etDept = view.findViewById<EditText>(R.id.etClubJoinDepartment)
        val etStudentId = view.findViewById<EditText>(R.id.etClubJoinStudentId)
        val etEmail = view.findViewById<EditText>(R.id.etClubJoinEmail)
        val etPhone = view.findViewById<EditText>(R.id.etClubJoinPhone)
        val btnSubmit = view.findViewById<MaterialButton>(R.id.btnSubmitClubJoin)
        val btnClose = view.findViewById<ImageButton>(R.id.btnCloseJoinClubDialog)

        tvClubName.text = club.name
        btnClose.setOnClickListener { dialog.dismiss() }

        // Pre-fill profile info
        if (currentUserName.isNotBlank()) etFullName.setText(currentUserName)
        if (currentUserDept.isNotBlank()) etDept.setText(currentUserDept)
        if (currentUserStudentId.isNotBlank()) etStudentId.setText(currentUserStudentId)
        if (currentUserEmail.isNotBlank()) etEmail.setText(currentUserEmail)
        if (currentUserPhone.isNotBlank()) etPhone.setText(currentUserPhone)

        com.shuaib.classmate.utils.GstuDepartments.attachPicker(
            etDept,
            supportFragmentManager,
            "Select Department"
        ) {}
        com.shuaib.classmate.utils.PhoneNumberUtils.attachPhonePrefixWatcher(etPhone)

        btnSubmit.setOnClickListener {
            val name = etFullName.text.toString().trim()
            val dept = etDept.text.toString().trim()
            val studentId = etStudentId.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val phone = com.shuaib.classmate.utils.PhoneNumberUtils.normalize(etPhone.text.toString().trim())

            if (name.isBlank()) {
                etFullName.error = "Name is required"
                return@setOnClickListener
            }
            if (dept.isBlank()) {
                etDept.error = "Department is required"
                return@setOnClickListener
            }
            if (studentId.isBlank()) {
                etStudentId.error = "Student ID is required"
                return@setOnClickListener
            }
            if (email.isBlank()) {
                etEmail.error = "Email is required"
                return@setOnClickListener
            }
            if (phone.isBlank()) {
                etPhone.error = "Phone number is required"
                return@setOnClickListener
            }

            val req = ClubMembershipRequest(
                clubId = club.id,
                clubName = club.name,
                userId = currentUserId,
                fullName = name,
                department = dept,
                studentId = studentId,
                email = email,
                phone = phone,
                status = "pending",
                timestamp = System.currentTimeMillis()
            )

            btnSubmit.isEnabled = false
            btnSubmit.text = "Submitting..."

            repo.submitClubMembershipRequest(
                request = req,
                onSuccess = {
                    dialog.dismiss()
                    Toast.makeText(this, "Membership application submitted! Awaiting executive approval.", Toast.LENGTH_LONG).show()
                    loadData()
                },
                onFailure = { e ->
                    btnSubmit.isEnabled = true
                    btnSubmit.text = "Submit Membership Application"
                    Toast.makeText(this, "Failed to submit: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }

        dialog.show()
    }

    private fun showClubDetailsDialog(club: CampusClub) {
        val userRole = club.getUserRole(currentUserId, isSuperAdmin)
        val canManageRoles = isSuperAdmin || club.presidentId == currentUserId

        val leadershipInfo = StringBuilder()
        leadershipInfo.append("👑 President: ${if (club.presidentName.isNotBlank()) club.presidentName else "Unassigned"}\n")
        leadershipInfo.append("📋 General Secretary (GS): ${if (club.gsName.isNotBlank()) club.gsName else "Unassigned"}\n")
        leadershipInfo.append("🎖 Assistant General Secretary (AGS): ${if (club.agsName.isNotBlank()) club.agsName else "Unassigned"}\n")
        leadershipInfo.append("\n👥 Active Community: ${club.getRealMemberCountText()}\n")
        leadershipInfo.append("🏷 Category: ${club.category}\n")
        if (club.contactEmail.isNotBlank()) {
            leadershipInfo.append("✉ Email: ${club.contactEmail}\n")
        }
        leadershipInfo.append("\nYour Status in this Club: $userRole")

        val builder = AlertDialog.Builder(this)
            .setTitle(club.name)
            .setMessage("${club.subtitle}\n\n${club.description}\n\n$leadershipInfo")
            .setPositiveButton("Leave Club") { _, _ ->
                repo.leaveClub(club.id, currentUserId, {
                    Toast.makeText(this, "You left ${club.name}", Toast.LENGTH_SHORT).show()
                    loadData()
                }, { e ->
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                })
            }
            .setNeutralButton("Close", null)

        if (canManageRoles) {
            builder.setNegativeButton("Manage Roles") { _, _ ->
                showManageClubRolesDialog(club)
            }
        }

        builder.show()
    }

    // Manage Club Roles (Superadmin & President)
    private fun showManageClubRolesDialog(club: CampusClub) {
        val dialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_manage_club_roles, null)
        dialog.setContentView(view)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.behavior.skipCollapsed = true

        val tvClubName = view.findViewById<TextView>(R.id.tvManageClubName)
        val etPres = view.findViewById<EditText>(R.id.etManagePresidentName)
        val etGs = view.findViewById<EditText>(R.id.etManageGsName)
        val etAgs = view.findViewById<EditText>(R.id.etManageAgsName)
        val btnSave = view.findViewById<MaterialButton>(R.id.btnSaveLeadershipRoles)
        val btnClose = view.findViewById<ImageButton>(R.id.btnCloseManageRoles)

        tvClubName.text = club.name
        etPres.setText(club.presidentName)
        etGs.setText(club.gsName)
        etAgs.setText(club.agsName)

        btnClose.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val pres = etPres.text.toString().trim()
            val gs = etGs.text.toString().trim()
            val ags = etAgs.text.toString().trim()

            if (pres.isBlank()) {
                etPres.error = "President name is required"
                return@setOnClickListener
            }
            if (gs.isBlank()) {
                etGs.error = "GS name is required"
                return@setOnClickListener
            }
            if (ags.isBlank()) {
                etAgs.error = "AGS name is required"
                return@setOnClickListener
            }

            btnSave.isEnabled = false
            btnSave.text = "Saving..."

            repo.updateClubLeadership(
                clubId = club.id,
                presidentName = pres,
                presidentId = if (isSuperAdmin && club.presidentId.isBlank()) currentUserId else club.presidentId,
                gsName = gs,
                gsId = club.gsId,
                agsName = ags,
                agsId = club.agsId,
                onSuccess = {
                    dialog.dismiss()
                    Toast.makeText(this, "Leadership roles updated successfully! 🎖", Toast.LENGTH_SHORT).show()
                    loadData()
                },
                onFailure = { e ->
                    btnSave.isEnabled = true
                    btnSave.text = "Save Leadership Changes"
                    Toast.makeText(this, "Failed to update: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }

        dialog.show()
    }

    // Approve Club Membership Request
    private fun approveClubMembership(req: ClubMembershipRequest) {
        AlertDialog.Builder(this)
            .setTitle("Approve Membership?")
            .setMessage("Approve ${req.fullName} (ID: ${req.studentId}, Dept: ${req.department}) as official member of '${req.clubName}'?")
            .setPositiveButton("Approve & Add") { _, _ ->
                repo.approveClubMembershipRequest(
                    request = req,
                    onSuccess = {
                        Toast.makeText(this, "Approved and added ${req.fullName} to club members! 🎉", Toast.LENGTH_SHORT).show()
                        loadData()
                    },
                    onFailure = { e ->
                        Toast.makeText(this, "Failed to approve: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ==========================================
    // EVENT REGISTRATION & CREATION FLOW
    // ==========================================

    private fun handleEventAction(event: CampusEvent) {
        if (event.isRegistered) {
            val statusMsg = if (event.registrationStatus.equals("pending", ignoreCase = true)) {
                "Your registration request for ${event.title} is currently pending executive confirmation."
            } else {
                "You are officially registered for ${event.title}! ✔"
            }
            AlertDialog.Builder(this)
                .setTitle("Registration Status")
                .setMessage(statusMsg)
                .setPositiveButton("OK", null)
                .show()
            return
        }

        if (!event.isRegistrationOpen) {
            showEventDetailsDialog(event)
            return
        }

        // Show Event Registration Form BottomSheet
        val dialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_event_register, null)
        dialog.setContentView(view)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.behavior.skipCollapsed = true

        val tvEventTitle = view.findViewById<TextView>(R.id.tvRegisterEventTitle)
        val etFullName = view.findViewById<EditText>(R.id.etRegisterFullName)
        val etDepartment = view.findViewById<EditText>(R.id.etRegisterDepartment)
        val etStudentId = view.findViewById<EditText>(R.id.etRegisterStudentId)
        val etEmail = view.findViewById<EditText>(R.id.etRegisterEmail)
        val etPhone = view.findViewById<EditText>(R.id.etRegisterPhone)
        val btnSubmit = view.findViewById<MaterialButton>(R.id.btnSubmitRegistration)
        val btnClose = view.findViewById<ImageButton>(R.id.btnCloseDialog)

        tvEventTitle.text = event.title
        btnClose.setOnClickListener { dialog.dismiss() }

        if (currentUserName.isNotBlank()) etFullName.setText(currentUserName)
        if (currentUserDept.isNotBlank()) etDepartment.setText(currentUserDept)
        if (currentUserStudentId.isNotBlank()) etStudentId.setText(currentUserStudentId)
        if (currentUserEmail.isNotBlank()) etEmail.setText(currentUserEmail)
        if (currentUserPhone.isNotBlank()) etPhone.setText(currentUserPhone)

        com.shuaib.classmate.utils.GstuDepartments.attachPicker(
            etDepartment,
            supportFragmentManager,
            "Select Department"
        ) {}
        com.shuaib.classmate.utils.PhoneNumberUtils.attachPhonePrefixWatcher(etPhone)

        btnSubmit.setOnClickListener {
            val name = etFullName.text.toString().trim()
            val dept = etDepartment.text.toString().trim()
            val studentId = etStudentId.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val phone = com.shuaib.classmate.utils.PhoneNumberUtils.normalize(etPhone.text.toString().trim())

            if (name.isBlank()) {
                etFullName.error = "Name is required"
                return@setOnClickListener
            }
            if (dept.isBlank()) {
                etDepartment.error = "Department is required"
                return@setOnClickListener
            }
            if (studentId.isBlank()) {
                etStudentId.error = "Student ID is required"
                return@setOnClickListener
            }
            if (email.isBlank()) {
                etEmail.error = "Email is required"
                return@setOnClickListener
            }
            if (phone.isBlank()) {
                etPhone.error = "Phone number is required"
                return@setOnClickListener
            }

            val reg = EventRegistration(
                eventId = event.id,
                eventTitle = event.title,
                clubId = event.clubId,
                clubName = event.organizer,
                userId = currentUserId,
                fullName = name,
                department = dept,
                studentId = studentId,
                email = email,
                phone = phone,
                status = "pending",
                timestamp = System.currentTimeMillis()
            )

            btnSubmit.isEnabled = false
            btnSubmit.text = "Submitting..."

            repo.registerForEvent(
                registration = reg,
                onSuccess = {
                    dialog.dismiss()
                    Toast.makeText(this, "Registration request submitted! Awaiting club executive confirmation.", Toast.LENGTH_LONG).show()
                    loadData()
                },
                onFailure = { e ->
                    btnSubmit.isEnabled = true
                    btnSubmit.text = "Submit Registration Request"
                    Toast.makeText(this, "Error submitting: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }

        dialog.show()
    }

    private fun showEventDetailsDialog(event: CampusEvent) {
        AlertDialog.Builder(this)
            .setTitle(event.title)
            .setMessage("${event.description}\n\n📍 Location: ${event.location}\n🕒 Time: ${event.dateTime}\n🏛 Organizer: ${event.organizer}\n\nStatus: ${event.status}")
            .setPositiveButton("Close", null)
            .show()
    }

    private fun confirmEventRegistration(reg: EventRegistration) {
        AlertDialog.Builder(this)
            .setTitle("Confirm Registration?")
            .setMessage("Confirm registration for ${reg.fullName} (ID: ${reg.studentId}) for event '${reg.eventTitle}'?")
            .setPositiveButton("Confirm") { _, _ ->
                repo.confirmRegistration(
                    registrationId = reg.id,
                    onSuccess = {
                        Toast.makeText(this, "Registration confirmed for ${reg.fullName}! ✔", Toast.LENGTH_SHORT).show()
                        loadData()
                    },
                    onFailure = { e ->
                        Toast.makeText(this, "Failed to confirm: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDeleteEvent(event: CampusEvent) {
        AlertDialog.Builder(this)
            .setTitle("Delete Event?")
            .setMessage("Are you sure you want to delete '${event.title}'? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                repo.deleteEvent(
                    eventId = event.id,
                    onSuccess = {
                        Toast.makeText(this, "Event deleted successfully", Toast.LENGTH_SHORT).show()
                        loadData()
                    },
                    onFailure = { e ->
                        Toast.makeText(this, "Error deleting event: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCreateEventDialog() {
        val eligibleClubs = if (isSuperAdmin) {
            allClubs
        } else {
            allClubs.filter { executiveClubIds.contains(it.id) }
        }

        if (eligibleClubs.isEmpty()) {
            Toast.makeText(this, "Only club executives or superadmin can create events", Toast.LENGTH_SHORT).show()
            return
        }

        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_create_campus_event, null)
        dialog.setContentView(view)
        dialog.applyFloatingPostAnimation(this, 0.94, 0.82)

        val spnrClub = view.findViewById<Spinner>(R.id.spnrClubSelector)
        val etTitle = view.findViewById<EditText>(R.id.etNewEventTitle)
        val etCategory = view.findViewById<EditText>(R.id.etNewEventCategory)
        val btnPickDate = view.findViewById<View>(R.id.btnPickDate)
        val tvSelectedDate = view.findViewById<TextView>(R.id.tvSelectedDate)
        val btnPickTime = view.findViewById<View>(R.id.btnPickTime)
        val tvSelectedTime = view.findViewById<TextView>(R.id.tvSelectedTime)
        val etLocation = view.findViewById<EditText>(R.id.etNewEventLocation)
        val etDesc = view.findViewById<EditText>(R.id.etNewEventDescription)
        val btnPublish = view.findViewById<MaterialButton>(R.id.btnPublishEvent)
        val btnClose = view.findViewById<ImageButton>(R.id.btnCloseCreateEvent)

        btnClose.setOnClickListener { dialog.dismiss() }

        val clubNames = eligibleClubs.map { it.name }
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, clubNames)
        spnrClub.adapter = spinnerAdapter

        var selectedDateStr = ""
        var selectedDateDisplay = ""
        var selectedDateMillis = 0L
        var selectedTimeStr = ""

        btnPickDate.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                val c = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
                val sdfValue = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val sdfDisplay = SimpleDateFormat("MMM dd, yyyy", Locale.US)
                selectedDateStr = sdfValue.format(c.time)
                selectedDateDisplay = sdfDisplay.format(c.time)
                selectedDateMillis = c.timeInMillis
                tvSelectedDate.text = selectedDateDisplay
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        btnPickTime.setOnClickListener {
            val cal = Calendar.getInstance()
            TimePickerDialog(this, { _, hourOfDay, minute ->
                val amPm = if (hourOfDay >= 12) "PM" else "AM"
                val hour12 = when {
                    hourOfDay == 0 -> 12
                    hourOfDay > 12 -> hourOfDay - 12
                    else -> hourOfDay
                }
                selectedTimeStr = String.format(Locale.US, "%02d:%02d %s", hour12, minute, amPm)
                tvSelectedTime.text = selectedTimeStr
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
        }

        btnPublish.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val category = etCategory.text.toString().trim()
            val location = etLocation.text.toString().trim()
            val description = etDesc.text.toString().trim()

            if (title.isBlank()) {
                etTitle.error = "Title is required"
                return@setOnClickListener
            }
            if (category.isBlank()) {
                etCategory.error = "Category is required"
                return@setOnClickListener
            }
            if (selectedDateStr.isBlank()) {
                Toast.makeText(this, "Please select an event date", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedTimeStr.isBlank()) {
                Toast.makeText(this, "Please select an event time", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (location.isBlank()) {
                etLocation.error = "Location is required"
                return@setOnClickListener
            }

            val selectedClub = eligibleClubs[spnrClub.selectedItemPosition]
            val displayDateTime = "$selectedDateDisplay • $selectedTimeStr"
            val eventRole = if (isSuperAdmin) "Superadmin" else selectedClub.getUserRole(currentUserId)

            val newEvent = CampusEvent(
                clubId = selectedClub.id,
                title = title,
                organizer = selectedClub.name,
                date = selectedDateStr,
                time = selectedTimeStr,
                dateTime = displayDateTime,
                eventDateMillis = selectedDateMillis,
                location = location,
                category = category,
                status = "REGISTRATION OPEN",
                isRegistrationOpen = true,
                description = description,
                creatorId = currentUserId,
                creatorRole = eventRole,
                createdAt = System.currentTimeMillis()
            )

            btnPublish.isEnabled = false
            btnPublish.text = "Publishing..."

            repo.addEvent(
                event = newEvent,
                onSuccess = {
                    dialog.dismiss()
                    Toast.makeText(this, "Event published successfully! 🎉", Toast.LENGTH_SHORT).show()
                    loadData()
                },
                onFailure = { e ->
                    btnPublish.isEnabled = true
                    btnPublish.text = "Publish Event"
                    Toast.makeText(this, "Error publishing: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }

        dialog.show()
    }

    // Create Club Dialog (Superadmin only) - Full Height & Expanded
    private fun showCreateClubDialog() {
        if (!isSuperAdmin) {
            Toast.makeText(this, "Only Superadmin can register new clubs", Toast.LENGTH_SHORT).show()
            return
        }

        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_create_campus_club, null)
        dialog.setContentView(view)
        dialog.applyFloatingPostAnimation(this, 0.94, 0.82)

        val etName = view.findViewById<EditText>(R.id.etNewClubName)
        val etSubtitle = view.findViewById<EditText>(R.id.etNewClubSubtitle)
        val etCategory = view.findViewById<EditText>(R.id.etNewClubCategory)
        val etPresident = view.findViewById<EditText>(R.id.etClubPresidentName)
        val etGs = view.findViewById<EditText>(R.id.etClubGsName)
        val etAgs = view.findViewById<EditText>(R.id.etClubAgsName)
        val etDesc = view.findViewById<EditText>(R.id.etNewClubDescription)
        val etEmail = view.findViewById<EditText>(R.id.etNewClubEmail)
        val btnSubmit = view.findViewById<MaterialButton>(R.id.btnCreateClubSubmit)
        val btnClose = view.findViewById<ImageButton>(R.id.btnCloseCreateClub)

        btnClose.setOnClickListener { dialog.dismiss() }

        btnSubmit.setOnClickListener {
            val name = etName.text.toString().trim()
            val subtitle = etSubtitle.text.toString().trim()
            val category = etCategory.text.toString().trim()
            val presidentName = etPresident.text.toString().trim()
            val gsName = etGs.text.toString().trim()
            val agsName = etAgs.text.toString().trim()
            val description = etDesc.text.toString().trim()
            val email = etEmail.text.toString().trim()

            if (name.isBlank()) {
                etName.error = "Club name is required"
                return@setOnClickListener
            }
            if (subtitle.isBlank()) {
                etSubtitle.error = "Subtitle is required"
                return@setOnClickListener
            }
            if (category.isBlank()) {
                etCategory.error = "Category is required"
                return@setOnClickListener
            }
            if (presidentName.isBlank()) {
                etPresident.error = "President name is required"
                return@setOnClickListener
            }
            if (gsName.isBlank()) {
                etGs.error = "GS name is required"
                return@setOnClickListener
            }
            if (agsName.isBlank()) {
                etAgs.error = "AGS name is required"
                return@setOnClickListener
            }

            val newClub = CampusClub(
                name = name,
                subtitle = subtitle,
                category = category,
                presidentName = presidentName,
                presidentId = currentUserId, // Superadmin creator is initially linked
                gsName = gsName,
                agsName = agsName,
                president = "Executive Committee",
                description = description,
                contactEmail = email,
                memberUids = emptyList(), // Real starting member count
                createdAt = System.currentTimeMillis()
            )

            btnSubmit.isEnabled = false
            btnSubmit.text = "Creating Club..."

            repo.addClub(
                club = newClub,
                onSuccess = {
                    dialog.dismiss()
                    Toast.makeText(this, "Club '$name' registered successfully! 🏛", Toast.LENGTH_SHORT).show()
                    loadData()
                },
                onFailure = { e ->
                    btnSubmit.isEnabled = true
                    btnSubmit.text = "Create Club"
                    Toast.makeText(this, "Error creating club: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }

        dialog.show()
    }

    private fun confirmDeleteClub(club: CampusClub) {
        val canDelete = isSuperAdmin || (club.presidentId.isNotBlank() && club.presidentId == currentUserId)
        if (!canDelete) {
            Toast.makeText(this, "Only Superadmin or Club President can delete a club", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Delete Club?")
            .setMessage("Are you sure you want to delete '${club.name}'? This will permanently remove the club organization.")
            .setPositiveButton("Delete") { _, _ ->
                repo.deleteClub(
                    clubId = club.id,
                    onSuccess = {
                        Toast.makeText(this, "Club deleted", Toast.LENGTH_SHORT).show()
                        loadData()
                    },
                    onFailure = { e ->
                        Toast.makeText(this, "Failed to delete: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
