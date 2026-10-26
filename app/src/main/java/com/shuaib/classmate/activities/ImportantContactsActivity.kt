package com.shuaib.classmate.activities

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.shuaib.classmate.R
import com.shuaib.classmate.adapters.ContactsAdapter
import com.shuaib.classmate.databinding.ActivityImportantContactsBinding
import com.shuaib.classmate.databinding.DialogAddImportantContactBinding
import com.shuaib.classmate.models.ImportantContact
import com.shuaib.classmate.models.User
import com.shuaib.classmate.utils.AppPreferences
import com.shuaib.classmate.utils.HapticHelper

class ImportantContactsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImportantContactsBinding
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var allContacts = mutableListOf<ImportantContact>()
    private var customContacts = mutableListOf<ImportantContact>()
    private var facultyContacts = mutableListOf<ImportantContact>()

    private var selectedCategory: String = "All"
    private var searchQuery = ""
    private var isSuperAdmin = false

    private lateinit var adapter: ContactsAdapter
    private var contactsListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImportantContactsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        checkSuperAdminStatus()

        adapter = ContactsAdapter(
            contacts = emptyList(),
            isSuperAdmin = isSuperAdmin,
            onDeleteContact = { contact -> promptDeleteContact(contact) }
        )
        binding.rvContacts.layoutManager = LinearLayoutManager(this)
        binding.rvContacts.adapter = adapter

        setupSearch()
        setupCategoryChips()

        // Populate initial contacts right away so user never sees empty state
        mergeAndFilter()

        binding.swipeRefresh.setOnRefreshListener {
            loadAllContacts()
        }

        binding.fabAddContact.setOnClickListener {
            HapticHelper.lightPop(it)
            showAddContactBottomSheet()
        }

        binding.btnSeedContacts.setOnClickListener {
            HapticHelper.lightPop(it)
            seedOfficialGstuContacts()
        }

        listenToCustomContacts()
        loadFacultyFromUsers()
    }

    override fun onDestroy() {
        super.onDestroy()
        contactsListener?.remove()
    }

    private fun checkSuperAdminStatus() {
        val uid = auth.currentUser?.uid
        val localDemoRole = AppPreferences(this).getDemoRole()

        if (localDemoRole.equals("superadmin", ignoreCase = true)) {
            isSuperAdmin = true
            binding.fabAddContact.visibility = View.VISIBLE
            return
        }

        if (uid != null) {
            firestore.collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    val role = doc.getString("role") ?: "student"
                    val demoRole = doc.getString("demoRole") ?: ""
                    val effectiveRole = localDemoRole.ifBlank { demoRole }.ifBlank { role }

                    isSuperAdmin = effectiveRole.equals("superadmin", ignoreCase = true)
                    binding.fabAddContact.visibility = if (isSuperAdmin) View.VISIBLE else View.GONE
                    filterContacts()
                }
        }
    }

    private fun setupSearch() {
        binding.etSearchContact.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString()?.trim()?.lowercase() ?: ""
                filterContacts()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupCategoryChips() {
        binding.chipGroupCategory.setOnCheckedStateChangeListener { _, checkedIds ->
            selectedCategory = when (checkedIds.firstOrNull()) {
                R.id.chipEmergency -> ImportantContact.CATEGORY_EMERGENCY
                R.id.chipAdmin -> ImportantContact.CATEGORY_ADMINISTRATION
                R.id.chipFaculty -> ImportantContact.CATEGORY_FACULTY
                R.id.chipMedical -> ImportantContact.CATEGORY_MEDICAL
                else -> "All"
            }
            filterContacts()
        }
    }

    private fun listenToCustomContacts() {
        if (allContacts.isEmpty()) {
            binding.progressBar.visibility = View.VISIBLE
        }
        contactsListener = firestore.collection("important_contacts")
            .addSnapshotListener { snapshot, error ->
                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false

                if (error != null) {
                    android.util.Log.e("ImportantContacts", "Listen failed", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    customContacts = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(ImportantContact::class.java)?.copy(id = doc.id)
                    }.toMutableList()
                    mergeAndFilter()
                }
            }
    }

    private fun loadFacultyFromUsers() {
        firestore.collection("users").get()
            .addOnSuccessListener { snapshot ->
                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
                facultyContacts = snapshot.documents.mapNotNull { doc ->
                    val user = doc.toObject(User::class.java)?.copy(uid = doc.id) ?: return@mapNotNull null
                    val r = user.role.lowercase()
                    if (r == "teacher" || r == "admin" || r == "superadmin") {
                        val contactName = user.fullName.ifBlank { user.name.ifBlank { "Faculty Member" } }
                        val dept = user.department.ifBlank { "University" }
                        val designation = when (r) {
                            "teacher" -> "Faculty Member"
                            "superadmin" -> "Super Administrator"
                            else -> "Academic Administrator"
                        }
                        val category = if (r == "teacher") ImportantContact.CATEGORY_FACULTY else ImportantContact.CATEGORY_ADMINISTRATION
                        val phoneNum = user.phone.ifBlank { user.whatsappNumber }

                        ImportantContact(
                            id = "user_${doc.id}",
                            name = contactName,
                            designation = designation,
                            departmentOrOffice = dept,
                            phone = phoneNum,
                            email = user.email,
                            category = category,
                            isEmergency = false
                        )
                    } else null
                }.toMutableList()
                mergeAndFilter()
            }
            .addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
                mergeAndFilter()
            }
    }

    private fun loadAllContacts() {
        binding.swipeRefresh.isRefreshing = true
        firestore.collection("important_contacts").get()
            .addOnSuccessListener { snapshot ->
                customContacts = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(ImportantContact::class.java)?.copy(id = doc.id)
                }.toMutableList()
                loadFacultyFromUsers()
            }
            .addOnFailureListener {
                loadFacultyFromUsers()
            }

        binding.swipeRefresh.postDelayed({
            if (isDestroyed) return@postDelayed
            binding.swipeRefresh.isRefreshing = false
            binding.progressBar.visibility = View.GONE
        }, 3000)
    }

    private fun mergeAndFilter() {
        val merged = mutableListOf<ImportantContact>()

        // Default official contacts from GSTU (Images 4 & 5)
        val defaultList = DEFAULT_OFFICIAL_CONTACTS.filter { def ->
            val defPhoneDigits = def.phone.replace("[^0-9]".toRegex(), "")
            customContacts.none { c ->
                val cPhoneDigits = c.phone.replace("[^0-9]".toRegex(), "")
                (defPhoneDigits.isNotBlank() && cPhoneDigits.endsWith(defPhoneDigits)) ||
                c.name.trim().equals(def.name.trim(), ignoreCase = true)
            }
        }
        merged.addAll(defaultList)
        merged.addAll(customContacts)
        merged.addAll(facultyContacts)

        allContacts = merged
        filterContacts()
    }

    private fun filterContacts() {
        val filtered = allContacts.filter { c ->
            val matchesCategory = when (selectedCategory) {
                "All" -> true
                ImportantContact.CATEGORY_EMERGENCY -> c.category.equals(ImportantContact.CATEGORY_EMERGENCY, ignoreCase = true) || c.isEmergency
                ImportantContact.CATEGORY_ADMINISTRATION -> c.category.equals(ImportantContact.CATEGORY_ADMINISTRATION, ignoreCase = true)
                ImportantContact.CATEGORY_FACULTY -> c.category.equals(ImportantContact.CATEGORY_FACULTY, ignoreCase = true)
                ImportantContact.CATEGORY_MEDICAL -> c.category.equals(ImportantContact.CATEGORY_MEDICAL, ignoreCase = true)
                else -> true
            }

            val matchesSearch = if (searchQuery.isBlank()) true else {
                c.name.lowercase().contains(searchQuery) ||
                c.designation.lowercase().contains(searchQuery) ||
                c.departmentOrOffice.lowercase().contains(searchQuery) ||
                c.phone.lowercase().contains(searchQuery) ||
                c.email.lowercase().contains(searchQuery)
            }

            matchesCategory && matchesSearch
        }.sortedWith(
            compareByDescending<ImportantContact> { it.isEmergency || it.category == ImportantContact.CATEGORY_EMERGENCY }
                .thenBy { it.name }
        )

        adapter.updateList(filtered, isSuperAdmin)

        if (filtered.isEmpty()) {
            binding.emptyState.visibility = View.VISIBLE
            binding.rvContacts.visibility = View.GONE
            binding.btnSeedContacts.visibility = if (isSuperAdmin && customContacts.isEmpty()) View.VISIBLE else View.GONE
        } else {
            binding.emptyState.visibility = View.GONE
            binding.rvContacts.visibility = View.VISIBLE
            binding.btnSeedContacts.visibility = View.GONE
        }
    }

    private fun showAddContactBottomSheet() {
        val dialog = BottomSheetDialog(this)
        val dialogBinding = DialogAddImportantContactBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        dialogBinding.btnCancel.setOnClickListener { dialog.dismiss() }

        val openDeptPicker = {
            com.shuaib.classmate.ui.DepartmentPickerBottomSheet.show(
                supportFragmentManager,
                "Select Department",
                dialogBinding.etDepartment.text?.toString()?.trim().orEmpty()
            ) { chosen ->
                dialogBinding.etDepartment.setText(chosen)
            }
        }
        dialogBinding.etDepartment.isFocusable = false
        dialogBinding.etDepartment.isFocusableInTouchMode = false
        dialogBinding.etDepartment.isCursorVisible = false
        dialogBinding.etDepartment.isClickable = true
        dialogBinding.etDepartment.setOnClickListener { openDeptPicker() }
        dialogBinding.tilDepartment.setEndIconOnClickListener { openDeptPicker() }

        dialogBinding.btnSaveContact.setOnClickListener {
            val name = dialogBinding.etContactName.text?.toString()?.trim().orEmpty()
            val designation = dialogBinding.etDesignation.text?.toString()?.trim().orEmpty()
            val department = dialogBinding.etDepartment.text?.toString()?.trim().orEmpty()
            val phone = dialogBinding.etPhone.text?.toString()?.trim().orEmpty()
            val email = dialogBinding.etEmail.text?.toString()?.trim().orEmpty()

            if (name.isBlank()) {
                dialogBinding.tilContactName.error = "Contact name is required"
                return@setOnClickListener
            }
            if (phone.isBlank()) {
                dialogBinding.tilPhone.error = "Phone number is required"
                return@setOnClickListener
            }

            val category = when (dialogBinding.chipGroupCategory.checkedChipId) {
                R.id.chipEmergency -> ImportantContact.CATEGORY_EMERGENCY
                R.id.chipAdmin -> ImportantContact.CATEGORY_ADMINISTRATION
                R.id.chipFaculty -> ImportantContact.CATEGORY_FACULTY
                R.id.chipMedical -> ImportantContact.CATEGORY_MEDICAL
                else -> ImportantContact.CATEGORY_GENERAL
            }

            val isEmergency = category == ImportantContact.CATEGORY_EMERGENCY

            val newDocRef = firestore.collection("important_contacts").document()
            val newContact = ImportantContact(
                id = newDocRef.id,
                name = name,
                designation = designation,
                departmentOrOffice = department,
                phone = phone,
                email = email,
                category = category,
                isEmergency = isEmergency,
                addedByUid = auth.currentUser?.uid.orEmpty(),
                createdAt = Timestamp.now()
            )

            dialogBinding.btnSaveContact.isEnabled = false
            dialogBinding.btnSaveContact.text = "Saving..."

            newDocRef.set(newContact)
                .addOnSuccessListener {
                    HapticHelper.success(this)
                    Toast.makeText(this, "✅ $name added to Important Contacts!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                .addOnFailureListener { e ->
                    dialogBinding.btnSaveContact.isEnabled = true
                    dialogBinding.btnSaveContact.text = "Save Contact"
                    Toast.makeText(this, "Failed to save: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }

        dialog.show()
    }

    private fun promptDeleteContact(contact: ImportantContact) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Contact?")
            .setMessage("Are you sure you want to remove '${contact.name}' from the university directory?")
            .setPositiveButton("Delete") { _, _ ->
                firestore.collection("important_contacts").document(contact.id).delete()
                    .addOnSuccessListener {
                        HapticHelper.lightPop(binding.root)
                        Toast.makeText(this, "Contact removed", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Delete failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun seedOfficialGstuContacts() {
        binding.progressBar.visibility = View.VISIBLE
        val batch = firestore.batch()

        val seedList = DEFAULT_OFFICIAL_CONTACTS.map {
            it.copy(id = "", addedByUid = auth.currentUser?.uid.orEmpty(), createdAt = Timestamp.now())
        }

        for (contact in seedList) {
            val docRef = firestore.collection("important_contacts").document()
            batch.set(docRef, contact.copy(id = docRef.id))
        }

        batch.commit()
            .addOnSuccessListener {
                binding.progressBar.visibility = View.GONE
                HapticHelper.success(this)
                Toast.makeText(this, "✅ Seeded official GSTU contacts successfully!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Failed to seed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    companion object {
        val DEFAULT_OFFICIAL_CONTACTS = listOf(
            ImportantContact(
                id = "default_med_1",
                name = "University Medical Center",
                designation = "24/7 Campus Medical Center",
                departmentOrOffice = "Campus Medical Center",
                phone = "01317848951",
                email = "medical@gstu.ac.bd",
                category = ImportantContact.CATEGORY_MEDICAL,
                isEmergency = true
            ),
            ImportantContact(
                id = "default_med_2",
                name = "University Ambulance",
                designation = "24/7 Emergency Ambulance Service",
                departmentOrOffice = "Campus Medical Center",
                phone = "01734926998",
                email = "ambulance@gstu.ac.bd",
                category = ImportantContact.CATEGORY_MEDICAL,
                isEmergency = true
            ),
            ImportantContact(
                id = "default_adm_1",
                name = "Proctor Office",
                designation = "Chief Proctor & Discipline Office",
                departmentOrOffice = "Proctorial Body",
                phone = "+8801763818889",
                email = "proctor@gstu.ac.bd",
                category = ImportantContact.CATEGORY_ADMINISTRATION,
                isEmergency = true
            ),
            ImportantContact(
                id = "default_adm_2",
                name = "Campus Security",
                designation = "24/7 Main Gate & Campus Security Desk",
                departmentOrOffice = "Security Control Room",
                phone = "+8801727202312",
                email = "security@gstu.ac.bd",
                category = ImportantContact.CATEGORY_ADMINISTRATION,
                isEmergency = true
            ),
            ImportantContact(
                id = "default_adm_3",
                name = "Transport Office",
                designation = "University Bus Schedule & Logistics",
                departmentOrOffice = "Transport Pool",
                phone = "+88-01720478937",
                email = "transport@gstu.ac.bd",
                category = ImportantContact.CATEGORY_ADMINISTRATION,
                isEmergency = false
            ),
            ImportantContact(
                id = "default_emg_1",
                name = "Fire Service (Nearby)",
                designation = "Local Fire Station & Rescue",
                departmentOrOffice = "Fire Service & Civil Defence",
                phone = "029555555",
                email = "",
                category = ImportantContact.CATEGORY_EMERGENCY,
                isEmergency = true
            ),
            ImportantContact(
                id = "default_emg_2",
                name = "Police Station (Nearby)",
                designation = "Local Police Station Helpline",
                departmentOrOffice = "Bangladesh Police",
                phone = "029666666",
                email = "",
                category = ImportantContact.CATEGORY_EMERGENCY,
                isEmergency = true
            ),
            ImportantContact(
                id = "default_emg_3",
                name = "National Emergency Service",
                designation = "National Emergency (Police, Fire, Ambulance)",
                departmentOrOffice = "National Emergency Hotline (Govt.)",
                phone = "999",
                email = "",
                category = ImportantContact.CATEGORY_EMERGENCY,
                isEmergency = true
            ),
            ImportantContact(
                id = "default_hall_1",
                name = "Shadhinota Dibos Hall",
                designation = "Provost & Residential Hall Administration",
                departmentOrOffice = "Residential Hall",
                phone = "+8801716094556",
                email = "shadhinota.hall@gstu.ac.bd",
                category = ImportantContact.CATEGORY_ADMINISTRATION,
                isEmergency = false
            ),
            ImportantContact(
                id = "default_hall_2",
                name = "Bijoy Dibos Hall",
                designation = "Provost & Residential Hall Administration",
                departmentOrOffice = "Residential Hall",
                phone = "+8801714706935",
                email = "bijoy.hall@gstu.ac.bd",
                category = ImportantContact.CATEGORY_ADMINISTRATION,
                isEmergency = false
            ),
            ImportantContact(
                id = "default_hall_3",
                name = "Bonolota Hall",
                designation = "Provost & Residential Hall Administration",
                departmentOrOffice = "Residential Hall",
                phone = "+8801722049300",
                email = "bonolota.hall@gstu.ac.bd",
                category = ImportantContact.CATEGORY_ADMINISTRATION,
                isEmergency = false
            ),
            ImportantContact(
                id = "default_hall_4",
                name = "Shaheed Titumir Hall",
                designation = "Provost & Residential Hall Administration",
                departmentOrOffice = "Residential Hall",
                phone = "+8801725158250",
                email = "titumir.hall@gstu.ac.bd",
                category = ImportantContact.CATEGORY_ADMINISTRATION,
                isEmergency = false
            ),
            ImportantContact(
                id = "default_hall_5",
                name = "Aparajita Hall",
                designation = "Provost & Residential Hall Administration",
                departmentOrOffice = "Residential Hall",
                phone = "+8801819116061",
                email = "aparajita.hall@gstu.ac.bd",
                category = ImportantContact.CATEGORY_ADMINISTRATION,
                isEmergency = false
            )
        )
    }
}
