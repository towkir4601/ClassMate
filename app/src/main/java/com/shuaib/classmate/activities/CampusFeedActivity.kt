package com.shuaib.classmate.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.shuaib.classmate.R
import com.shuaib.classmate.adapters.CampusFeedAdapter
import com.shuaib.classmate.adapters.CampusFeedCommentAdapter
import com.shuaib.classmate.databinding.ActivityCampusFeedBinding
import com.shuaib.classmate.databinding.DialogDropComposerBinding
import com.shuaib.classmate.models.CampusFeedComment
import com.shuaib.classmate.models.CampusFeedPost
import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import com.shuaib.classmate.databinding.ItemPollComposerOptionBinding
import com.shuaib.classmate.repository.CampusFeedRepository
import com.shuaib.classmate.utils.AppPreferences
import com.shuaib.classmate.utils.CloudinaryUploader
import com.shuaib.classmate.utils.UserRoleHelper
import kotlinx.coroutines.launch

/**
 * GSTU Go - CampusFeed Activity
 * Cool sky-white theme campus public square with "For You" & "Latest" streams.
 * Closed Beta preview for Superadmin and other Admin levels.
 */
class CampusFeedActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCampusFeedBinding
    private val repo = CampusFeedRepository()
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private var currentUserId = ""
    private var currentUserName = "Student"
    private var currentUserHandle = "@student"
    private var currentUserAvatar = ""
    private var currentUserRole = "student"
    private var currentUserDepartment = ""
    private var currentUserBatch = ""

    private lateinit var adapter: CampusFeedAdapter
    private var feedListener: ListenerRegistration? = null
    private var selectedTab = TAB_FOR_YOU

    private var selectedImageUri: Uri? = null
    private var composerImagePreview: ImageView? = null
    private var composerPreviewLayout: View? = null

    // Image Picker for Drop Composer
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                selectedImageUri = uri
                composerPreviewLayout?.isVisible = true
                composerImagePreview?.let {
                    Glide.with(this).load(uri).centerCrop().into(it)
                }
            }
        }
    }

    companion object {
        const val TAB_FOR_YOU = CampusFeedRepository.TAB_FOR_YOU
        const val TAB_LATEST = CampusFeedRepository.TAB_LATEST
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Beta Security Guard: Only Superadmin and other Admin levels can access Live CampusFeed
        val role = UserRoleHelper.getEffectiveRole(this)
        val perms = UserRoleHelper.getEffectivePermissions(this, role)
        val r = role.trim().lowercase()
        val isAdmin = (r == "superadmin" || r == "admin" || r == "teacher" || r == "club_admin" || r == "cr" || perms.values.any { it == true })

        if (!isAdmin) {
            Toast.makeText(this, "CampusFeed is currently in closed beta for admins.", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, CampusFeedFeaturesActivity::class.java))
            finish()
            return
        }

        binding = ActivityCampusFeedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadCurrentUserInfo()
        setupRecyclerView()
        setupTabs()
        setupListeners()
        loadFeed(selectedTab)
    }

    override fun onDestroy() {
        super.onDestroy()
        feedListener?.remove()
    }

    private fun loadCurrentUserInfo() {
        val prefs = AppPreferences(this)
        val prefName = prefs.getUserName()
        val prefRole = prefs.getUserRole()
        val prefBatch = prefs.getUserBatch()

        val sp = getSharedPreferences("classmate_prefs", Context.MODE_PRIVATE)
        val prefDept = sp.getString("user_department", "") ?: ""
        val prefAvatar = sp.getString("user_photo_url", "") ?: sp.getString("user_avatar", "") ?: ""

        if (prefName.isNotBlank()) currentUserName = prefName
        if (prefRole.isNotBlank()) currentUserRole = prefRole
        if (prefDept.isNotBlank()) currentUserDepartment = prefDept
        if (prefBatch.isNotBlank()) currentUserBatch = prefBatch
        if (prefAvatar.isNotBlank()) currentUserAvatar = prefAvatar

        val user = auth.currentUser
        if (user != null) {
            currentUserId = user.uid
            if (currentUserName == "Student") {
                currentUserName = user.displayName ?: "Student"
            }
            currentUserHandle = "@" + (user.email?.substringBefore("@") ?: "student")

            db.collection("users").document(currentUserId).get()
                .addOnSuccessListener { doc ->
                    if (!doc.exists()) return@addOnSuccessListener
                    currentUserName = doc.getString("fullName") ?: doc.getString("name") ?: currentUserName
                    currentUserAvatar = doc.getString("photoUrl").orEmpty()
                    currentUserRole = doc.getString("role") ?: "student"
                    currentUserDepartment = doc.getString("department").orEmpty()
                    currentUserBatch = doc.getString("batch").orEmpty()

                    val clean = currentUserName.lowercase().replace(" ", "_").filter { it.isLetterOrDigit() || it == '_' }
                    currentUserHandle = if (clean.isNotBlank()) "@$clean" else "@student"
                }
        } else {
            // Guest or non-logged-in session fallback: ensure non-empty authorId so writes never fail
            currentUserId = prefs.getOrCreateGuestId()
            if (currentUserName == "Student") currentUserName = "GSTU Student"
            currentUserHandle = "@student"
            currentUserRole = "guest"
        }

        com.shuaib.classmate.repository.CampusFeedBookmarkManager.fetchCloudBookmarks(currentUserId, this)
    }

    private fun setupRecyclerView() {
        adapter = CampusFeedAdapter(
            currentUserId = currentUserId,
            currentUserName = currentUserName,
            onLoveClick = { post, willLove ->
                repo.toggleLove(post.id, currentUserId, willLove)
            },
            onPollVote = { post, selectedOption ->
                repo.votePoll(post.id, currentUserId, selectedOption)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Vote recorded!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Vote recorded!", Toast.LENGTH_SHORT).show()
                    }
            },
            onPetitionSign = { post ->
                repo.signPetition(post.id, currentUserId)
                    .addOnSuccessListener {
                        Toast.makeText(this, "✍️ Petition signed! Thank you for standing with fellow students.", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "✍️ Petition signed!", Toast.LENGTH_SHORT).show()
                    }
            },
            onCommentClick = { post ->
                showCommentsBottomSheet(post)
            },
            onShareClick = { post ->
                sharePost(post)
            },
            onReportClick = { post ->
                showReportDialog(post)
            }
        )

        binding.rvCampusFeed.layoutManager = LinearLayoutManager(this)
        binding.rvCampusFeed.adapter = adapter
    }

    private fun setupTabs() {
        binding.tabForYou.setOnClickListener { switchTab(TAB_FOR_YOU) }
        binding.tabLatest.setOnClickListener { switchTab(TAB_LATEST) }
        updateTabStyles(selectedTab)
    }

    private fun switchTab(tab: Int) {
        if (selectedTab == tab) return
        selectedTab = tab
        updateTabStyles(tab)
        loadFeed(tab)
    }

    private fun updateTabStyles(tab: Int) {
        val activeBg = R.drawable.bg_campus_sky_tab_active
        val inactiveBg = R.drawable.bg_campus_sky_tab_inactive
        val skyPrimary = Color.parseColor("#0284C7")
        val muted = Color.parseColor("#64748B")

        binding.tabForYou.setBackgroundResource(if (tab == TAB_FOR_YOU) activeBg else inactiveBg)
        binding.tabForYou.setTextColor(if (tab == TAB_FOR_YOU) skyPrimary else muted)

        binding.tabLatest.setBackgroundResource(if (tab == TAB_LATEST) activeBg else inactiveBg)
        binding.tabLatest.setTextColor(if (tab == TAB_LATEST) skyPrimary else muted)
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }

        // Enforce pure white color on the post FAB icon
        binding.fabNewDrop.setColorFilter(Color.WHITE)
        binding.fabNewDrop.imageTintList = ColorStateList.valueOf(Color.WHITE)

        binding.swipeRefreshFeed.setOnRefreshListener {
            loadFeed(selectedTab)
        }

        binding.fabNewDrop.setOnClickListener {
            // macOS-style tactile spring scale bounce before launching the window
            it.animate()
                .scaleX(0.82f).scaleY(0.82f)
                .setDuration(120)
                .withEndAction {
                    it.animate()
                        .scaleX(1.0f).scaleY(1.0f)
                        .setDuration(180)
                        .start()
                    showDropComposerDialog()
                }
                .start()
        }
    }

    private fun loadFeed(tab: Int) {
        feedListener?.remove()
        binding.pbFeedLoading.isVisible = true
        binding.layoutEmptyFeed.isVisible = false

        // Load immediate local cache so UI is instantaneous
        val immediatePosts = repo.getLocalPosts(tab, currentUserDepartment, currentUserBatch, currentUserId)
        if (immediatePosts.isNotEmpty()) {
            adapter.submitList(immediatePosts)
            binding.layoutEmptyFeed.isVisible = false
        }

        val query = if (tab == TAB_FOR_YOU) {
            repo.getForYouQuery()
        } else {
            repo.getLatestQuery()
        }

        feedListener = query.addSnapshotListener { snapshot, error ->
            binding.pbFeedLoading.isVisible = false
            binding.swipeRefreshFeed.isRefreshing = false

            if (error != null) {
                // If composite index is pending or firestore rules deny, fallback to simple query or local posts
                repo.getSimpleFeedQuery().get().addOnSuccessListener { fbSnap ->
                    binding.pbFeedLoading.isVisible = false
                    binding.swipeRefreshFeed.isRefreshing = false
                    val remotePosts = fbSnap.documents.mapNotNull { doc ->
                        doc.toObject(CampusFeedPost::class.java)?.copy(id = doc.id)
                    }
                    val merged = repo.mergeWithLocal(remotePosts, tab, currentUserDepartment, currentUserBatch, currentUserId)
                    adapter.submitList(merged)
                    binding.layoutEmptyFeed.isVisible = merged.isEmpty()
                }.addOnFailureListener {
                    // Local fallback
                    binding.pbFeedLoading.isVisible = false
                    binding.swipeRefreshFeed.isRefreshing = false
                    val fallback = repo.getLocalPosts(tab, currentUserDepartment, currentUserBatch, currentUserId)
                    adapter.submitList(fallback)
                    binding.layoutEmptyFeed.isVisible = fallback.isEmpty()
                }
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val remotePosts = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(CampusFeedPost::class.java)?.copy(id = doc.id)
                }

                val merged = repo.mergeWithLocal(remotePosts, tab, currentUserDepartment, currentUserBatch, currentUserId)
                adapter.submitList(merged)
                binding.layoutEmptyFeed.isVisible = merged.isEmpty()

                // Record impressions/views for visible posts
                merged.forEach { post ->
                    repo.recordView(post.id, currentUserId)
                }
            }
        }
    }

    // --- Floating Post Composer ---
    private fun showDropComposerDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val b = DialogDropComposerBinding.inflate(layoutInflater)
        dialog.setContentView(b.root)

        // Float with gap above and below (70% screen height), and smooth animation expanding from FAB region
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val width = (resources.displayMetrics.widthPixels * 0.94).toInt()
            val height = (resources.displayMetrics.heightPixels * 0.70).toInt()
            setLayout(width, height)
            setGravity(Gravity.CENTER)
            attributes?.windowAnimations = R.style.FloatingPostDialogAnimation
        }

        var selectedDropType = "thought"
        selectedImageUri = null
        composerImagePreview = b.ivComposerPreview
        composerPreviewLayout = b.layoutImagePreview

        // Character counter
        b.etDropContent.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val count = s?.length ?: 0
                b.tvCharacterCount.text = "$count/500"
                if (count > 500) {
                    b.tvCharacterCount.setTextColor(Color.RED)
                } else {
                    b.tvCharacterCount.setTextColor(Color.parseColor("#64748B"))
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Toggleable Feature Chips: Poll & Petition below text box (one-click select, another click deselect)
        fun updateToggleStyles() {
            val sel = R.drawable.bg_campus_sky_tab_active
            val unsel = R.drawable.bg_campus_sky_tab_inactive
            val skyPrimary = Color.parseColor("#0284C7")
            val muted = Color.parseColor("#64748B")

            val isPoll = (selectedDropType == "poll")
            val isPetition = (selectedDropType == "petition")

            b.chipTypePoll.setBackgroundResource(if (isPoll) sel else unsel)
            b.chipTypePoll.setTextColor(if (isPoll) skyPrimary else muted)
            b.chipTypePoll.setTypeface(null, if (isPoll) Typeface.BOLD else Typeface.NORMAL)

            b.chipTypePetition.setBackgroundResource(if (isPetition) sel else unsel)
            b.chipTypePetition.setTextColor(if (isPetition) skyPrimary else muted)
            b.chipTypePetition.setTypeface(null, if (isPetition) Typeface.BOLD else Typeface.NORMAL)

            b.sectionPollConfig.isVisible = isPoll
            b.sectionPetitionConfig.isVisible = isPetition
        }

        b.chipTypePoll.setOnClickListener {
            selectedDropType = if (selectedDropType == "poll") "thought" else "poll"
            updateToggleStyles()
        }

        b.chipTypePetition.setOnClickListener {
            selectedDropType = if (selectedDropType == "petition") "thought" else "petition"
            updateToggleStyles()
        }

        updateToggleStyles()

        // Dynamic Poll Options: "+ Add option"
        val extraOptionViews = mutableListOf<View>()
        b.btnPollAddOption.setOnClickListener {
            if (extraOptionViews.size >= 4) { // Max 6 total options (2 base + 4 extra)
                Toast.makeText(this, "Maximum 6 options allowed", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val optBinding = ItemPollComposerOptionBinding.inflate(layoutInflater, b.layoutPollExtraOptions, false)
            val optIndex = extraOptionViews.size + 3
            optBinding.etExtraOption.hint = "Option $optIndex"
            val rowView = optBinding.root
            optBinding.btnRemoveExtraOption.setOnClickListener {
                b.layoutPollExtraOptions.removeView(rowView)
                extraOptionViews.remove(rowView)
                b.btnPollAddOption.isVisible = true
            }
            b.layoutPollExtraOptions.addView(rowView)
            extraOptionViews.add(rowView)
            if (extraOptionViews.size >= 4) {
                b.btnPollAddOption.isVisible = false
            }
        }

        // Petition Quick Goal Chips
        fun selectGoal(goal: Int) {
            b.etPetitionTarget.setText("$goal")
            val sel = R.drawable.bg_campus_sky_tab_active
            val unsel = R.drawable.bg_campus_sky_tab_inactive
            val skyPrimary = Color.parseColor("#0284C7")
            val muted = Color.parseColor("#64748B")

            b.chipGoal50.setBackgroundResource(if (goal == 50) sel else unsel)
            b.chipGoal50.setTextColor(if (goal == 50) skyPrimary else muted)

            b.chipGoal100.setBackgroundResource(if (goal == 100) sel else unsel)
            b.chipGoal100.setTextColor(if (goal == 100) skyPrimary else muted)

            b.chipGoal250.setBackgroundResource(if (goal == 250) sel else unsel)
            b.chipGoal250.setTextColor(if (goal == 250) skyPrimary else muted)

            b.chipGoal500.setBackgroundResource(if (goal == 500) sel else unsel)
            b.chipGoal500.setTextColor(if (goal == 500) skyPrimary else muted)
        }

        b.chipGoal50.setOnClickListener { selectGoal(50) }
        b.chipGoal100.setOnClickListener { selectGoal(100) }
        b.chipGoal250.setOnClickListener { selectGoal(250) }
        b.chipGoal500.setOnClickListener { selectGoal(500) }

        // Attach Image
        b.btnAttachImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
            imagePickerLauncher.launch(intent)
        }

        b.btnAttachCamera.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
            imagePickerLauncher.launch(intent)
        }

        b.btnRemoveImage.setOnClickListener {
            selectedImageUri = null
            b.layoutImagePreview.isVisible = false
        }

        b.btnCancelDrop.setOnClickListener {
            dialog.dismiss()
        }

        // Submit Post
        b.btnSubmitDrop.setOnClickListener {
            val content = b.etDropContent.text.toString().trim()
            if (content.isBlank() && selectedDropType == "thought") {
                Toast.makeText(this, "Please write something to post!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Extract hashtags
            val hashtags = Regex("#(\\w+)").findAll(content).map { it.value }.toList()

            // Validate post-specific inputs
            val pollOptions = mutableListOf<String>()
            var petitionTitle = ""
            var petitionAddressedTo = ""
            var petitionGoal = 100

            when (selectedDropType) {
                "poll" -> {
                    val opt1 = b.etPollOption1.text.toString().trim()
                    val opt2 = b.etPollOption2.text.toString().trim()

                    if (opt1.isBlank() || opt2.isBlank()) {
                        Toast.makeText(this, "A poll requires at least 2 options!", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    pollOptions.add(opt1)
                    pollOptions.add(opt2)

                    extraOptionViews.forEach { view ->
                        val text = view.findViewById<EditText>(R.id.etExtraOption)?.text?.toString()?.trim() ?: ""
                        if (text.isNotBlank()) {
                            pollOptions.add(text)
                        }
                    }
                }
                "petition" -> {
                    petitionTitle = b.etPetitionTitle.text.toString().trim()
                    if (petitionTitle.isBlank()) {
                        Toast.makeText(this, "Please enter a petition title!", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    petitionAddressedTo = b.etPetitionAddressedTo.text.toString().trim()
                    val goalInput = b.etPetitionTarget.text.toString().trim().toIntOrNull()
                    petitionGoal = if (goalInput != null && goalInput > 0) goalInput else 100
                }
            }

            b.btnSubmitDrop.isEnabled = false
            b.ivSubmitIcon.isVisible = false
            b.pbSubmitLoading.isVisible = true

            lifecycleScope.launch {
                var imageUrl = ""
                if (selectedImageUri != null) {
                    try {
                        imageUrl = uploadImage(selectedImageUri!!)
                    } catch (e: Exception) {
                        Toast.makeText(this@CampusFeedActivity, "Image upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                val post = CampusFeedPost(
                    authorId = currentUserId,
                    authorName = currentUserName,
                    authorHandle = currentUserHandle,
                    authorAvatar = currentUserAvatar,
                    authorRole = currentUserRole,
                    authorDepartment = currentUserDepartment,
                    authorBatch = currentUserBatch,
                    content = content,
                    imageUrls = if (imageUrl.isNotBlank()) listOf(imageUrl) else emptyList(),
                    hashtags = hashtags,
                    dropType = selectedDropType,
                    pollQuestion = if (selectedDropType == "poll") content else "",
                    pollOptions = pollOptions,
                    petitionTitle = petitionTitle,
                    petitionAddressedTo = petitionAddressedTo,
                    petitionTargetGoal = petitionGoal
                )

                repo.createPost(post)
                    .addOnCompleteListener {
                        Toast.makeText(this@CampusFeedActivity, "Post published to CampusFeed!", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        // Immediate reload of the feed to reveal the new post
                        val updated = repo.getLocalPosts(selectedTab, currentUserDepartment, currentUserBatch, currentUserId)
                        adapter.submitList(updated) {
                            binding.rvCampusFeed.scrollToPosition(0)
                        }
                        binding.layoutEmptyFeed.isVisible = updated.isEmpty()
                    }
            }
        }

        dialog.show()
    }

    private suspend fun uploadImage(uri: Uri): String = kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
        CloudinaryUploader.uploadImage(
            context = this,
            fileUri = uri,
            folder = "campus_feed",
            onSuccess = { url, _ ->
                if (continuation.isActive) continuation.resumeWith(Result.success(url))
            },
            onFailure = { error ->
                if (continuation.isActive) continuation.resumeWith(Result.failure(Exception(error)))
            }
        )
    }

    private fun sharePost(post: CampusFeedPost) {
        repo.recordShare(post.id)
        val uniqueLink = "https://gstu-go.web.app/feed?id=${post.id}"
        val shareText = "🗣️ GSTU CampusFeed — ${post.authorName} (${post.authorRole.uppercase()}):\n" +
                "\"${post.content}\"\n\n" +
                "🔗 Open on GSTU Go:\n$uniqueLink"

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        startActivity(Intent.createChooser(sendIntent, "Share Campus Post"))
    }

    private fun showReportDialog(post: CampusFeedPost) {
        AlertDialog.Builder(this)
            .setTitle("Report Post")
            .setMessage("Would you like to report this post for violating GSTU community guidelines?")
            .setPositiveButton("Report") { _, _ ->
                repo.reportPost(post.id, currentUserId, "Community Report")
                    .addOnSuccessListener {
                        Toast.makeText(this, "Post reported for moderation.", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCommentsBottomSheet(post: CampusFeedPost) {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_notice_comments, null)
        dialog.setContentView(view)

        val tvTitle = view.findViewById<TextView>(R.id.commentsHeader)
        val btnClose = view.findViewById<View>(R.id.btnClose)
        val rvComments = view.findViewById<RecyclerView>(R.id.rvComments)
        val tvEmptyComments = view.findViewById<TextView>(R.id.tvEmptyComments)
        val tvComposerAvatar = view.findViewById<TextView>(R.id.tvComposerAvatar)
        val etComment = view.findViewById<EditText>(R.id.etComment)
        val btnSend = view.findViewById<View>(R.id.btnSendComment)

        tvTitle?.text = "Discussion (${post.commentCount})"
        btnClose?.setOnClickListener { dialog.dismiss() }
        tvComposerAvatar?.text = currentUserName.firstOrNull()?.uppercaseChar()?.toString() ?: "U"

        val commentAdapter = CampusFeedCommentAdapter()
        rvComments?.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        rvComments?.adapter = commentAdapter

        // Load cached comments first
        val cached = repo.getLocalComments(post.id).toMutableList()
        commentAdapter.submitList(cached)
        tvEmptyComments?.isVisible = cached.isEmpty()

        // Realtime Firestore listener for all comments on this post
        val registration = repo.getCommentsQuery(post.id)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val firestoreComments = snapshot.toObjects(CampusFeedComment::class.java)
                val allComments = (firestoreComments + cached)
                    .distinctBy { it.id.ifBlank { "${it.authorId}_${it.content}" } }
                    .sortedBy { it.timestamp?.toDate()?.time ?: 0L }

                commentAdapter.submitList(allComments) {
                    if (allComments.isNotEmpty()) {
                        rvComments?.scrollToPosition(allComments.size - 1)
                    }
                }
                tvEmptyComments?.isVisible = allComments.isEmpty()
                val totalCount = maxOf(allComments.size, post.commentCount)
                tvTitle?.text = "Discussion ($totalCount)"
            }

        dialog.setOnDismissListener {
            registration.remove()
        }

        btnSend?.setOnClickListener {
            val text = etComment?.text?.toString()?.trim() ?: ""
            if (text.isBlank()) return@setOnClickListener

            val comment = CampusFeedComment(
                authorId = currentUserId,
                authorName = currentUserName,
                authorHandle = currentUserHandle,
                authorAvatar = currentUserAvatar,
                authorRole = currentUserRole,
                content = text
            )

            etComment?.setText("")

            repo.addComment(post.id, comment)
                .addOnSuccessListener {
                    Toast.makeText(this, "Reply posted!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Reply posted!", Toast.LENGTH_SHORT).show()
                }
        }

        dialog.show()
    }

    private fun showFeedInfoDialog() {
        AlertDialog.Builder(this)
            .setTitle("About CampusFeed")
            .setMessage(
                "🔥 For You: Algorithmic ranking prioritizing active debates, high-engagement discussions, polls, and urgent petitions.\n\n" +
                "⏱ Latest: Pure real-time stream ordered chronologically.\n\n" +
                "All campus drops (thoughts, student polls, petitions, and interest clubs) appear seamlessly across both streams."
            )
            .setPositiveButton("View Features Roadmap") { _, _ ->
                startActivity(Intent(this, CampusFeedFeaturesActivity::class.java))
            }
            .setNegativeButton("Got it", null)
            .show()
    }
}
