package com.shuaib.classmate.adapters

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.shuaib.classmate.R
import com.shuaib.classmate.activities.ImageViewerActivity
import com.shuaib.classmate.databinding.ItemLostFoundBinding
import com.shuaib.classmate.models.LostFoundItem
import com.shuaib.classmate.repositories.AppNotificationRepository
import com.shuaib.classmate.repositories.LostFoundRepository
import com.shuaib.classmate.utils.AppPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LostFoundAdapter(
    items: List<LostFoundItem>,
    private var currentUserId: String = "",
    private var currentUserName: String = "Student",
    private var isAdmin: Boolean = false,
    private val onDeleteClick: (LostFoundItem) -> Unit = {},
    private val onItemClick: (LostFoundItem) -> Unit = {},
    private val onStatusChangeClick: (LostFoundItem, String) -> Unit = { _, _ -> },
    private val onReportClick: (LostFoundItem) -> Unit = {}
) : RecyclerView.Adapter<LostFoundAdapter.ViewHolder>() {

    private var items: MutableList<LostFoundItem> = items.toMutableList()
    private val repo = LostFoundRepository()
    private val notificationRepo = AppNotificationRepository()

    class ViewHolder(val binding: ItemLostFoundBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLostFoundBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val b = holder.binding
        val context = holder.itemView.context

        b.tvItemTitle.text = item.title
        b.tvItemTitle.setTextColor(Color.parseColor("#0F172A"))

        b.tvItemLocation.text = item.location.ifBlank { "Campus" }
        b.tvItemLocation.setTextColor(Color.parseColor("#334155"))

        b.tvContactInfo.text = "Contact: ${item.contactPhone.ifBlank { item.reporterName.ifBlank { "Campus Desk" } }}"
        b.tvContactInfo.setTextColor(Color.parseColor("#334155"))

        b.tvItemTime.text = formatItemTime(item.timestamp?.seconds)
        b.tvItemTime.setTextColor(Color.parseColor("#64748B"))

        if (item.description.isNotBlank() && !item.description.equals(item.title, ignoreCase = true)) {
            b.tvItemDescription.visibility = View.VISIBLE
            b.tvItemDescription.text = item.description
            b.tvItemDescription.setTextColor(Color.parseColor("#475569"))
        } else {
            b.tvItemDescription.visibility = View.GONE
        }

        if (item.imageUrl.isNotBlank()) {
            b.cardItemImage.visibility = View.VISIBLE
            Glide.with(context)
                .load(item.imageUrl)
                .placeholder(R.drawable.bg_lost_found_input)
                .error(R.drawable.bg_lost_found_input)
                .centerCrop()
                .into(b.ivItemPhoto)

            b.cardItemImage.setOnClickListener {
                val intent = Intent(context, ImageViewerActivity::class.java).apply {
                    putExtra(ImageViewerActivity.EXTRA_IMAGE_URL, item.imageUrl)
                }
                context.startActivity(intent)
            }
        } else {
            b.cardItemImage.visibility = View.GONE
            b.cardItemImage.setOnClickListener(null)
        }

        // Status badge
        if (item.isFound) {
            b.tvTypeBadge.text = "✔ FOUND"
            b.tvTypeBadge.setTextColor(Color.parseColor("#15803D"))
            b.tvTypeBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#DCFCE7"))
        } else {
            b.tvTypeBadge.text = "🔍 LOST"
            b.tvTypeBadge.setTextColor(Color.parseColor("#B91C1C"))
            b.tvTypeBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FEE2E2"))
        }

        // Resolved badge (Day theme)
        if (item.isResolved || item.isItemGiven || item.isItemFound) {
            b.tvResolvedStatusBadge.visibility = View.VISIBLE
            b.tvResolvedStatusBadge.text = when {
                item.isItemGiven -> "ITEM GIVEN"
                item.isItemFound -> "ITEM FOUND"
                else -> "RESOLVED"
            }
            b.tvResolvedStatusBadge.setTextColor(Color.parseColor("#0369A1"))
            b.tvResolvedStatusBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#E0F2FE"))
        } else {
            b.tvResolvedStatusBadge.visibility = View.GONE
        }

        // Delete button visibility: strictly creator or admin
        val canDelete = (item.reporterId.isNotBlank() && item.reporterId == currentUserId) || isAdmin
        b.btnDeleteItem.visibility = if (canDelete) View.VISIBLE else View.GONE
        b.btnDeleteItem.setOnClickListener {
            onDeleteClick(item)
        }

        // View count tracking (including guests)
        val prefs = AppPreferences(context)
        if (!prefs.hasViewedPost(item.id)) {
            prefs.markPostViewed(item.id)
            repo.recordView(item.id, currentUserId.ifBlank { null })
        }
        val displayViews = Math.max(item.viewsCount, if (prefs.hasViewedPost(item.id)) 1 else 0)
        b.tvViewsCount.text = "$displayViews ${if (displayViews == 1) "view" else "views"}"
        b.tvViewsCount.setTextColor(Color.parseColor("#64748B"))

        // Love react setup (Day theme + toggleable)
        val isLovedByMe = currentUserId.isNotBlank() && item.loveReactors.contains(currentUserId)
        if (isLovedByMe) {
            b.ivLove.setImageResource(R.drawable.ic_heart_filled)
            b.ivLove.imageTintList = ColorStateList.valueOf(Color.parseColor("#F43F5E"))
            b.tvLoveCount.setTextColor(Color.parseColor("#E11D48"))
        } else {
            b.ivLove.setImageResource(R.drawable.ic_heart_outline)
            b.ivLove.imageTintList = ColorStateList.valueOf(Color.parseColor("#64748B"))
            b.tvLoveCount.setTextColor(Color.parseColor("#475569"))
        }
        b.tvLoveCount.text = "${item.loveCount}"

        b.btnLove.setOnClickListener {
            if (currentUserId.isBlank()) {
                Toast.makeText(context, "Please sign in to react to this post", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val currentPos = holder.bindingAdapterPosition
            if (currentPos == RecyclerView.NO_POSITION || currentPos >= items.size) return@setOnClickListener
            val currentItem = items[currentPos]
            val willLove = !currentItem.loveReactors.contains(currentUserId)
            val updatedReactors = currentItem.loveReactors.toMutableList()
            if (willLove) {
                if (!updatedReactors.contains(currentUserId)) updatedReactors.add(currentUserId)
                b.ivLove.setImageResource(R.drawable.ic_heart_filled)
                b.ivLove.imageTintList = ColorStateList.valueOf(Color.parseColor("#F43F5E"))
                b.tvLoveCount.setTextColor(Color.parseColor("#E11D48"))
                b.tvLoveCount.text = "${updatedReactors.size}"

                // Send notification to who posted
                notificationRepo.sendNotificationToAuthor(
                    authorId = currentItem.reporterId,
                    type = "react",
                    title = "❤️ New Reaction on Lost & Found",
                    message = "$currentUserName loved your post: \"${currentItem.title}\"",
                    postId = currentItem.id,
                    postType = "lost_found",
                    actorId = currentUserId,
                    actorName = currentUserName
                )
            } else {
                updatedReactors.remove(currentUserId)
                b.ivLove.setImageResource(R.drawable.ic_heart_outline)
                b.ivLove.imageTintList = ColorStateList.valueOf(Color.parseColor("#64748B"))
                b.tvLoveCount.setTextColor(Color.parseColor("#475569"))
                b.tvLoveCount.text = "${updatedReactors.size}"
            }
            // Update in-memory item so subsequent clicks can take out the reaction or react again smoothly
            items[currentPos] = currentItem.copy(
                loveReactors = updatedReactors,
                loveCount = updatedReactors.size
            )
            repo.toggleLove(currentItem.id, currentUserId, willLove)
        }

        // Share button setup (Instagram-style share with unique link)
        b.ivShare.setImageResource(R.drawable.ic_instagram_share)
        b.ivShare.imageTintList = ColorStateList.valueOf(Color.parseColor("#64748B"))
        b.tvShareText.setTextColor(Color.parseColor("#475569"))

        b.btnShare.setOnClickListener {
            repo.recordShare(item.id)
            val uniqueLink = "https://gstu-go.web.app/lostfound?id=${item.id}"
            val shareText = "🔍 GSTU Go — ${if (item.isFound) "Found Item Notice" else "Lost Item Alert"}:\n" +
                    "📦 ${item.title}\n" +
                    "📍 Location: ${item.location}\n" +
                    "${if (item.description.isNotBlank() && !item.description.equals(item.title, ignoreCase = true)) "📝 Details: ${item.description}\n" else ""}" +
                    "📞 Contact: ${item.contactPhone.ifBlank { "Campus Desk" }}\n\n" +
                    "🔗 View Post on GSTU Go:\n$uniqueLink"
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, shareText)
                type = "text/plain"
            }
            context.startActivity(Intent.createChooser(sendIntent, "Share Item"))

            // Send notification to author
            if (currentUserId.isNotBlank()) {
                notificationRepo.sendNotificationToAuthor(
                    authorId = item.reporterId,
                    type = "share",
                    title = "🔗 Your Post was Shared",
                    message = "$currentUserName shared your post: \"${item.title}\"",
                    postId = item.id,
                    postType = "lost_found",
                    actorId = currentUserId,
                    actorName = currentUserName
                )
            }
        }

        // Call option
        b.btnCallAction.setOnClickListener {
            handleContactAction(context, item)
        }
        b.layoutContact.setOnClickListener {
            handleContactAction(context, item)
        }

        // Three-Dot More Options Menu
        b.btnMoreOptions.setOnClickListener { v ->
            showThreeDotMenu(v, context, item)
        }

        b.cardItemRoot.setOnClickListener {
            onItemClick(item)
        }
    }

    private fun showThreeDotMenu(anchor: View, context: Context, item: LostFoundItem) {
        val popup = PopupMenu(context, anchor)
        // Item Given if found post, Item Found if lost post
        val statusActionTitle = if (item.isFound) "Item Given" else "Item Found"
        popup.menu.add(0, 1, 0, statusActionTitle)
        popup.menu.add(0, 2, 1, "Report Post")

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                1 -> {
                    val status = if (item.isFound) "item_given" else "item_found"
                    onStatusChangeClick(item, status)
                    true
                }
                2 -> {
                    onReportClick(item)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun handleContactAction(context: Context, item: LostFoundItem) {
        val phoneDigits = item.contactPhone.filter { it.isDigit() || it == '+' }
        if (phoneDigits.length >= 5) {
            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneDigits"))
            context.startActivity(dialIntent)
        } else {
            Toast.makeText(context, "Contact: ${item.contactPhone}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newItems: List<LostFoundItem>) {
        items = newItems.toMutableList()
        notifyDataSetChanged()
    }

    fun setUserAndRole(userId: String, userName: String, adminStatus: Boolean) {
        currentUserId = userId
        currentUserName = userName.ifBlank { "Student" }
        isAdmin = adminStatus
        notifyDataSetChanged()
    }

    private fun formatItemTime(seconds: Long?): String {
        if (seconds == null || seconds <= 0) return "Recently"
        val nowSec = System.currentTimeMillis() / 1000
        val diffSec = Math.max(0, nowSec - seconds)
        return when {
            diffSec < 86400 -> {
                val sdf = SimpleDateFormat("h:mm a", Locale.US)
                "Today, ${sdf.format(Date(seconds * 1000))}"
            }
            diffSec < 172800 -> "Yesterday"
            else -> "${diffSec / 86400} days ago"
        }
    }
}
