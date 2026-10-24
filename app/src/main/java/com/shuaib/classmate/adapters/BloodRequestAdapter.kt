package com.shuaib.classmate.adapters

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.shuaib.classmate.databinding.ItemBloodRequestBinding
import com.shuaib.classmate.models.BloodRequest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BloodRequestAdapter(
    private var requests: List<BloodRequest>,
    private var currentUserId: String = "",
    private var isAdmin: Boolean = false,
    private val onFulfillClick: (BloodRequest) -> Unit = {},
    private val onDeleteClick: (BloodRequest) -> Unit = {}
) : RecyclerView.Adapter<BloodRequestAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemBloodRequestBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBloodRequestBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val req = requests[position]
        val b = holder.binding
        val context = holder.itemView.context

        b.tvBloodGroupBadge.text = "🚨 ${req.bloodGroup} NEEDED"

        val bagText = if (req.bagsNeeded > 1) "${req.bagsNeeded} Bags Needed" else "1 Bag Needed"
        b.tvBagsBadge.text = bagText

        b.tvHospitalTitle.text = req.hospitalName.ifBlank { "Hospital in Gopalganj" }

        if (req.patientName.isNotBlank()) {
            b.tvPatientDetails.visibility = View.VISIBLE
            b.tvPatientDetails.text = "Patient: ${req.patientName}"
        } else {
            b.tvPatientDetails.visibility = View.GONE
        }

        if (req.urgencyNote.isNotBlank()) {
            b.tvUrgencyNote.visibility = View.VISIBLE
            b.tvUrgencyNote.text = "Urgency Note: ${req.urgencyNote}"
        } else {
            b.tvUrgencyNote.visibility = View.GONE
        }

        val requester = req.requesterName.ifBlank { "Student" }
        b.tvRequesterInfo.text = "Posted by $requester · Contact: ${req.contactPhone}"

        b.tvRequestTime.text = formatRequestTime(req.timestamp?.seconds)

        // Actions: Call & WhatsApp
        b.btnCallDonor.setOnClickListener {
            handleCall(context, req.contactPhone)
        }

        b.btnWhatsApp.setOnClickListener {
            handleWhatsApp(context, req.contactPhone, req.bloodGroup, req.hospitalName)
        }

        // Fulfill / Delete buttons for Author or Admin
        val isAuthor = req.requesterId.isNotBlank() && req.requesterId == currentUserId
        val canManage = isAuthor || isAdmin

        b.btnMarkFulfilled.visibility = if (canManage) View.VISIBLE else View.GONE
        b.btnDeleteRequest.visibility = if (canManage) View.VISIBLE else View.GONE

        b.btnMarkFulfilled.setOnClickListener {
            onFulfillClick(req)
        }

        b.btnDeleteRequest.setOnClickListener {
            onDeleteClick(req)
        }
    }

    private fun handleCall(context: Context, phone: String) {
        val digits = phone.filter { it.isDigit() || it == '+' }
        if (digits.length >= 5) {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$digits"))
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "Contact: $phone", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleWhatsApp(context: Context, phone: String, bloodGroup: String, hospital: String) {
        val digits = phone.filter { it.isDigit() }
        if (digits.length < 7) {
            Toast.makeText(context, "Invalid phone number for WhatsApp", Toast.LENGTH_SHORT).show()
            return
        }

        val formattedPhone = when {
            digits.startsWith("880") -> digits
            digits.startsWith("0") -> "88$digits"
            else -> "880$digits"
        }

        val message = "Hello, I saw your urgent request on GSTU Go for $bloodGroup blood at $hospital. I want to help."
        val url = "https://api.whatsapp.com/send?phone=$formattedPhone&text=${Uri.encode(message)}"

        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                setPackage("com.whatsapp")
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                // Fallback to browser or any WA client
                val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(fallbackIntent)
            } catch (_: Exception) {
                Toast.makeText(context, "WhatsApp is not installed on this device", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun getItemCount(): Int = requests.size

    fun updateList(newRequests: List<BloodRequest>) {
        requests = newRequests
        notifyDataSetChanged()
    }

    fun setUserAndRole(userId: String, adminStatus: Boolean) {
        currentUserId = userId
        isAdmin = adminStatus
        notifyDataSetChanged()
    }

    private fun formatRequestTime(seconds: Long?): String {
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
