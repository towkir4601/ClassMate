package com.shuaib.classmate.adapters

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.shuaib.classmate.databinding.ItemCampusEventBinding
import com.shuaib.classmate.models.CampusEvent

class CampusEventAdapter(
    private var items: List<CampusEvent>,
    private var currentUserId: String = "",
    private var isSuperAdmin: Boolean = false,
    private var executiveClubIds: List<String> = emptyList(),
    private val onActionClick: (CampusEvent) -> Unit,
    private val onDeleteClick: (CampusEvent) -> Unit = {}
) : RecyclerView.Adapter<CampusEventAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemCampusEventBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCampusEventBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val event = items[position]
        val b = holder.binding

        b.tvEventTitle.text = event.title
        b.tvEventOrganizer.text = event.organizer
        b.tvEventDateTime.text = if (event.dateTime.isNotBlank()) event.dateTime else "${event.date} • ${event.time}"
        b.tvEventLocation.text = if (event.location.isNotBlank()) event.location else "Campus Premises"
        b.tvEventCategory.text = if (event.category.isNotBlank()) event.category else "Campus"

        // Status badge
        if (event.isRegistrationOpen) {
            b.tvEventStatus.text = "REGISTRATION OPEN"
            b.tvEventStatus.setTextColor(Color.parseColor("#34D399"))
            b.tvEventStatus.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#132B20"))
        } else {
            b.tvEventStatus.text = "CLOSED"
            b.tvEventStatus.setTextColor(Color.parseColor("#F87171"))
            b.tvEventStatus.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#371818"))
        }

        // Delete Button: visible only to club executives (President, GS, AGS) or Superadmin
        val canDelete = isSuperAdmin ||
                executiveClubIds.contains(event.clubId) ||
                (event.creatorId.isNotBlank() && event.creatorId == currentUserId)

        if (canDelete) {
            b.btnDeleteEvent.visibility = View.VISIBLE
            b.btnDeleteEvent.setOnClickListener {
                onDeleteClick(event)
            }
        } else {
            b.btnDeleteEvent.visibility = View.GONE
        }

        // Action button states
        if (event.isRegistered) {
            if (event.registrationStatus.equals("pending", ignoreCase = true)) {
                b.btnEventAction.text = "⏳  Registration Pending"
                b.btnEventAction.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D97706"))
            } else {
                b.btnEventAction.text = "✔  Registered (Confirmed)"
                b.btnEventAction.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#10B981"))
            }
        } else if (event.isRegistrationOpen) {
            b.btnEventAction.text = "✔  Register for Event"
            b.btnEventAction.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#6366F1"))
        } else {
            b.btnEventAction.text = "ⓘ  View Details"
            b.btnEventAction.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#2E3642"))
        }

        b.btnEventAction.setOnClickListener {
            onActionClick(event)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateList(
        newItems: List<CampusEvent>,
        userId: String = currentUserId,
        superAdmin: Boolean = isSuperAdmin,
        execClubs: List<String> = executiveClubIds
    ) {
        items = newItems
        currentUserId = userId
        isSuperAdmin = superAdmin
        executiveClubIds = execClubs
        notifyDataSetChanged()
    }
}
