package com.shuaib.classmate.adapters

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.shuaib.classmate.databinding.ItemEventRegistrationRequestBinding
import com.shuaib.classmate.models.EventRegistration

class EventRegistrationAdapter(
    private var items: List<EventRegistration>,
    private val onConfirmClick: (EventRegistration) -> Unit
) : RecyclerView.Adapter<EventRegistrationAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemEventRegistrationRequestBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemEventRegistrationRequestBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val reg = items[position]
        val b = holder.binding

        b.tvRequestEventTitle.text = if (reg.eventTitle.isNotBlank()) reg.eventTitle else "Campus Event"
        b.tvApplicantName.text = reg.fullName
        b.tvApplicantStudentId.text = if (reg.studentId.isNotBlank()) "ID: ${reg.studentId}" else "ID: N/A"
        b.tvApplicantDepartment.text = if (reg.department.isNotBlank()) reg.department else "General"
        b.tvApplicantEmail.text = if (reg.email.isNotBlank()) reg.email else "No email"
        b.tvApplicantPhone.text = if (reg.phone.isNotBlank()) reg.phone else "No phone"

        if (reg.status.equals("confirmed", ignoreCase = true)) {
            b.tvRequestStatus.text = "CONFIRMED"
            b.tvRequestStatus.setTextColor(Color.parseColor("#34D399"))
            b.tvRequestStatus.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#132B20"))
            b.btnConfirmRegistration.visibility = View.GONE
        } else {
            b.tvRequestStatus.text = "PENDING"
            b.tvRequestStatus.setTextColor(Color.parseColor("#F59E0B"))
            b.tvRequestStatus.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#3B2606"))
            b.btnConfirmRegistration.visibility = View.VISIBLE
            b.btnConfirmRegistration.setOnClickListener {
                onConfirmClick(reg)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newItems: List<EventRegistration>) {
        items = newItems
        notifyDataSetChanged()
    }
}
