// com/shuaib/classmate/fragments/FriendsFragment.kt
package com.shuaib.classmate.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.shuaib.classmate.R
import com.shuaib.classmate.adapters.FriendsAdapter
import com.shuaib.classmate.databinding.FragmentFriendsBinding
import com.shuaib.classmate.models.User

class FriendsFragment : Fragment() {

    private var _binding: FragmentFriendsBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: FirebaseFirestore
    private lateinit var friendsAdapter: FriendsAdapter

    private var usersListener: ListenerRegistration? = null
    private var configListener: ListenerRegistration? = null

    private var allUsersList = listOf<User>()
    private var isFriendsPublic = false
    private var currentUserRole = "student"
    private var currentUserBloodGroup = ""
    private var currentUserDistrict = ""
    private var currentUserBatch = ""
    private var accessChecked = false   // becomes true once both config + role are fetched
    private var selectedBloodGroup: String? = null
    private var selectedDistrict: String? = null
    private var selectedBatch: String? = null
    private var showingMatches = false
    private var isTeacherMode = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFriendsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = FirebaseFirestore.getInstance()

        isTeacherMode = arguments?.getBoolean("isTeacherMode") ?: false
        if (isTeacherMode) {
            binding.tvHeaderTitle.text = "Teacher Directory"
            binding.chipBatch?.visibility = android.view.View.GONE
            binding.chipMyMatches.visibility = android.view.View.GONE
            binding.tilSearch.hint = "Search name or department..."
            binding.chipDistrict.text = "Department"
        } else {
            binding.tvHeaderTitle.text = "Student Directory"
        }
        
        setupRecyclerView()
        setupSearch()
        setupFilters()

