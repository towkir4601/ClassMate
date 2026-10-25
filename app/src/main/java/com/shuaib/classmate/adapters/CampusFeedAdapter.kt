package com.shuaib.classmate.adapters

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.Timestamp
import com.shuaib.classmate.R
import com.shuaib.classmate.databinding.ItemCampusFeedPollOptionBinding
import com.shuaib.classmate.databinding.ItemCampusFeedPostBinding
import com.shuaib.classmate.models.CampusFeedPost
import com.shuaib.classmate.repository.CampusFeedBookmarkManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CampusFeedAdapter(
    private val currentUserId: String,
    private val currentUserName: String,
    private val onLoveClick: (CampusFeedPost, Boolean) -> Unit,
    private val onPollVote: (CampusFeedPost, String) -> Unit,
    private val onPetitionSign: (CampusFeedPost) -> Unit,
    private val onCommentClick: (CampusFeedPost) -> Unit,
    private val onShareClick: (CampusFeedPost) -> Unit,
    private val onReportClick: (CampusFeedPost) -> Unit,
    private val onBookmarkClick: ((CampusFeedPost, Boolean) -> Unit)? = null
) : ListAdapter<CampusFeedPost, CampusFeedAdapter.PostViewHolder>(PostDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemCampusFeedPostBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PostViewHolder(val b: ItemCampusFeedPostBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun bind(post: CampusFeedPost) {
            val ctx = itemView.context

            // 1. Author Info
            b.tvAuthorName.text = post.authorName.ifBlank { "GSTUian" }

            val handle = post.authorHandle.ifBlank {
                val clean = post.authorName.lowercase().replace(" ", "_").filter { it.isLetterOrDigit() || it == '_' }
                if (clean.isNotBlank()) "@$clean" else "@student"
            }
            b.tvAuthorHandle.text = handle
            b.tvTimestamp.text = formatRelativeTime(post.timestamp)

            // Author Role Badge
            if (post.authorRole.isNotBlank() && !post.authorRole.equals("student", ignoreCase = true)) {
                b.tvAuthorRoleBadge.isVisible = true
                b.tvAuthorRoleBadge.text = post.authorRole.uppercase()
            } else if (post.authorBatch.isNotBlank()) {
                b.tvAuthorRoleBadge.isVisible = true
                b.tvAuthorRoleBadge.text = post.authorBatch.uppercase()
            } else {
                b.tvAuthorRoleBadge.isVisible = false
            }

            // Author Avatar
            if (post.authorAvatar.isNotBlank()) {
                b.ivAuthorAvatar.isVisible = true
                b.tvAvatarFallback.isVisible = false
                Glide.with(ctx)
                    .load(post.authorAvatar)
                    .circleCrop()
                    .placeholder(R.drawable.ic_person)
                    .into(b.ivAuthorAvatar)
            } else {
                b.ivAuthorAvatar.isVisible = false
                b.tvAvatarFallback.isVisible = true
                b.tvAvatarFallback.text = post.authorName.firstOrNull()?.uppercaseChar()?.toString() ?: "G"
                b.tvAvatarFallback.background = ctx.getDrawable(R.drawable.bg_circle_avatar)
            }

            // 2. Post Content
            b.tvPostContent.text = post.content

            // 3. Optional Image Attachment
            if (post.imageUrls.isNotEmpty() && post.imageUrls[0].isNotBlank()) {
                b.cardPostImage.isVisible = true
                Glide.with(ctx)
                    .load(post.imageUrls[0])
                    .centerCrop()
                    .into(b.ivPostImage)
            } else {
                b.cardPostImage.isVisible = false
            }

            // 4. DROPS
            // --- Poll Drop ---
            if (post.isPoll && post.pollOptions.isNotEmpty()) {
                b.layoutPollDrop.isVisible = true
                renderPoll(post, ctx)
            } else {
                b.layoutPollDrop.isVisible = false
            }

            // --- Petition Drop ---
            if (post.isPetition) {
                b.layoutPetitionDrop.isVisible = true
                renderPetition(post)
            } else {
                b.layoutPetitionDrop.isVisible = false
            }

            // 5. Engagement Action Bar
            // Comments (uncolored gray when empty, sky blue when reacted/commented)
            if (post.commentCount > 0) {
                b.ivCommentIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#1D9BF0"))
                b.tvCommentCount.setTextColor(Color.parseColor("#1D9BF0"))
                b.tvCommentCount.text = "${post.commentCount}"
            } else {
                b.ivCommentIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#64748B"))
                b.tvCommentCount.setTextColor(Color.parseColor("#64748B"))
                b.tvCommentCount.text = ""
            }
            b.btnActionComment.setOnClickListener {
                onCommentClick(post)
            }

            // Shares
            b.tvShareCount.text = if (post.shareCount > 0) "${post.shareCount}" else ""
            b.btnActionShare.setOnClickListener {
                onShareClick(post)
            }

            // Love / Like
            val hasLoved = post.hasLoved(currentUserId)
            renderLoveState(hasLoved, post.loveCount)

            b.btnActionLove.setOnClickListener {
                val willLove = !post.hasLoved(currentUserId)
                // Animate pop
                val animX = ObjectAnimator.ofFloat(b.ivLoveIcon, "scaleX", 0.7f, 1.25f, 1.0f)
                val animY = ObjectAnimator.ofFloat(b.ivLoveIcon, "scaleY", 0.7f, 1.25f, 1.0f)
                animX.duration = 300
                animY.duration = 300
                animX.interpolator = OvershootInterpolator(2.0f)
                animX.start()
                animY.start()

                val newCount = if (willLove) post.loveCount + 1 else maxOf(0, post.loveCount - 1)
                renderLoveState(willLove, newCount)
                onLoveClick(post, willLove)
            }

            // Views (always colored sky blue)
            b.tvViewCount.text = formatCount(post.viewCount)

            // Three-dot Options (Save to Bookmark with colored icon, Report)
            b.btnPostOptions.setOnClickListener { v ->
                val popup = PopupMenu(ctx, v)
                val isBookmarked = CampusFeedBookmarkManager.isBookmarked(currentUserId, post.id, ctx)

                // 1. Save to Bookmark (colored icon)
                val bookmarkTitle = if (isBookmarked) "Remove from Bookmarks" else "Save to Bookmark"
                val bookmarkItem = popup.menu.add(0, 3, 0, bookmarkTitle)
                val bookmarkIcon = ContextCompat.getDrawable(ctx, R.drawable.ic_bookmark_colored)
                bookmarkItem.icon = bookmarkIcon

                // 2. Report
                val reportItem = popup.menu.add(0, 2, 1, "Report Post")
                val reportIcon = ContextCompat.getDrawable(ctx, R.drawable.ic_report_flag)
                reportItem.icon = reportIcon

                // Force show colored icons in PopupMenu across all Android versions
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    popup.setForceShowIcon(true)
                } else {
                    try {
                        val field = PopupMenu::class.java.getDeclaredField("mPopup")
                        field.isAccessible = true
                        val helper = field.get(popup)
                        helper.javaClass.getDeclaredMethod("setForceShowIcon", Boolean::class.java).invoke(helper, true)
                    } catch (_: Exception) {}
                }

                popup.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        3 -> {
                            val newlyBookmarked = CampusFeedBookmarkManager.toggleBookmark(currentUserId, post.id, ctx)
                            val toastMsg = if (newlyBookmarked) "Saved to Bookmarks 🔖" else "Removed from Bookmarks"
                            Toast.makeText(ctx, toastMsg, Toast.LENGTH_SHORT).show()
                            onBookmarkClick?.invoke(post, newlyBookmarked)
                            true
                        }
                        2 -> {
                            onReportClick(post)
                            true
                        }
                        else -> false
                    }
                }
                popup.show()
            }
        }

        private fun renderLoveState(hasLoved: Boolean, count: Int) {
            if (hasLoved) {
                b.ivLoveIcon.setImageResource(R.drawable.ic_heart_filled)
                b.ivLoveIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#F91880"))
                b.tvLoveCount.setTextColor(Color.parseColor("#F91880"))
            } else {
                b.ivLoveIcon.setImageResource(R.drawable.ic_heart_outline)
                b.ivLoveIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#64748B"))
                b.tvLoveCount.setTextColor(Color.parseColor("#64748B"))
            }
            b.tvLoveCount.text = if (count > 0) formatCount(count) else ""
        }

        private fun renderPoll(post: CampusFeedPost, ctx: Context) {
            b.containerPollOptions.removeAllViews()
            val hasVoted = post.hasVoted(currentUserId)
            val isExpired = post.isPollExpired
            val userVote = post.userSelectedOption(currentUserId)

            b.tvPollMeta.text = buildString {
                append("${post.totalPollVotes} ${if (post.totalPollVotes == 1) "vote" else "votes"}")
                append(" · ")
                if (isExpired) {
                    append("Final results")
                } else {
                    append("Open poll")
                }
            }

            post.pollOptions.forEach { option ->
                val optBinding = ItemCampusFeedPollOptionBinding.inflate(
                    LayoutInflater.from(ctx), b.containerPollOptions, false
                )

                optBinding.tvOptionText.text = option
                val isMyPick = (option == userVote)

                if (hasVoted || isExpired) {
                    // Show percentage and fill bar
                    val percent = post.pollOptionPercentage(option)
                    optBinding.tvOptionPercent.isVisible = true
                    optBinding.tvOptionPercent.text = "$percent%"
                    optBinding.viewPollFill.isVisible = true

                    optBinding.rootPollOption.post {
                        val totalW = optBinding.rootPollOption.width
                        val fillW = (totalW * (percent / 100f)).toInt()
                        val lp = optBinding.viewPollFill.layoutParams
                        lp.width = fillW
                        optBinding.viewPollFill.layoutParams = lp
                    }

                    if (isMyPick) {
                        optBinding.rootPollOption.setBackgroundResource(R.drawable.bg_poll_option_selected)
                        optBinding.tvOptionText.text = "✓ $option"
                        optBinding.tvOptionText.setTextColor(Color.parseColor("#2563EB"))
                    } else {
                        optBinding.rootPollOption.setBackgroundResource(R.drawable.bg_poll_option)
                    }
                } else {
                    // Unvoted state
                    optBinding.tvOptionPercent.isVisible = false
                    optBinding.viewPollFill.isVisible = false
                    optBinding.rootPollOption.setBackgroundResource(R.drawable.bg_poll_option)

                    optBinding.rootPollOption.setOnClickListener {
                        onPollVote(post, option)
                    }
                }

                b.containerPollOptions.addView(optBinding.root)
            }
        }

        private fun renderPetition(post: CampusFeedPost) {
            b.tvPetitionTitle.text = post.petitionTitle.ifBlank { post.content }

            if (post.petitionAddressedTo.isNotBlank()) {
                b.tvPetitionAddressedTo.isVisible = true
                b.tvPetitionAddressedTo.text = "Target Authority: ${post.petitionAddressedTo}"
            } else {
                b.tvPetitionAddressedTo.isVisible = false
            }

            val count = post.petitionSignaturesCount
            val goal = post.petitionTargetGoal
            val percent = post.petitionProgressPercentage

            b.tvPetitionSignaturesCount.text = "$count of $goal ($percent%)"
            b.pbPetitionGoal.progress = percent

            val hasSigned = post.hasSigned(currentUserId)
            if (hasSigned) {
                b.btnSignPetition.text = "✅ Signed"
                b.btnSignPetition.isEnabled = false
                b.btnSignPetition.alpha = 0.85f
            } else {
                b.btnSignPetition.text = "✍️ Sign Petition"
                b.btnSignPetition.isEnabled = true
                b.btnSignPetition.alpha = 1.0f
                b.btnSignPetition.setOnClickListener {
                    onPetitionSign(post)
                }
            }
        }
    }

    class PostDiffCallback : DiffUtil.ItemCallback<CampusFeedPost>() {
        override fun areItemsTheSame(oldItem: CampusFeedPost, newItem: CampusFeedPost): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: CampusFeedPost, newItem: CampusFeedPost): Boolean =
            oldItem == newItem
    }

    companion object {
        fun formatRelativeTime(timestamp: Timestamp?): String {
            val time = timestamp?.toDate()?.time ?: return "just now"
            val diff = maxOf(0L, System.currentTimeMillis() - time)
            val seconds = diff / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            val days = hours / 24
            return when {
                seconds < 45 -> "just now"
                minutes < 60 -> "${minutes}m"
                hours < 24 -> "${hours}h"
                days < 7 -> "${days}d"
                else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(time))
            }
        }

        fun formatCount(count: Int): String {
            return when {
                count < 1000 -> "$count"
                count < 1_000_000 -> String.format(Locale.US, "%.1fk", count / 1000.0)
                else -> String.format(Locale.US, "%.1fM", count / 1000000.0)
            }
        }
    }
}
