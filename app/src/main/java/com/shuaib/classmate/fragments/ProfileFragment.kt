// C:/Users/USER/AndroidStudioProjects/ClassMate/app/src/main/java/com/shuaib/classmate/fragments/ProfileFragment.kt
package com.shuaib.classmate.fragments

import com.shuaib.classmate.BuildConfig
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.widget.TextView
import com.google.firebase.firestore.ListenerRegistration
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.onesignal.OneSignal
import com.shuaib.classmate.activities.LoginActivity
import com.shuaib.classmate.activities.EditProfileActivity
import com.shuaib.classmate.R
import com.shuaib.classmate.activities.AdminPanelActivity
import com.shuaib.classmate.activities.AiSettingsActivity
import com.shuaib.classmate.activities.UserManagementActivity
import com.shuaib.classmate.activities.MainActivity
import com.shuaib.classmate.activities.PostNoticeActivity
import com.shuaib.classmate.activities.ViewProfileActivity
import com.shuaib.classmate.adapters.PdfAdapter

import com.shuaib.classmate.adapters.SubjectAdapter
import com.shuaib.classmate.databinding.DialogEditProfileBinding
import com.shuaib.classmate.databinding.DialogRoleSwitcherBinding
import com.shuaib.classmate.databinding.FragmentProfileBinding
import com.shuaib.classmate.models.PdfFile
import com.shuaib.classmate.utils.HapticHelper
import com.shuaib.classmate.utils.UserRoleHelper

