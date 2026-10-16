package com.shuaib.classmate.adapters

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.shuaib.classmate.R
import com.shuaib.classmate.databinding.ItemCampusClubBinding
import com.shuaib.classmate.models.CampusClub

class CampusClubAdapter(
    private var items: List<CampusClub>,
    private var currentUserId: String = "",
    private var isSuperAdmin: Boolean = false,
    private val onClubClick: (CampusClub) -> Unit,
    private val onDeleteClick: (CampusClub) -> Unit = {}
) : RecyclerView.Adapter<CampusClubAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemCampusClubBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCampusClubBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val club = items[position]
        val b = holder.binding

        b.tvClubName.text = club.name
        b.tvClubSubtitle.text = club.subtitle
        // Real member count: never fake numbers!
        b.tvClubMembers.text = club.getRealMemberCountText()

        // User's role in this club
        val userRole = club.getUserRole(currentUserId, isSuperAdmin)
        if (userRole != "Visitor") {
            b.tvClubRoleBadge.visibility = View.VISIBLE
            b.tvClubRoleBadge.text = userRole
            when (userRole) {
                "Superadmin" -> {
                    b.tvClubRoleBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#4C1D95"))
                    b.tvClubRoleBadge.setTextColor(Color.parseColor("#E9D5FF"))
                }
                "President" -> {
                    b.tvClubRoleBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#78350F"))
                    b.tvClubRoleBadge.setTextColor(Color.parseColor("#FDE68A"))
                }
                "General Secretary (GS)" -> {
                    b.tvClubRoleBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#14532D"))
                    b.tvClubRoleBadge.setTextColor(Color.parseColor("#BBF7D0"))
                }
                "Assistant General Secretary (AGS)" -> {
                    b.tvClubRoleBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#0C4A6E"))
                    b.tvClubRoleBadge.setTextColor(Color.parseColor("#BAE6FD"))
                }
                else -> {
                    b.tvClubRoleBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#312E81"))
                    b.tvClubRoleBadge.setTextColor(Color.parseColor("#C7D2FE"))
                }
            }
        } else {
            b.tvClubRoleBadge.visibility = View.GONE
        }

        // Delete Club Button: Only Superadmin or President of the club
        val canDelete = isSuperAdmin || (club.presidentId.isNotBlank() && club.presidentId == currentUserId)
        if (canDelete) {
            b.btnDeleteClub.visibility = View.VISIBLE
            b.btnDeleteClub.setOnClickListener {
                onDeleteClick(club)
            }
        } else {
            b.btnDeleteClub.visibility = View.GONE
        }

        // Set Icon based on club category or predefined icons
        val iconRes = when {
            club.name.contains("Computer", ignoreCase = true) || club.category.contains("Software", ignoreCase = true) -> R.drawable.ic_code_terminal
            club.name.contains("Robotics", ignoreCase = true) || club.category.contains("Hardware", ignoreCase = true) -> R.drawable.ic_robot_arm
            club.name.contains("Debat", ignoreCase = true) || club.category.contains("Speaking", ignoreCase = true) -> R.drawable.ic_debate_voice
            club.name.contains("Cultural", ignoreCase = true) || club.category.contains("Music", ignoreCase = true) -> R.drawable.ic_music_note
            else -> R.drawable.ic_users
        }
        b.ivClubIcon.setImageResource(iconRes)

        b.cardClubRoot.setOnClickListener {
            onClubClick(club)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateList(
        newItems: List<CampusClub>,
        userId: String = currentUserId,
        superAdmin: Boolean = isSuperAdmin
    ) {
        items = newItems
        currentUserId = userId
        isSuperAdmin = superAdmin
        notifyDataSetChanged()
    }
}
