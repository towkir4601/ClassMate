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
import androidx.recyclerview.widget.RecyclerView
import com.shuaib.classmate.databinding.ItemLostFoundBinding
import com.shuaib.classmate.models.LostFoundItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LostFoundAdapter(
    private var items: List<LostFoundItem>,
    private var currentUserId: String = "",
    private var isAdmin: Boolean = false,
    private val onDeleteClick: (LostFoundItem) -> Unit = {},
    private val onItemClick: (LostFoundItem) -> Unit = {}
) : RecyclerView.Adapter<LostFoundAdapter.ViewHolder>() {

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
        b.tvItemTitle.setTextColor(Color.WHITE)

        b.tvItemLocation.text = item.location.ifBlank { "Campus" }
        b.tvItemLocation.setTextColor(Color.parseColor("#CBD5E1"))

        b.tvContactInfo.text = "Contact: ${item.contactPhone.ifBlank { item.reporterName.ifBlank { "Campus Desk" } }}"
        b.tvContactInfo.setTextColor(Color.parseColor("#CBD5E1"))

        b.tvItemTime.text = formatItemTime(item.timestamp?.seconds)
        b.tvItemTime.setTextColor(Color.parseColor("#94A3B8"))

        if (item.description.isNotBlank() && !item.description.equals(item.title, ignoreCase = true)) {
            b.tvItemDescription.visibility = View.VISIBLE
            b.tvItemDescription.text = item.description
            b.tvItemDescription.setTextColor(Color.parseColor("#94A3B8"))
        } else {
            b.tvItemDescription.visibility = View.GONE
        }

        if (item.isFound) {
            b.tvTypeBadge.text = "✔ FOUND"
            b.tvTypeBadge.setTextColor(Color.parseColor("#34D399"))
            b.tvTypeBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#243427"))
        } else {
            b.tvTypeBadge.text = "🔍 LOST"
            b.tvTypeBadge.setTextColor(Color.parseColor("#F87171"))
            b.tvTypeBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#3A2224"))
        }

        // Delete button visibility: strictly creator or admin
        val canDelete = (item.reporterId.isNotBlank() && item.reporterId == currentUserId) || isAdmin
        b.btnDeleteItem.visibility = if (canDelete) View.VISIBLE else View.GONE
        b.btnDeleteItem.setOnClickListener {
            onDeleteClick(item)
        }

        b.layoutContact.setOnClickListener {
            handleContactAction(context, item)
        }

        b.cardItemRoot.setOnClickListener {
            onItemClick(item)
        }
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
        items = newItems
        notifyDataSetChanged()
    }

    fun setUserAndRole(userId: String, adminStatus: Boolean) {
        currentUserId = userId
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