        fetchCurrentUserRole()
    }

    private fun fetchCurrentUserRole() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            showLockedState()
            return
        }

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (_binding == null || !isAdded) return@addOnSuccessListener
                currentUserRole = doc.getString("role") ?: "student"
                currentUserBloodGroup = doc.getString("bloodGroup").orEmpty()
                currentUserDistrict = doc.getString("homeDistrict").orEmpty()
                currentUserBatch = doc.getString("batch").orEmpty()
                listenToFriendsConfig()
            }
            .addOnFailureListener {
                if (_binding == null || !isAdded) return@addOnFailureListener
                // Couldn't load role – fall back to config-only check
                listenToFriendsConfig()
            }
    }

    /**
     * Step 2 – Listen to config/friends in real-time.
     * This ensures the list appears / disappears immediately when
     * the admin toggles "Make Friends List Public".
     */
    private fun listenToFriendsConfig() {
        isFriendsPublic = true
        applyAccessDecision()
    }

    /**
     * Core access gate – called each time either the role or the flag changes.
     * Admins and superadmins always have access regardless of the toggle.
     */
    private fun applyAccessDecision() {
        val hasAccess = true

        if (hasAccess) {
            showNormalState()
            // Start (or restart) listening for users only when access is granted
            if (usersListener == null) listenForUsers()
        } else {
            showLockedState()
            // Stop the real-time users listener to avoid unnecessary reads
            usersListener?.remove()
            usersListener = null
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // UI state helpers
    // ──────────────────────────────────────────────────────────────────────

    private fun showNormalState() {
        if (_binding == null) return
        binding.tilSearch.isVisible = true
        binding.rvFriends.isVisible = true
        binding.emptySearchState.isVisible = false
        binding.lockedState.isVisible = false
    }

    private fun showLockedState() {
        if (_binding == null) return
        binding.tilSearch.isVisible = false
        binding.rvFriends.isVisible = false
        binding.emptySearchState.isVisible = false
        binding.lockedState.isVisible = true
    }

    // ──────────────────────────────────────────────────────────────────────
    // RecyclerView & Search
    // ──────────────────────────────────────────────────────────────────────

    private fun setupRecyclerView() {
        friendsAdapter = FriendsAdapter(
            friends = emptyList(),
            onFriendClick = { user ->
                val bundle = Bundle().apply { putString("userId", user.uid) }
                findNavController().navigate(R.id.action_friends_to_detail, bundle)
            },
            onCallClick = { user ->
                if (user.phone.isNotEmpty()) {
                    val intent = Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:${user.phone}")
                    }
                    startActivity(intent)
                } else {
                    Toast.makeText(context, "Phone number not available", Toast.LENGTH_SHORT).show()
                }
            },
            onWhatsappClick = { user ->
                if (user.phone.isNotEmpty()) {
                    val cleanPhone = user.phone.filter { it.isDigit() || it == '+' }
                    val url = "https://api.whatsapp.com/send?phone=$cleanPhone"
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse(url)
                    }
                    try {
                        startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Phone number not available", Toast.LENGTH_SHORT).show()
                }
            }
        )

        binding.rvFriends.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = friendsAdapter
        }
    }

    private fun listenForUsers() {
        usersListener?.remove()
        usersListener = db.collection("users")
            .addSnapshotListener { snapshot, error ->
                if (_binding == null) return@addSnapshotListener
                if (error != null) {
                    // Permission denied means the Firestore rule blocked the read.
                    // Surface a clean message instead of a raw exception.
                    if (error.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        Toast.makeText(
                            context,
                            "Friends list is not available right now.",
                            Toast.LENGTH_SHORT
                        ).show()
                        showLockedState()
                    } else {
                        Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    allUsersList = snapshot.documents.mapNotNull { doc ->
                        try {
                            val userBatch = doc.getString("batch").orEmpty()
                            val userRole = doc.getString("role") ?: "student"

                            if (isTeacherMode && userRole != "teacher") {
                                return@mapNotNull null
                            }
                            if (!isTeacherMode && userRole == "teacher") {
                                return@mapNotNull null
                            }
                            
                            User(
                                uid = doc.id,
                                batch = userBatch,
                                name = doc.getString("name").orEmpty(),
                                fullName = doc.getString("fullName").orEmpty(),
                                studentId = doc.getString("studentId").orEmpty(),
                                department = doc.getString("department").orEmpty(),
                                email = doc.getString("email").orEmpty(),
                                phone = doc.getString("phone").orEmpty(),
                                bloodGroup = doc.getString("bloodGroup").orEmpty(),
                                homeDistrict = doc.getString("homeDistrict").orEmpty(),
                                address = doc.getString("address").orEmpty(),
                                role = doc.getString("role") ?: "student",
                                approved = doc.getBoolean("approved") ?: false,
                                photoUrl = doc.getString("photoUrl").orEmpty(),
                                authProvider = doc.getString("authProvider").orEmpty(),
                                oneSignalPlayerId = doc.getString("oneSignalPlayerId").orEmpty()
                            )
                        } catch (e: Exception) {
                            android.util.Log.e("FriendsFragment", "Skipping user \${doc.id} due to parse error", e)
                            null
                        }
                    }.sortedBy { it.name }

                    val query = binding.etSearch.text.toString()
                    filterList(query)
                }
            }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterList(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupFilters() {
        binding.chipGroupFilters.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.contains(R.id.chipAll)) {
                showingMatches = false
                resetFilters()
            }
        }

        binding.chipMyMatches.setOnClickListener {
            showingMatches = true
            selectedBloodGroup = currentUserBloodGroup.takeIf { it.isNotBlank() }
            selectedDistrict = currentUserDistrict.takeIf { it.isNotBlank() }
            binding.chipBlood.text = "Blood Group"
            binding.chipDistrict.text = "Home District"
            filterList(binding.etSearch.text.toString())
            if (selectedBloodGroup == null && selectedDistrict == null) {
                Toast.makeText(context, "Please update your profile with blood group & district first.", Toast.LENGTH_LONG).show()
            }
        }

        binding.chipBlood.setOnClickListener {
            showingMatches = false
            showBloodGroupSelector()
        }

        binding.chipDistrict.setOnClickListener {
            showingMatches = false
            if (isTeacherMode) showDepartmentSelector() else showDistrictSelector()
        }
        
        binding.chipBatch?.setOnClickListener {
            showingMatches = false
            showBatchSelector()
        }
    }

    private fun resetFilters() {
        selectedBloodGroup = null
        selectedDistrict = null
        selectedBatch = null
        binding.chipBlood.text = "Blood Group"
        binding.chipDistrict.text = if (isTeacherMode) "Department" else "Home District"
        binding.chipBatch?.text = "Batch"
        binding.chipAll.isChecked = true
        filterList(binding.etSearch.text.toString())
    }

    private fun showBloodGroupSelector() {
        val bloodGroups = listOf("A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-")
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Blood Group")
            .setItems(bloodGroups.toTypedArray()) { _, which ->
                val selected = bloodGroups[which]
                selectedBloodGroup = selected
                selectedDistrict = null
                selectedBatch = null
                binding.chipBlood.text = "Blood: $selected"
                binding.chipDistrict.text = "Home District"
                binding.chipBatch?.text = "Batch"
                binding.chipBlood.isChecked = true
                filterList(binding.etSearch.text.toString())
            }
            .setNeutralButton("Clear Filter") { _, _ ->
                resetFilters()
            }
            .show()
    }

    private fun showDistrictSelector() {
        val districts = allUsersList.map { it.homeDistrict.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .sortedWith(String.CASE_INSENSITIVE_ORDER)

        if (districts.isEmpty()) {
            Toast.makeText(context, "No districts available to filter", Toast.LENGTH_SHORT).show()
            binding.chipAll.isChecked = true
            return
        }

        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Home District")
            .setItems(districts.toTypedArray()) { _, which ->
                val selected = districts[which]
                selectedDistrict = selected
                selectedBloodGroup = null
                binding.chipDistrict.text = "District: $selected"
                binding.chipBlood.text = "Blood Group"
                binding.chipDistrict.isChecked = true
                filterList(binding.etSearch.text.toString())
            }
            .setNeutralButton("Clear Filter") { _, _ ->
                resetFilters()
            }
            .show()
    }

    private fun showDepartmentSelector() {
        val depts = allUsersList.map { it.department.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
            
        if (depts.isEmpty()) {
            android.widget.Toast.makeText(context, "No departments found", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Department")
            .setItems(depts.toTypedArray()) { _, which ->
                val selected = depts[which]
                selectedDistrict = selected // Reuse selectedDistrict variable for department
                selectedBloodGroup = null
                selectedBatch = null
                binding.chipDistrict.text = "Dept: $selected"
                binding.chipBlood.text = "Blood Group"
                binding.chipBatch?.text = "Batch"
                binding.chipDistrict.isChecked = true
                filterList(binding.etSearch.text.toString())
            }
            .setNeutralButton("Clear Filter") { _, _ ->
                resetFilters()
            }
            .show()
    }

    private fun showBatchSelector() {
        val batches = allUsersList.map { it.batch.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
            
        if (batches.isEmpty()) {
            android.widget.Toast.makeText(context, "No batches found", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Batch")
            .setItems(batches.toTypedArray()) { _, which ->
                val selected = batches[which]
                selectedBatch = selected
                selectedBloodGroup = null
                selectedDistrict = null
                binding.chipBatch?.text = "Batch: $selected"
                binding.chipBlood.text = "Blood Group"
                binding.chipDistrict.text = "Home District"
                binding.chipBatch?.isChecked = true
                filterList(binding.etSearch.text.toString())
            }
            .setNeutralButton("Clear Filter") { _, _ ->
                resetFilters()
            }
            .show()
    }

    private fun filterList(query: String) {
        var filtered = allUsersList

        if (showingMatches) {
            val hasBlood = !currentUserBloodGroup.isNullOrBlank()
            val hasDistrict = !currentUserDistrict.isNullOrBlank()
            
            filtered = filtered.filter { user ->
                // Don't match self
                if (user.uid == FirebaseAuth.getInstance().currentUser?.uid) return@filter false
                
                val matchBlood = hasBlood && user.bloodGroup.equals(currentUserBloodGroup, ignoreCase = true)
                val matchDistrict = hasDistrict && user.homeDistrict.equals(currentUserDistrict, ignoreCase = true)
                
                matchBlood || matchDistrict
            }
        } else {
            // Apply normal AND filtering
            selectedBloodGroup?.let { blood ->
                filtered = filtered.filter { it.bloodGroup.equals(blood, ignoreCase = true) }
            }
            selectedDistrict?.let { district ->
                if (isTeacherMode) {
                    filtered = filtered.filter { it.department.equals(district, ignoreCase = true) }
                } else {
                    filtered = filtered.filter { it.homeDistrict.equals(district, ignoreCase = true) }
                }
            }
            selectedBatch?.let { batch ->
                filtered = filtered.filter { it.batch.equals(batch, ignoreCase = true) }
            }
        }

        // Apply Text Query Filter
        val trimmedQuery = query.trim()
        if (trimmedQuery.isNotEmpty()) {
            filtered = filtered.filter { user ->
                user.name.contains(trimmedQuery, ignoreCase = true) ||
                user.studentId.contains(trimmedQuery, ignoreCase = true) ||
                user.bloodGroup.contains(trimmedQuery, ignoreCase = true) ||
                user.homeDistrict.contains(trimmedQuery, ignoreCase = true) ||
                user.department.contains(trimmedQuery, ignoreCase = true) ||
                user.address.contains(trimmedQuery, ignoreCase = true) ||
                user.phone.contains(trimmedQuery)
            }
        }

        updateListWithEmptyCheck(filtered)
    }

    private fun updateListWithEmptyCheck(list: List<User>) {
        friendsAdapter.updateList(list)
        binding.emptySearchState.isVisible = list.isEmpty()
        binding.rvFriends.isVisible = list.isNotEmpty()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        usersListener?.remove()
        configListener?.remove()
        _binding = null
    }
}
