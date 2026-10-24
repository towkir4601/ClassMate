package com.shuaib.classmate.adapters

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.shuaib.classmate.R
import com.shuaib.classmate.databinding.ItemAppNotificationBinding
import com.shuaib.classmate.models.AppNotification
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationsAdapter(
    private var notifications: List<AppNotification>,
    private var currentUserId: String = "",
    private val onItemClick: (AppNotification) -> Unit = {}
) : RecyclerView.Adapter<NotificationsAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemAppNotificationBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAppNotificationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val n = notifications[position]
        val b = holder.binding

        b.tvNotificationTitle.text = n.title
        b.tvNotificationMessage.text = n.message
        b.tvNotificationTime.text = formatTimeAgo(n.timestamp?.seconds)

        val isUnread = !n.isRead && !n.readBy.contains(currentUserId)
        b.dotUnread.visibility = if (isUnread) View.VISIBLE else View.GONE

        // Color coding and icons based on notification type
        when (n.type) {
            "report" -> {
                b.flIconContainer.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#3B1D24"))
                b.ivNotificationIcon.setImageResource(R.drawable.ic_notice)
                b.ivNotificationIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#EF4444"))
            }
            "blood_donated" -> {
                b.flIconContainer.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#34141A"))
                b.ivNotificationIcon.setImageResource(R.drawable.ic_blood_drop)
                b.ivNotificationIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#F87171"))
            }
            "item_given" -> {
                b.flIconContainer.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#142E22"))
                b.ivNotificationIcon.setImageResource(R.drawable.ic_location_pin)
                b.ivNotificationIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#34D399"))
            }
            "item_found" -> {
                b.flIconContainer.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#172554"))
                b.ivNotificationIcon.setImageResource(R.drawable.ic_location_pin)
                b.ivNotificationIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#60A5FA"))
            }
            "react" -> {
                b.flIconContainer.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#3B1D28"))
                b.ivNotificationIcon.setImageResource(R.drawable.ic_heart_filled)
                b.ivNotificationIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#F43F5E"))
            }
            "share" -> {
                b.flIconContainer.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#1E1B4B"))
                b.ivNotificationIcon.setImageResource(R.drawable.ic_share_outline)
                b.ivNotificationIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#818CF8"))
            }
            else -> {
                b.flIconContainer.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#1E293B"))
                b.ivNotificationIcon.setImageResource(R.drawable.ic_bell_outline)
                b.ivNotificationIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#94A3B8"))
            }
        }

        b.layoutNotificationRoot.setOnClickListener {
            onItemClick(n)
        }
    }

    override fun getItemCount(): Int = notifications.size

    fun updateList(newList: List<AppNotification>) {
        notifications = newList
        notifyDataSetChanged()
    }

    private fun formatTimeAgo(seconds: Long?): String {
        if (seconds == null || seconds <= 0) return "Just now"
        val nowSec = System.currentTimeMillis() / 1000
        val diffSec = Math.max(0, nowSec - seconds)
        return when {
            diffSec < 60 -> "Just now"
            diffSec < 3600 -> "${diffSec / 60}m ago"
            diffSec < 86400 -> "${diffSec / 3600}h ago"
            diffSec < 172800 -> "Yesterday"
            else -> "${diffSec / 86400}d ago"
        }
    }
}
