package com.shuaib.classmate.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.shuaib.classmate.R
import com.shuaib.classmate.adapters.CampusFeedAdapter
import com.shuaib.classmate.databinding.ActivityViewProfileBinding
import com.shuaib.classmate.models.CampusFeedPost
import com.shuaib.classmate.models.User
import com.shuaib.classmate.repository.CampusFeedBookmarkManager
import com.shuaib.classmate.repository.CampusFeedRepository

/**
 * Modern, rich Profile & Activity Viewer for GSTU Go.
 * Displays academic information, contact details, social links, and real-time CampusFeed drops/posts.
 */
class ViewProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityViewProfileBinding
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    private var targetUserId: String = ""
    private var isSelfProfile: Boolean = false
    private var loadedUser: User? = null

    private lateinit var feedAdapter: CampusFeedAdapter
    private val feedRepository by lazy { CampusFeedRepository() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val currentUid = auth.currentUser?.uid.orEmpty()
        targetUserId = intent.getStringExtra(EXTRA_USER_ID)?.ifBlank { currentUid } ?: currentUid
        isSelfProfile = (targetUserId == currentUid)

        setupToolbar()
        setupTabs()
        setupFeedAdapter()
        loadUserProfile()
        loadUserPosts()

        if (intent.getStringExtra(EXTRA_INITIAL_TAB) == "info") {
            binding.btnTabInfo.performClick()
        }
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnShareProfile.setOnClickListener { shareProfileCard() }
    }

    private fun setupTabs() {
        binding.btnTabInfo.setOnClickListener {
            binding.btnTabInfo.setBackgroundResource(R.drawable.bg_dept_acronym_pill)
            binding.btnTabInfo.setTextColor(getColor(R.color.cm_primary))
            binding.btnTabPosts.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            binding.btnTabPosts.setTextColor(getColor(R.color.cm_text_muted))

            binding.layoutTabInfo.visibility = View.VISIBLE
            binding.layoutTabPosts.visibility = View.GONE
        }

        binding.btnTabPosts.setOnClickListener {
            binding.btnTabPosts.setBackgroundResource(R.drawable.bg_dept_acronym_pill)
            binding.btnTabPosts.setTextColor(getColor(R.color.cm_primary))
            binding.btnTabInfo.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            binding.btnTabInfo.setTextColor(getColor(R.color.cm_text_muted))

            binding.layoutTabInfo.visibility = View.GONE
            binding.layoutTabPosts.visibility = View.VISIBLE
        }

        // Open Campus Feed button
        binding.btnOpenCampusFeed.setOnClickListener {
            startActivity(Intent(this, CampusFeedActivity::class.java))
        }
    }

    private fun setupFeedAdapter() {
        val currentUid = auth.currentUser?.uid.orEmpty()
        val currentUserName = auth.currentUser?.displayName.orEmpty().ifBlank { "User" }

        feedAdapter = CampusFeedAdapter(
            currentUserId = currentUid,
            currentUserName = currentUserName,
            onLoveClick = { post, isLoved ->
                feedRepository.toggleLove(post.id, currentUid, isLoved)
            },
            onPollVote = { post, option ->
                feedRepository.votePoll(post.id, currentUid, option)
            },
            onPetitionSign = { post ->
                feedRepository.signPetition(post.id, currentUid)
            },
            onCommentClick = { post ->
                val intent = Intent(this, CampusFeedActivity::class.java).apply {
                    putExtra("target_post_id", post.id)
                }
                startActivity(intent)
            },
            onShareClick = { post ->
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, "GSTU CampusFeed: \"${post.content}\" - by ${post.authorName}")
                    type = "text/plain"
                }
                startActivity(Intent.createChooser(sendIntent, "Share post via"))
            },
            onReportClick = { post ->
                feedRepository.reportPost(post.id, currentUid, "Reported from profile")
                Toast.makeText(this, "Post reported for review", Toast.LENGTH_SHORT).show()
            },
            onBookmarkClick = { post, _ ->
                CampusFeedBookmarkManager.toggleBookmark(currentUid, post.id, this)
            }
        )

        binding.rvUserPosts.layoutManager = LinearLayoutManager(this)
        binding.rvUserPosts.adapter = feedAdapter
    }

    private fun loadUserProfile() {
        if (targetUserId.isBlank()) return

        firestore.collection("users").document(targetUserId).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    Toast.makeText(this, "Profile not found", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val user = User(
                    uid = doc.id,
                    name = doc.getString("name").orEmpty().ifBlank { doc.getString("fullName").orEmpty() },
                    fullName = doc.getString("fullName").orEmpty(),
                    studentId = doc.getString("studentId").orEmpty(),
                    batch = doc.getString("batch").orEmpty(),
                    semester = doc.getString("semester").orEmpty(),
                    teacherCode = doc.getString("teacherCode").orEmpty(),
                    department = doc.getString("department").orEmpty(),
                    email = doc.getString("email").orEmpty(),
                    phone = doc.getString("phone").orEmpty(),
                    whatsappNumber = doc.getString("whatsappNumber").orEmpty(),
                    bloodGroup = doc.getString("bloodGroup").orEmpty(),
                    homeDistrict = doc.getString("homeDistrict").orEmpty(),
                    presentAddress = doc.getString("presentAddress").orEmpty(),
                    permanentAddress = doc.getString("permanentAddress").orEmpty(),
                    role = doc.getString("role") ?: "student",
                    photoUrl = doc.getString("photoUrl").orEmpty(),
                    isVerified = doc.getBoolean("isVerified") ?: false,
                    authProvider = doc.getString("authProvider").orEmpty(),
                    createdAt = doc.getTimestamp("createdAt")
                )
                loadedUser = user
                populateUI(user)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun populateUI(user: User) {
        val isTeacher = user.role.equals("teacher", ignoreCase = true)

        val resolvedName = user.name.ifBlank { user.fullName.ifBlank { user.email.substringBefore("@") } }
        binding.tvFullName.text = resolvedName.ifBlank { "GSTU Member" }
        binding.ivVerifiedBadge.isVisible = user.isVerified || user.role.equals("superadmin", true) || isTeacher

        // Role Badge
        binding.tvRoleBadge.text = user.role.uppercase()
        when (user.role.lowercase()) {
            "superadmin" -> binding.tvRoleBadge.backgroundTintList = getColorStateList(R.color.cm_cancel_red)
            "admin", "university_admin" -> binding.tvRoleBadge.backgroundTintList = getColorStateList(R.color.cm_accent)
            "teacher" -> binding.tvRoleBadge.backgroundTintList = getColorStateList(R.color.cm_success)
            "cr" -> binding.tvRoleBadge.backgroundTintList = getColorStateList(R.color.cm_warning)
            else -> binding.tvRoleBadge.backgroundTintList = getColorStateList(R.color.cm_primary)
        }

        // Subtitle Department + Batch
        val dept = user.department.ifBlank { "GSTU Campus" }
        val batchText = if (isTeacher) "Faculty Member" else user.batch.takeIf { it.isNotBlank() }?.let { "Batch $it" } ?: ""
        binding.tvDepartmentBatch.text = if (batchText.isNotBlank()) "$dept • $batchText" else dept

        // ID / Teacher Code
        val idLabel = if (isTeacher) {
            user.teacherCode.takeIf { it.isNotBlank() }?.let { "Code: $it" } ?: "Teacher"
        } else {
            user.studentId.takeIf { it.isNotBlank() }?.let { "ID: $it" } ?: "Student"
        }
        binding.tvStudentId.text = idLabel

        // Avatar
        if (user.photoUrl.isNotBlank()) {
            binding.ivAvatar.visibility = View.VISIBLE
            binding.tvAvatarFallback.visibility = View.GONE
            Glide.with(this)
                .load(user.photoUrl)
                .placeholder(R.drawable.ic_default_avatar)
                .into(binding.ivAvatar)
        } else {
            binding.ivAvatar.visibility = View.GONE
            binding.tvAvatarFallback.visibility = View.VISIBLE
            binding.tvAvatarFallback.text = user.name.firstOrNull()?.toString()?.uppercase() ?: "U"
        }

        // Stats Bar
        val deptAcronym = when {
            user.department.contains("Computer", true) -> "CSE"
            user.department.contains("Electrical", true) -> "EEE"
            user.department.contains("Civil", true) -> "CE"
            user.department.contains("Pharmacy", true) -> "PHARM"
            user.department.contains("Math", true) -> "MATH"
            user.department.contains("Law", true) -> "LAW"
            user.department.isNotBlank() -> user.department.split(" ").mapNotNull { it.firstOrNull()?.toString() }.joinToString("").take(4).uppercase()
            else -> "GSTU"
        }
        binding.tvStatDeptCode.text = deptAcronym
        binding.tvStatBatch.text = if (isTeacher) "Faculty" else if (user.batch.isNotBlank()) "${user.batch}th" else "N/A"

        // Actions: Self vs Other
        if (isSelfProfile) {
            binding.layoutSelfActions.visibility = View.VISIBLE
            binding.layoutOtherActions.visibility = View.GONE

            binding.btnEditProfile.setOnClickListener {
                startActivity(Intent(this, EditProfileActivity::class.java))
            }
            binding.btnShareCard.setOnClickListener { shareProfileCard() }
        } else {
            binding.layoutSelfActions.visibility = View.GONE
            binding.layoutOtherActions.visibility = View.VISIBLE

            binding.btnChat.setOnClickListener {
                openChatWithUser(user)
            }
            binding.btnCall.setOnClickListener {
                if (user.phone.isNotBlank()) {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${user.phone}"))
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "Phone number not available", Toast.LENGTH_SHORT).show()
                }
            }
            binding.btnWhatsApp.setOnClickListener {
                val waNumber = user.whatsappNumber.ifBlank { user.phone }
                if (waNumber.isNotBlank()) {
                    val cleanPhone = waNumber.filter { it.isDigit() || it == '+' }
                    val url = "https://api.whatsapp.com/send?phone=$cleanPhone"
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    } catch (e: Exception) {
                        Toast.makeText(this, "WhatsApp is not installed", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "WhatsApp number not available", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 2. Info Tab Rows
        binding.rowUniversity.tvLabel.text = "University"
        binding.rowUniversity.tvValue.text = "Gopalganj Science and Technology University"

        binding.rowDepartment.tvLabel.text = "Department"
        binding.rowDepartment.tvValue.text = user.department.ifBlank { "Not specified" }

        binding.rowStudentId.tvLabel.text = if (isTeacher) "Teacher Code" else "Student ID"
        binding.rowStudentId.tvValue.text = if (isTeacher) user.teacherCode.ifBlank { "Not set" } else user.studentId.ifBlank { "Not set" }

        binding.rowBatch.tvLabel.text = if (isTeacher) "Designation" else "Batch"
        binding.rowBatch.tvValue.text = if (isTeacher) "Faculty Member" else user.batch.takeIf { it.isNotBlank() }?.let { "Batch $it" } ?: "Not set"

        binding.rowSemester.tvLabel.text = "Semester"
        binding.rowSemester.tvValue.text = user.semester.ifBlank { "Not set" }

        binding.rowEmail.tvLabel.text = "Email"
        binding.rowEmail.tvValue.text = user.email.ifBlank { "Not set" }
        binding.rowEmail.root.setOnClickListener {
            if (user.email.isNotBlank()) {
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Email", user.email))
                Toast.makeText(this, "Email copied to clipboard", Toast.LENGTH_SHORT).show()
            }
        }

        binding.rowPhone.tvLabel.text = "Phone"
        binding.rowPhone.tvValue.text = user.phone.ifBlank { "Not set" }

        binding.rowWhatsApp.tvLabel.text = "WhatsApp"
        binding.rowWhatsApp.tvValue.text = user.whatsappNumber.ifBlank { user.phone.ifBlank { "Not set" } }

        binding.rowBloodGroup.tvLabel.text = "Blood Group"
        binding.rowBloodGroup.tvValue.text = user.bloodGroup.ifBlank { "Not set" }

        binding.rowDistrict.tvLabel.text = "Home District"
        binding.rowDistrict.tvValue.text = user.homeDistrict.ifBlank { "Not set" }

        binding.rowPresentAddress.tvLabel.text = "Present Address"
        binding.rowPresentAddress.tvValue.text = user.presentAddress.ifBlank { "Not set" }

        binding.rowPermanentAddress.tvLabel.text = "Permanent Address"
        binding.rowPermanentAddress.tvValue.text = user.permanentAddress.ifBlank { "Not set" }
    }

    private fun loadUserPosts() {
        if (targetUserId.isBlank()) return
        binding.pbPostsLoading.visibility = View.VISIBLE

        firestore.collection("campus_feed")
            .whereEqualTo("authorId", targetUserId)
            .get()
            .addOnSuccessListener { snapshot ->
                binding.pbPostsLoading.visibility = View.GONE
                val posts = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(CampusFeedPost::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        null
                    }
                }.sortedByDescending { it.timestamp?.seconds ?: 0L }

                binding.tvStatPostsCount.text = posts.size.toString()

                if (posts.isEmpty()) {
                    binding.rvUserPosts.visibility = View.GONE
                    binding.layoutNoPosts.visibility = View.VISIBLE
                    if (isSelfProfile) {
                        binding.tvNoPostsSubtitle.text = "You haven't shared any drops on the Campus Feed yet."
                        binding.btnOpenCampusFeed.visibility = View.VISIBLE
                    } else {
                        binding.tvNoPostsSubtitle.text = "This user hasn't posted anything on the Campus Feed yet."
                        binding.btnOpenCampusFeed.visibility = View.GONE
                    }
                } else {
                    binding.rvUserPosts.visibility = View.VISIBLE
                    binding.layoutNoPosts.visibility = View.GONE
                    feedAdapter.submitList(posts)
                }
            }
            .addOnFailureListener {
                binding.pbPostsLoading.visibility = View.GONE
                binding.tvStatPostsCount.text = "0"
            }
    }

    private fun openChatWithUser(user: User) {
        val myUid = auth.currentUser?.uid.orEmpty()
        if (myUid.isBlank() || user.uid.isBlank()) return

        val sortedIds = listOf(myUid, user.uid).sorted()
        val roomId = "${sortedIds[0]}_${sortedIds[1]}"
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("gstugo://chat/dm/$roomId")).apply {
                putExtra("roomId", roomId)
                putExtra("otherUserName", user.name.ifBlank { "Chat" })
                putExtra("otherUserId", user.uid)
                setPackage(packageName)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open direct message", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareProfileCard() {
        val user = loadedUser ?: return
        com.shuaib.classmate.ui.GstuIdCardBottomSheet.show(supportFragmentManager, user)
    }

    override fun onResume() {
        super.onResume()
        if (isSelfProfile) {
            loadUserProfile()
            loadUserPosts()
        }
    }

    companion object {
        const val EXTRA_USER_ID = "extra_user_id"
        const val EXTRA_INITIAL_TAB = "extra_initial_tab"
    }
}