import com.shuaib.classmate.models.User
import com.shuaib.classmate.storage.LibraryUrlOpener
import com.shuaib.classmate.ui.GlowHelper
import com.shuaib.classmate.utils.AppPreferences
import com.shuaib.classmate.utils.CloudinaryUploader
import com.shuaib.classmate.utils.Subject
import com.shuaib.classmate.utils.SubjectList
import com.shuaib.classmate.utils.ThemeColors
import com.shuaib.classmate.utils.applyClickAnimation
import androidx.recyclerview.widget.LinearLayoutManager
import java.io.File
import kotlinx.coroutines.launch
import java.security.MessageDigest
import com.shuaib.classmate.utils.AppUpdateManager
import com.shuaib.classmate.utils.AppUpdateInfo
import com.shuaib.classmate.notices.NoticeTextFormatter
import com.shuaib.classmate.databinding.DialogAppUpdateBinding
import kotlinx.coroutines.Job

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private var currentUser: User? = null
    private var tempImageUri: Uri? = null
    private lateinit var subjectAdapter: SubjectAdapter
    private lateinit var pdfAdapter: PdfAdapter
    private var favoriteSubjects = emptyList<Subject>()
    private var favoritePdfIdsSet = emptySet<String>()
    private var pdfCounts = emptyMap<String, Int>()
    private var lastFetchedSubjects = emptyList<String>()
    private var lastFetchedPdfIds = emptyList<String>()
    private var isSubjectsExpanded = false
    private var isPdfsExpanded = false
    private var profileListener: ListenerRegistration? = null
    private var configListener: ListenerRegistration? = null
    private var isFriendsPublic = false

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { uploadProfilePicture(it) }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) tempImageUri?.let { uploadProfilePicture(it) }
    }

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            com.shuaib.classmate.services.ShakeToTorchService.start(requireContext())
        } else {
            Toast.makeText(context, "Camera permission is required to turn on the flashlight.", Toast.LENGTH_SHORT).show()
            binding.switchShakeToTorch.isChecked = false
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        setupDarkModeToggle()
        setupNotificationsToggle()
        setupAutoMuteToggle()
        setupShakeToTorchToggle()
        setupAiSettings()
        setupSavedResources()
        listenToFriendsConfig()
        fetchUserProfile()

        context?.let { ctx ->
            GlowHelper.pulseGlow(binding.profileBorder, ThemeColors.primary(ctx))
        }

        binding.fabEditPhoto.setOnClickListener {
            showPhotoOptions()
        }

        binding.cardSwitchRole.setOnClickListener {
            showRoleSwitcherDialog()
        }

        binding.tvRoleBadge.setOnClickListener {
            showRoleSwitcherDialog()
        }

        binding.btnHeaderEditProfile.setOnClickListener {
            HapticHelper.lightPop(it)
            startActivity(Intent(requireContext(), EditProfileActivity::class.java))
            requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        binding.btnHeaderShareCard.setOnClickListener {
            HapticHelper.lightPop(it)
            val activeUser = currentUser ?: auth.currentUser?.let { fbUser ->
                User(
                    uid = fbUser.uid,
                    name = fbUser.displayName ?: "",
                    fullName = fbUser.displayName ?: "",
                    email = fbUser.email ?: "",
                    photoUrl = fbUser.photoUrl?.toString() ?: ""
                )
            } ?: User(
                uid = "guest_user",
                name = "GSTU Member",
                email = "member@gstu.ac.bd"
            )
            com.shuaib.classmate.ui.GstuIdCardBottomSheet.show(childFragmentManager, activeUser)
        }

        binding.layoutViewProfile.setOnClickListener {
            HapticHelper.lightPop(it)
            startActivity(Intent(requireContext(), ViewProfileActivity::class.java))
            requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        binding.layoutSecurityInfo.applyClickAnimation {
            showSecurityInfoDialog()
        }

        binding.cardTeacherDirectory.applyClickAnimation {
            val args = android.os.Bundle().apply { putBoolean("isTeacherMode", true) }
            (activity as? MainActivity)?.openChildDestination(R.id.nav_profile, R.id.nav_friends, args)
        }

        binding.cardAdminPanel.applyClickAnimation {
            startActivity(Intent(requireContext(), AdminPanelActivity::class.java))
            requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        binding.layoutUserManagement.setOnClickListener {
            startActivity(Intent(requireContext(), UserManagementActivity::class.java))
            requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        binding.layoutAddWidget.setOnClickListener {
            val context = requireContext()
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(context)
                val myProvider = android.content.ComponentName(context, com.shuaib.classmate.widget.ClassMateWidget::class.java)

                if (appWidgetManager.isRequestPinAppWidgetSupported) {
                    val successCallback = android.app.PendingIntent.getBroadcast(
                        context, 0,
                        android.content.Intent(context, com.shuaib.classmate.widget.ClassMateWidget::class.java),
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                    )
                    appWidgetManager.requestPinAppWidget(myProvider, null, successCallback)
                    Toast.makeText(context, "Requesting to pin GSTU Go Widget...", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(
                        context,
                        "Launcher does not support automatic pinning. Long press home screen > Widgets > Find GSTU Go Widget",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } else {
                Toast.makeText(
                    context,
                    "Long press your home screen > Widgets > Find GSTU Go Widget",
                    Toast.LENGTH_LONG
                ).show()
            }
        }



        binding.btnLogout.applyClickAnimation {
            signOut()
        }

        binding.layoutDeleteOfflineCache.setOnClickListener {
            val context = requireContext()
            val offlineFiles = com.shuaib.classmate.storage.LibraryDownloadManager.getDownloadedFiles(context)
            if (offlineFiles.isEmpty()) {
                Toast.makeText(context, "No offline cache found", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            com.google.android.material.dialog.MaterialAlertDialogBuilder(context, R.style.Theme_ClassMate_Dialog)
                .setTitle("Delete Offline Cache")
                .setMessage("Are you sure you want to delete all offline PDF files? This will free up storage space.")
                .setPositiveButton("Delete") { _, _ ->
                    offlineFiles.forEach { pdf ->
                        com.shuaib.classmate.storage.LibraryDownloadManager.deleteDownload(context, pdf.id)
                    }
                    Toast.makeText(context, "Offline cache cleared", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }



        binding.switchFriendsPublic.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked == isFriendsPublic) return@setOnCheckedChangeListener
            firestore.collection("config").document("friends")
                .set(mapOf("isPublic" to isChecked))
                .addOnSuccessListener {
                    Toast.makeText(context, "Friends list visibility updated!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    if (_binding == null) return@addOnFailureListener
                    Toast.makeText(context, "Failed to update visibility: Please try again later.", Toast.LENGTH_SHORT).show()
                    binding.switchFriendsPublic.isChecked = isFriendsPublic
                }
        }
        
        setupAppVersion()
    }

    private fun setupAppVersion() {
        val currentVersion = "${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})"
        binding.tvAppVersion.text = currentVersion
        
        binding.layoutCheckUpdates.setOnClickListener {
            checkUpdatesManually()
        }
    }

    private fun checkUpdatesManually() {
        binding.pbCheckUpdates.visibility = View.VISIBLE
        binding.ivCheckUpdatesChevron.visibility = View.GONE
        binding.layoutCheckUpdates.isClickable = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val updateInfo = AppUpdateManager.checkLatestRelease()
                val isAvailable = AppUpdateManager.isUpdateAvailable(updateInfo.latestVersionName)
                
                binding.pbCheckUpdates.visibility = View.GONE
                binding.ivCheckUpdatesChevron.visibility = View.VISIBLE
                binding.layoutCheckUpdates.isClickable = true

                if (isAvailable) {
                    AppUpdateManager.showUpdateDialog(requireContext(), updateInfo, viewLifecycleOwner.lifecycleScope)
                } else {
                    Toast.makeText(requireContext(), "GSTU Go is up to date!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                binding.pbCheckUpdates.visibility = View.GONE
                binding.ivCheckUpdatesChevron.visibility = View.VISIBLE
                binding.layoutCheckUpdates.isClickable = true
                Toast.makeText(requireContext(), "Failed to check for updates: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun signOut() {
        val context = requireContext()
        val uid = auth.currentUser?.uid
        if (uid != null) {
            val updates = mapOf(
                "oneSignalPlayerId" to com.google.firebase.firestore.FieldValue.delete(),
                "onesignalPlayerId" to com.google.firebase.firestore.FieldValue.delete(),
                "playerId" to com.google.firebase.firestore.FieldValue.delete()
            )
            FirebaseFirestore.getInstance().document("users/$uid")
                .update(updates)
                .addOnFailureListener { e ->
                    android.util.Log.e("ProfileFragment", "Failed to clear push ID in Firestore: ${e.message}")
                }
        }
        
        try {
            OneSignal.User.pushSubscription.optOut()
            OneSignal.logout()
        } catch (e: Exception) {
            android.util.Log.e("ProfileFragment", "OneSignal signout failed: ${e.message}")
        }
        
        com.shuaib.classmate.chat.ChatRepository.close()
        try {
            com.google.firebase.messaging.FirebaseMessaging.getInstance()
                .unsubscribeFromTopic("notices")
            com.google.firebase.messaging.FirebaseMessaging.getInstance()
                .unsubscribeFromTopic("cancellations")
            android.util.Log.d("ProfileFragment", "Unsubscribed from FCM topics")
        } catch (e: Exception) {
            android.util.Log.e("ProfileFragment", "FCM unsubscribe failed: ${e.message}")
        }
        AppPreferences(context).setGuestMode(false)
        UserRoleHelper.clearTestRole(context)
        auth.signOut()
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
            } catch (_: Exception) {
            }
            val intent = Intent(context, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            requireActivity().finishAffinity()
        }
    }

    
    private fun updateUserInFirestore(data: Map<String, Any>) {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users").document(uid).update(data)
            .addOnSuccessListener {
                Toast.makeText(context, "Profile updated!", Toast.LENGTH_SHORT).show()
                fetchUserProfile()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Update failed: Please try again later.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showPhotoOptions() {
        val dialog = BottomSheetDialog(requireContext(), R.style.Theme_ClassMate_Dialog)
        val view = layoutInflater.inflate(R.layout.dialog_photo_options, null)

        view.findViewById<View>(R.id.btnCamera).setOnClickListener {
            dialog.dismiss()
            openCamera()
        }
        view.findViewById<View>(R.id.btnGallery).setOnClickListener {
            dialog.dismiss()
            galleryLauncher.launch("image/*")
        }

        dialog.setContentView(view)
        dialog.show()
    }

    private fun openCamera() {
        val photoFile = File(requireContext().cacheDir, "profile_temp.jpg")
        tempImageUri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", photoFile)
        cameraLauncher.launch(tempImageUri)
    }

    private fun setupDarkModeToggle() {
        val prefs = AppPreferences(requireContext())
        val isDarkMode = prefs.isDarkMode()
        binding.switchDarkMode.isChecked = isDarkMode

        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            if (prefs.isDarkMode() == isChecked) return@setOnCheckedChangeListener
            prefs.setDarkMode(isChecked)

            // Re-apply theme with a slight delay to allow the switch animation to finish
            // and avoid immediate activity recreation in the middle of a callback
            binding.root.postDelayed({
                AppCompatDelegate.setDefaultNightMode(
                    if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
                )
            }, 150)
        }
    }

    private fun setupNotificationsToggle() {
        val prefs = AppPreferences(requireContext())
        val isNotificationsEnabled = prefs.isNotificationsEnabled()
        binding.switchNotifications.isChecked = isNotificationsEnabled

        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            prefs.setNotificationsEnabled(isChecked)
            if (isChecked) OneSignal.User.pushSubscription.optIn()
            else OneSignal.User.pushSubscription.optOut()
        }
    }

    private fun setupAutoMuteToggle() {
        val prefs = AppPreferences(requireContext())
        binding.switchAutoMute.isChecked = prefs.isAutoMuteEnabled()

        binding.switchAutoMute.setOnCheckedChangeListener { _, isChecked ->
            prefs.setAutoMuteEnabled(isChecked)
            
            if (isChecked) {
                val notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
                val hasPolicyAccess = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    notificationManager?.isNotificationPolicyAccessGranted == true
                } else {
                    true
                }

                if (!hasPolicyAccess) {
                    showDndPermissionDialog()
                }
            }

            com.shuaib.classmate.services.AutoMuteScheduler.scheduleAlarms(requireContext())
        }
    }

    private fun showDndPermissionDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Silent Mode Access Required")
            .setMessage("To allow GSTU Go to shift your phone into complete Silent mode during class, Do Not Disturb access is required. If not granted, the app will fall back to Vibrate mode.")
            .setPositiveButton("Grant Access") { _, _ ->
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    val intent = Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                    startActivity(intent)
                }
            }
            .setNegativeButton("Use Vibrate Mode Only") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showSecurityInfoDialog() {
        val message = """
            🛡️ GSTU Go Security & Privacy Architecture:

            • Data in Transit: 100% of network traffic between your device and Google Cloud / Firebase Firestore is secured using TLS 1.3 encryption.

            • Data at Rest: All personal profiles, academic library files, and records are encrypted using 256-bit AES (AES-256).

            • Role-Based Access Control: Granular Firestore rules enforce verified roles (Super Admin, Dept Admin, Teacher, Club Admin, CR, Student) to protect records from unauthorized modification.

            • Direct Messaging Privacy: All messages are cloud-encrypted. The upcoming roadmap introduces device-to-device End-to-End Encryption (E2EE) with Android KeyStore backed private keys.

            Your data is strictly protected and dedicated to GSTU academic & campus community empowerment.
        """.trimIndent()

        MaterialAlertDialogBuilder(requireContext(), R.style.Theme_ClassMate_Dialog)
            .setTitle("Data Security & Privacy")
            .setMessage(message)
            .setPositiveButton("Understood", null)
            .show()
    }

    private fun setupAiSettings() {
        binding.cardAiSettings.isVisible = false
    }

    private fun setupSavedResources() {
        subjectAdapter = SubjectAdapter(emptyList()) { subject ->
            (activity as? MainActivity)?.openChildDestination(
                R.id.nav_pdf,
                R.id.fragment_subject_pdf_list,
                Bundle().apply {
                    putString("subjectName", subject.name)
                }
            )
        }
        binding.rvSavedResources.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = subjectAdapter
        }

        pdfAdapter = PdfAdapter(emptyList(), false, emptySet())
        pdfAdapter.onItemClick = { pdf -> LibraryUrlOpener.open(requireContext(), pdf) }
        pdfAdapter.onFavoriteClick = { pdf -> togglePdfFavorite(pdf) }
        binding.rvSavedFiles.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = pdfAdapter
        }

        binding.rowSavedSubjects.setOnClickListener {
            isSubjectsExpanded = !isSubjectsExpanded
            binding.containerSavedSubjects.visibility = if (isSubjectsExpanded) View.VISIBLE else View.GONE
            val rotation = if (isSubjectsExpanded) 90f else 0f
            binding.ivSavedSubjectsChevron.animate().rotation(rotation).setDuration(150).start()
        }

        binding.rowSavedPdfs.setOnClickListener {
            isPdfsExpanded = !isPdfsExpanded
            binding.containerSavedPdfs.visibility = if (isPdfsExpanded) View.VISIBLE else View.GONE
            val rotation = if (isPdfsExpanded) 90f else 0f
            binding.ivSavedPdfsChevron.animate().rotation(rotation).setDuration(150).start()
        }
    }

    private fun fetchSavedResources(favoriteNames: List<String>, favoritePdfIds: List<String>) {
        favoritePdfIdsSet = favoritePdfIds.toSet()

        val hasSubjects = favoriteNames.isNotEmpty()
        val hasPdfs = favoritePdfIds.isNotEmpty()
        
        val subjectsChanged = favoriteNames != lastFetchedSubjects
        val pdfsChanged = favoritePdfIds != lastFetchedPdfIds
        
        lastFetchedSubjects = favoriteNames
        lastFetchedPdfIds = favoritePdfIds

        if (hasSubjects || hasPdfs) {
            binding.savedResourcesTagSection.visibility = View.VISIBLE

            // Subjects Row Visibility
            binding.rowSavedSubjects.visibility = if (hasSubjects) View.VISIBLE else View.GONE
            binding.containerSavedSubjects.visibility = if (hasSubjects && isSubjectsExpanded) View.VISIBLE else View.GONE
            binding.tvSavedSubjectsTitle.text = "Saved Subjects (${favoriteNames.size})"
            binding.ivSavedSubjectsChevron.rotation = if (isSubjectsExpanded) 90f else 0f

            // PDFs Row Visibility
            binding.rowSavedPdfs.visibility = if (hasPdfs) View.VISIBLE else View.GONE
            binding.containerSavedPdfs.visibility = if (hasPdfs && isPdfsExpanded) View.VISIBLE else View.GONE
            binding.tvSavedPdfsTitle.text = "Saved PDFs (${favoritePdfIds.size})"
            binding.ivSavedPdfsChevron.rotation = if (isPdfsExpanded) 90f else 0f

            // Divider visibility
            binding.dividerSavedResources.visibility = if (hasSubjects && hasPdfs) View.VISIBLE else View.GONE
        } else {
            binding.savedResourcesTagSection.visibility = View.GONE
        }

        // Fetch Subjects
        if (hasSubjects) {
            favoriteSubjects = SubjectList.subjects.filter { it.name in favoriteNames }
            if (favoriteSubjects.isNotEmpty() && !hasPdfs) {
                updateSubjectCountsOnly()
            }
        }

        // Fetch Files
        if (hasPdfs) {
            fetchFavoritePdfs(favoritePdfIds)
        }
    }

    private fun updateSubjectCountsOnly() {
        firestore.collection("library_files")
            .whereEqualTo("isDeleted", false)
            .get()
            .addOnSuccessListener { snapshot ->
                if (_binding == null || !isAdded) return@addOnSuccessListener
                val files = snapshot.documents.map { doc -> doc.getString("subject") ?: "" }
                pdfCounts = files.groupBy { it }.mapValues { it.value.size }
                subjectAdapter.updateList(favoriteSubjects, pdfCounts)
            }
    }

    private fun fetchFavoritePdfs(favoritePdfIds: List<String>) {
        firestore.collection("library_files")
            .whereEqualTo("isDeleted", false)
            .get()
            .addOnSuccessListener { snapshot ->
                if (_binding == null || !isAdded) return@addOnSuccessListener
                val userBatch = context?.let { com.shuaib.classmate.utils.AppPreferences(it).getUserBatch() } ?: ""
                val userDeptId = context?.let { com.shuaib.classmate.utils.AppPreferences(it).getUserDepartmentId() } ?: ""
                val isSuperAdmin = currentUser?.isSuperAdmin() == true
                val allFiles = snapshot.documents.map { doc -> doc.toPdfFile() }
                    .filter { it.canUserAccess(userDeptId, userBatch, isSuperAdmin) }
                pdfCounts = allFiles.groupBy { it.subject }.mapValues { it.value.size }

                val favoritePdfs = allFiles.filter { it.id in favoritePdfIds }

                if (favoriteSubjects.isNotEmpty()) {
                    subjectAdapter.updateList(favoriteSubjects, pdfCounts)
                }

                if (favoritePdfs.isNotEmpty()) {
                    pdfAdapter.updateList(favoritePdfs, favoritePdfIdsSet)
                    binding.tvSavedPdfsTitle.text = "Saved PDFs (${favoritePdfs.size})"
                }
            }
    }

    private fun togglePdfFavorite(pdf: PdfFile) {
        val uid = auth.currentUser?.uid ?: return
        val isNowFavorite = !favoritePdfIdsSet.contains(pdf.id)
        val wasFavoritePdfIds = favoritePdfIdsSet

        if (isNowFavorite) {
            favoritePdfIdsSet = favoritePdfIdsSet + pdf.id
            firestore.collection("users").document(uid)
                .update("favoritePdfIds", com.google.firebase.firestore.FieldValue.arrayUnion(pdf.id))
                .addOnFailureListener {
                    favoritePdfIdsSet = wasFavoritePdfIds
                    android.util.Log.e("ProfileFragment", "Save favorite failed", it)
                    Toast.makeText(context, "Save failed. Please try again.", Toast.LENGTH_SHORT).show()
                }
        } else {
            favoritePdfIdsSet = favoritePdfIdsSet - pdf.id
            firestore.collection("users").document(uid)
                .update("favoritePdfIds", com.google.firebase.firestore.FieldValue.arrayRemove(pdf.id))
                .addOnFailureListener {
                    favoritePdfIdsSet = wasFavoritePdfIds
                    android.util.Log.e("ProfileFragment", "Remove favorite failed", it)
                    Toast.makeText(context, "Remove failed. Please try again.", Toast.LENGTH_SHORT).show()
                }
        }

        currentUser?.let { fetchUserFavorites(it.favoriteSubjects, favoritePdfIdsSet.toList()) }
    }

    private fun fetchUserProfile() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            val safeCtx = context ?: return
            val effectiveRole = UserRoleHelper.getEffectiveRole(safeCtx, "student")
            val guestUser = User(
                uid = "guest_user",
                name = "Guest User",
                email = "guest@gstu.ac.bd",
                role = effectiveRole,
                approved = true,
                permissions = User.getRoleDefaultPermissions(effectiveRole)
            )
            currentUser = guestUser
            updateUI(guestUser)
            return
        }
        profileListener?.remove()
        profileListener = firestore.collection("users").document(uid)
            .addSnapshotListener { document, error ->
                if (_binding == null || !isAdded) return@addSnapshotListener
                if (error != null) {
                    android.util.Log.e("ProfileFragment", "Error listening to profile", error)
                    return@addSnapshotListener
                }
                if (document != null && document.exists()) {
                    try {
                        val approvedVal = document.get("approved")
                        val isApproved = approvedVal == true || approvedVal == "true"
                        val ctx = context ?: return@addSnapshotListener
                        val docRole = document.getString("role") ?: "student"
                        val localDemoRole = AppPreferences(ctx).getDemoRole()
                        val firestoreDemoRole = document.getString("demoRole") ?: ""
                        val effectiveDemoRole = localDemoRole.ifBlank { firestoreDemoRole }
                        val candidateRole = effectiveDemoRole.ifBlank { docRole }
                        val effectiveRole = UserRoleHelper.getEffectiveRole(ctx, candidateRole)
                        val effectivePermissions = UserRoleHelper.getEffectivePermissions(ctx, effectiveRole, document.get("permissions"))
                        
                        val user = User(
                            uid = document.id,
                            name = document.getString("name") ?: "",
                            fullName = document.getString("fullName") ?: "",
                            studentId = document.getString("studentId") ?: "",
                            batch = document.getString("batch") ?: "",
                            teacherCode = document.getString("teacherCode") ?: "",
                            department = document.getString("department") ?: "",
                            email = document.getString("email") ?: "",
                            phone = document.getString("phone") ?: "",
                            bloodGroup = document.getString("bloodGroup") ?: "",
                            homeDistrict = document.getString("homeDistrict") ?: "",
                            address = document.getString("address") ?: "",
                            role = effectiveRole,
                            permissions = effectivePermissions,
                            approved = if (UserRoleHelper.isTestRoleActive(ctx)) true else isApproved,
                            photoUrl = document.getString("photoUrl") ?: "",
                            authProvider = document.getString("authProvider") ?: "",
                            oneSignalPlayerId = document.getString("oneSignalPlayerId") ?: "",
                            favoriteSubjects = document.get("favoriteSubjects") as? List<String> ?: emptyList(),
                            favoritePdfIds = document.get("favoritePdfIds") as? List<String> ?: emptyList(),
                            assignedClubId = document.getString("assignedClubId") ?: (if (effectiveDemoRole == "club_admin") "club_computer_society" else ""),
                            assignedClubName = document.getString("assignedClubName") ?: (if (effectiveDemoRole == "club_admin") "GSTU Computer Club (GSTUCC)" else ""),
                            demoRole = effectiveDemoRole,
                            isVerified = document.getBoolean("isVerified") == true || (document.getString("email") ?: "").trim().lowercase().let { it.endsWith(".edu.bd") || it.endsWith("@gstu.ac.bd") || it.contains(".gstu.ac.bd") }
                        )
                        
                        currentUser = user
                        updateUI(user)
                        _binding?.let { activeBinding ->
                            loadProfilePicture(user.email, user.photoUrl, activeBinding.ivProfile)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("ProfileFragment", "Error parsing User object: ${e.message}")
                    }
                }
            }
    }

    private fun applyRoleBadge(textView: TextView, role: String) {
        val (fill, label) = when (role.lowercase()) {
            "superadmin"                -> "#E11D48" to "SUPER ADMIN"
            "admin", "university_admin" -> "#D97706" to "ADMIN"
            "teacher"                   -> "#2563EB" to "TEACHER"
            "club_admin"                -> "#7C3AED" to "CLUB ADMIN"
            "cr"                        -> "#059669" to "CLASS REP (CR)"
            "student"                   -> "#475569" to "STUDENT"
            else                        -> "#34D399" to role.uppercase()
        }
        textView.text = label
        textView.setTextColor(Color.WHITE)
        textView.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 999f
            setColor(Color.parseColor(fill))
        }
    }

    private fun updateUI(user: User) {
        if (_binding == null || !isAdded) return
        val activeRole = user.activeRole.lowercase()

        val resolvedName = user.name.ifBlank {
            user.fullName.ifBlank {
                auth.currentUser?.displayName?.ifBlank { null }
                    ?: user.email.substringBefore("@").replace(".", " ").split(" ")
                        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            }
        }
        binding.tvProfileName.text = resolvedName.ifBlank { "GSTU Member" }
        val isVerified = user.isBlueVerified
        binding.ivVerifiedBadge.visibility = if (isVerified) View.VISIBLE else View.GONE
        binding.ivVerifiedBadge.setOnClickListener {
            val verifyReason = when {
                user.hasEduMail() -> "Verified via Institutional Email (${user.email})"
                user.isSuperAdmin() -> "Verified System Super Administrator"
                user.isUniversityAdmin() -> "Verified University Administrator"
                else -> "Official GSTU Go Verified Account"
            }
            Toast.makeText(requireContext(), "✅ $verifyReason", Toast.LENGTH_SHORT).show()
        }
        applyRoleBadge(binding.tvRoleBadge, user.role)

        // Update role testing card
        val roleKey = user.role.lowercase()
        val (roleColorHex, rolePillText, roleIconRes) = when (roleKey) {
            "superadmin" -> Triple("#E11D48", "SUPER ADMIN", R.drawable.ic_shield_check)
            "admin"      -> Triple("#D97706", "DEPT ADMIN", R.drawable.ic_building)
            "teacher"    -> Triple("#2563EB", "TEACHER", R.drawable.ic_person)
            "club_admin" -> Triple("#7C3AED", "CLUB ADMIN", R.drawable.ic_trophy)
            "cr"         -> Triple("#059669", "CLASS REP (CR)", R.drawable.ic_badge)
            else         -> Triple("#475569", "STUDENT", R.drawable.ic_users)
        }
        val roleColorParsed = Color.parseColor(roleColorHex)
        binding.tvCurrentRolePill.text = rolePillText
        binding.tvCurrentRolePill.backgroundTintList = ColorStateList.valueOf(roleColorParsed)
        val alphaColor = Color.argb(35, Color.red(roleColorParsed), Color.green(roleColorParsed), Color.blue(roleColorParsed))
        binding.containerRoleIcon.backgroundTintList = ColorStateList.valueOf(alphaColor)
        binding.ivCurrentRoleIcon.setImageResource(roleIconRes)
        binding.ivCurrentRoleIcon.imageTintList = ColorStateList.valueOf(roleColorParsed)
        binding.tvSwitchRoleDesc.text = "Currently testing as ${user.getRoleDisplayName()}. Tap to change."

        fetchUserFavorites(user.favoriteSubjects, user.favoritePdfIds)

        // Update summary info under Personal Information
        binding.tvUserSubInfo.text = if (!user.studentId.isNullOrEmpty()) {
            "${user.studentId} - ${user.email}"
        } else {
            user.email
        }

        // Update header metadata pills
        val deptDisplay = if (user.department.isNotBlank()) user.department else "GSTU"
        binding.tvHeaderDeptPill.text = deptDisplay
        binding.tvHeaderDeptPill.isVisible = true

        val batchDisplay = if (user.batch.isNotBlank()) "Batch ${user.batch}" else if (user.role.equals("teacher", true)) "Faculty" else ""
        if (batchDisplay.isNotBlank()) {
            binding.tvHeaderBatchPill.text = batchDisplay
            binding.tvHeaderBatchPill.isVisible = true
        } else {
            binding.tvHeaderBatchPill.isVisible = false
        }

        val isAdmin = user.isAdmin()
        binding.adminSection.isVisible = isAdmin

        val canManageUsers = user.canManageUsers()
        binding.layoutUserManagement.isVisible = canManageUsers
        binding.dividerUserManagement.isVisible = canManageUsers

        binding.dividerFriendsToggle.isVisible = false
        binding.layoutFriendsToggle.isVisible = false
    }

    private fun loadProfilePicture(email: String, customUrl: String?, imageView: ImageView) {
        if (!isAdded) return
        val emailHash = MessageDigest.getInstance("MD5")
            .digest(email.trim().lowercase().toByteArray())
            .joinToString("") { "%02x".format(it) }
        val gravatarUrl = "https://www.gravatar.com/avatar/$emailHash?s=200&d=identicon"
        val url = if (!customUrl.isNullOrEmpty()) customUrl else gravatarUrl

        Glide.with(this)
            .load(url)
            .circleCrop()
            .placeholder(R.drawable.ic_default_avatar)
            .into(imageView)
    }

    private fun uploadProfilePicture(uri: Uri) {
        val uid = auth.currentUser?.uid ?: return
        binding.pbUpload.isVisible = true
        binding.fabEditPhoto.isEnabled = false

        CloudinaryUploader.uploadImage(
            context = requireContext(),
            fileUri = uri,
            folder = "profile_pictures",
            onSuccess = { url, _ ->
                firestore.collection("users").document(uid)
                    .update("photoUrl", url)
                    .addOnSuccessListener {
                        if (_binding == null || !isAdded) return@addOnSuccessListener
                        binding.pbUpload.isVisible = false
                        binding.fabEditPhoto.isEnabled = true
                        fetchUserProfile()
                        Toast.makeText(context, "Profile picture updated!", Toast.LENGTH_SHORT).show()
                    }
            },
            onFailure = { error ->
                if (_binding == null || !isAdded) return@uploadImage
                binding.pbUpload.isVisible = false
                binding.fabEditPhoto.isEnabled = true
                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun fetchUserFavorites(favoriteNames: List<String>, favoritePdfIds: List<String>) {
        fetchSavedResources(favoriteNames, favoritePdfIds)
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toPdfFile(): PdfFile = PdfFile.fromDocument(this)

    private fun listenToFriendsConfig() {
        configListener?.remove()
        configListener = firestore.collection("config").document("friends")
            .addSnapshotListener { document, error ->
                if (_binding == null || !isAdded) return@addSnapshotListener
                if (error != null) {
                    android.util.Log.e("ProfileFragment", "Error listening to config/friends", error)
                    return@addSnapshotListener
                }

                isFriendsPublic = document?.getBoolean("isPublic") ?: false
                binding.switchFriendsPublic.isChecked = isFriendsPublic
            }
    }

    override fun onResume() {
        super.onResume()
    }

    private fun setupShakeToTorchToggle() {
        val prefs = AppPreferences(requireContext())
        binding.switchShakeToTorch.isChecked = prefs.isShakeToTorchEnabled()

        binding.switchShakeToTorch.setOnCheckedChangeListener { _, isChecked ->
            prefs.setShakeToTorchEnabled(isChecked)
            if (isChecked) {
                val hasCameraPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.CAMERA
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                
                if (hasCameraPermission) {
                    com.shuaib.classmate.services.ShakeToTorchService.start(requireContext())
                } else {
                    requestCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                }
            } else {
                com.shuaib.classmate.services.ShakeToTorchService.stop(requireContext())
            }
        }
    }

    private fun showRoleSwitcherDialog() {
        if (!isAdded || context == null) return
        val ctx = requireContext()
        val dialog = BottomSheetDialog(ctx)
        val dialogView = layoutInflater.inflate(R.layout.dialog_switch_role, null)
        dialog.setContentView(dialogView)

        val currentRole = UserRoleHelper.getEffectiveRole(ctx, currentUser?.role)

        val itemSuperAdmin = dialogView.findViewById<View>(R.id.itemRoleSuperAdmin)
        val itemAdmin = dialogView.findViewById<View>(R.id.itemRoleAdmin)
        val itemTeacher = dialogView.findViewById<View>(R.id.itemRoleTeacher)
        val itemClubAdmin = dialogView.findViewById<View>(R.id.itemRoleClubAdmin)
        val itemCR = dialogView.findViewById<View>(R.id.itemRoleCR)
        val itemStudent = dialogView.findViewById<View>(R.id.itemRoleStudent)

        val checkSuperAdmin = dialogView.findViewById<View>(R.id.ivCheckSuperAdmin)
        val checkAdmin = dialogView.findViewById<View>(R.id.ivCheckAdmin)
        val checkTeacher = dialogView.findViewById<View>(R.id.ivCheckTeacher)
        val checkClubAdmin = dialogView.findViewById<View>(R.id.ivCheckClubAdmin)
        val checkCR = dialogView.findViewById<View>(R.id.ivCheckCR)
        val checkStudent = dialogView.findViewById<View>(R.id.ivCheckStudent)

        val btnClose = dialogView.findViewById<android.widget.ImageButton>(R.id.btnCloseRoleDialog)
        btnClose?.setOnClickListener { dialog.dismiss() }

        // Highlight currently active role
        checkSuperAdmin?.isVisible = currentRole == "superadmin"
        checkAdmin?.isVisible = currentRole == "admin"
        checkTeacher?.isVisible = currentRole == "teacher"
        checkClubAdmin?.isVisible = currentRole == "club_admin"
        checkCR?.isVisible = currentRole == "cr"
        checkStudent?.isVisible = currentRole == "student" || currentRole.isBlank()

        fun performRoleSwitch(newRole: String, roleTitle: String) {
            val safeCtx = context ?: return

            // 1. Instantly set test role in preferences
            UserRoleHelper.setTestRole(safeCtx, newRole)

            // 2. Instantly update currentUser in memory and refresh UI
            val perms = User.getRoleDefaultPermissions(newRole)
            val updatedUser = (currentUser ?: User(
                uid = auth.currentUser?.uid ?: "test_user",
                name = auth.currentUser?.displayName ?: "User",
                email = auth.currentUser?.email ?: ""
            )).copy(
                role = newRole,
                permissions = perms,
                approved = true
            )
            currentUser = updatedUser
            updateUI(updatedUser)

            // 3. Dismiss dialog immediately so user experiences instant response
            dialog.dismiss()

            Toast.makeText(
                safeCtx,
                "Switched to $roleTitle! Active for all app features.",
                Toast.LENGTH_SHORT
            ).show()

            // 4. Try updating Firestore in background (best-effort)
            val uid = auth.currentUser?.uid
            if (!uid.isNullOrBlank()) {
                val updates = mapOf(
                    "role" to newRole,
                    "approved" to true,
                    "permissions" to perms,
                    "updatedAt" to com.google.firebase.Timestamp.now()
                )
                firestore.collection("users").document(uid).update(updates)
                    .addOnFailureListener { e ->
                        android.util.Log.w("ProfileFragment", "Remote Firestore role update skipped: ${e.message}")
                    }
            }
        }

        // Helper to bind clicks recursively on the card and its container
        fun bindCard(card: View?, newRole: String, roleTitle: String) {
            if (card == null) return
            val listener = View.OnClickListener { performRoleSwitch(newRole, roleTitle) }
            card.setOnClickListener(listener)
            if (card is ViewGroup) {
                for (i in 0 until card.childCount) {
                    val child = card.getChildAt(i)
                    child.setOnClickListener(listener)
                    if (child is ViewGroup) {
                        for (j in 0 until child.childCount) {
                            child.getChildAt(j).setOnClickListener(listener)
                        }
                    }
                }
            }
        }

        bindCard(itemSuperAdmin, "superadmin", "Super Admin")
        bindCard(itemAdmin, "admin", "Dept Admin")
        bindCard(itemTeacher, "teacher", "Teacher")
        bindCard(itemClubAdmin, "club_admin", "Club Admin")
        bindCard(itemCR, "cr", "Class Representative (CR)")
        bindCard(itemStudent, "student", "Student")

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        profileListener?.remove()
        configListener?.remove()
        _binding = null
    }
}
