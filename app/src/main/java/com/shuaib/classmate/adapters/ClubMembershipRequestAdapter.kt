package com.shuaib.classmate.adapters

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.shuaib.classmate.databinding.ItemClubMembershipRequestBinding
import com.shuaib.classmate.models.ClubMembershipRequest

class ClubMembershipRequestAdapter(
    private var items: List<ClubMembershipRequest>,
    private val onApproveClick: (ClubMembershipRequest) -> Unit
) : RecyclerView.Adapter<ClubMembershipRequestAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemClubMembershipRequestBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemClubMembershipRequestBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val req = items[position]
        val b = holder.binding

        b.tvRequestClubName.text = if (req.clubName.isNotBlank()) req.clubName else "Campus Organization"
        b.tvMembershipApplicantName.text = req.fullName
        b.tvMembershipApplicantId.text = if (req.studentId.isNotBlank()) "ID: ${req.studentId}" else "ID: N/A"
        b.tvMembershipApplicantDept.text = if (req.department.isNotBlank()) req.department else "General"
        b.tvMembershipApplicantEmail.text = if (req.email.isNotBlank()) req.email else "No email"
        b.tvMembershipApplicantPhone.text = if (req.phone.isNotBlank()) req.phone else "No phone"

        if (req.status.equals("approved", ignoreCase = true)) {
            b.tvMembershipRequestStatus.text = "APPROVED"
            b.tvMembershipRequestStatus.setTextColor(Color.parseColor("#34D399"))
            b.tvMembershipRequestStatus.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#132B20"))
            b.btnApproveClubMembership.visibility = View.GONE
        } else {
            b.tvMembershipRequestStatus.text = "PENDING"
            b.tvMembershipRequestStatus.setTextColor(Color.parseColor("#F59E0B"))
            b.tvMembershipRequestStatus.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#3B2606"))
            b.btnApproveClubMembership.visibility = View.VISIBLE
            b.btnApproveClubMembership.setOnClickListener {
                onApproveClick(req)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newItems: List<ClubMembershipRequest>) {
        items = newItems
        notifyDataSetChanged()
    }
}
