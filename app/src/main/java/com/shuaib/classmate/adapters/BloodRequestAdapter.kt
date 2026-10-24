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
import com.shuaib.classmate.R
import com.shuaib.classmate.databinding.ItemBloodRequestBinding
import com.shuaib.classmate.models.BloodRequest
import com.shuaib.classmate.repositories.AppNotificationRepository
import com.shuaib.classmate.repositories.BloodRequestRepository
import com.shuaib.classmate.utils.AppPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BloodRequestAdapter(
    private var requests: List<BloodRequest>,
    private var currentUserId: String = "",
    private var currentUserName: String = "Student",
    private val onBloodDonatedClick: (BloodRequest) -> Unit = {},
    private val onReportClick: (BloodRequest) -> Unit = {}
) : RecyclerView.Adapter<BloodRequestAdapter.ViewHolder>() {

    private val repo = BloodRequestRepository()
    private val notificationRepo = AppNotificationRepository()

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

        b.tvBloodGroupBadge.text = "🩸 ${req.bloodGroup} URGENT"
        b.tvBagsNeededBadge.text = "${req.bagsNeeded} ${if (req.bagsNeeded > 1) "Bags" else "Bag"}"

        if (req.isDonated || req.isFulfilled) {
            b.tvDonatedBadge.visibility = View.VISIBLE
        } else {
            b.tvDonatedBadge.visibility = View.GONE
        }

        b.tvRequestTime.text = formatItemTime(req.timestamp?.seconds)
        b.tvHospitalName.text = req.hospitalName.ifBlank { "Local Hospital" }

        if (req.urgencyNote.isNotBlank()) {
            b.tvUrgencyNote.visibility = View.VISIBLE
            b.tvUrgencyNote.text = req.urgencyNote
        } else {
            b.tvUrgencyNote.visibility = View.GONE
        }

        b.tvRequesterInfo.text = "Requested by ${req.requesterName.ifBlank { "Student" }}"
        b.tvBloodPhone.text = "Contact: ${req.contactPhone}"

        // View count tracking (including guests)
        val prefs = AppPreferences(context)
        if (!prefs.hasViewedPost(req.id)) {
            prefs.markPostViewed(req.id)
            repo.recordView(req.id, currentUserId.ifBlank { null })
        }
        val displayViews = Math.max(req.viewsCount, if (prefs.hasViewedPost(req.id)) 1 else 0)
        b.tvBloodViewsCount.text = "$displayViews ${if (displayViews == 1) "view" else "views"}"

        // Love react setup
        val isLovedByMe = currentUserId.isNotBlank() && req.loveReactors.contains(currentUserId)
        if (isLovedByMe) {
            b.ivBloodLove.setImageResource(R.drawable.ic_heart_filled)
            b.ivBloodLove.imageTintList = ColorStateList.valueOf(Color.parseColor("#F43F5E"))
            b.tvBloodLoveCount.setTextColor(Color.parseColor("#FDA4AF"))
        } else {
            b.ivBloodLove.setImageResource(R.drawable.ic_heart_outline)
            b.ivBloodLove.imageTintList = ColorStateList.valueOf(Color.parseColor("#94A3B8"))
            b.tvBloodLoveCount.setTextColor(Color.parseColor("#CBD5E1"))
        }
        b.tvBloodLoveCount.text = "${req.loveCount}"

        b.btnBloodLove.setOnClickListener {
            if (currentUserId.isBlank()) {
                Toast.makeText(context, "Please sign in to react to this request", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val willLove = !req.loveReactors.contains(currentUserId)
            val updatedReactors = req.loveReactors.toMutableList()
            if (willLove) {
                updatedReactors.add(currentUserId)
                b.ivBloodLove.setImageResource(R.drawable.ic_heart_filled)
                b.ivBloodLove.imageTintList = ColorStateList.valueOf(Color.parseColor("#F43F5E"))
                b.tvBloodLoveCount.setTextColor(Color.parseColor("#FDA4AF"))
                b.tvBloodLoveCount.text = "${updatedReactors.size}"

                // Send notification to requester
                notificationRepo.sendNotificationToAuthor(
                    authorId = req.requesterId,
                    type = "react",
                    title = "❤️ New Support on Blood Request",
                    message = "$currentUserName reacted with love to your blood request for ${req.bloodGroup} at ${req.hospitalName}.",
                    postId = req.id,
                    postType = "blood_request",
                    actorId = currentUserId,
                    actorName = currentUserName
                )
            } else {
                updatedReactors.remove(currentUserId)
                b.ivBloodLove.setImageResource(R.drawable.ic_heart_outline)
                b.ivBloodLove.imageTintList = ColorStateList.valueOf(Color.parseColor("#94A3B8"))
                b.tvBloodLoveCount.setTextColor(Color.parseColor("#CBD5E1"))
                b.tvBloodLoveCount.text = "${updatedReactors.size}"
            }
            repo.toggleLove(req.id, currentUserId, willLove)
        }

        // Share button setup
        b.btnBloodShare.setOnClickListener {
            repo.recordShare(req.id)
            val shareText = "🚨 GSTU Go - URGENT BLOOD NEEDED 🚨\n" +
                    "🩸 Blood Group: ${req.bloodGroup}\n" +
                    "🏥 Hospital: ${req.hospitalName}\n" +
                    "📦 Bags Needed: ${req.bagsNeeded}\n" +
                    "${if (req.urgencyNote.isNotBlank()) "📝 Note: ${req.urgencyNote}\n" else ""}" +
                    "📞 Contact: ${req.contactPhone}\n\n" +
                    "Please reach out or share to help save a life!"
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, shareText)
                type = "text/plain"
            }
            context.startActivity(Intent.createChooser(sendIntent, "Share Blood Request"))

            // Send notification to requester
            if (currentUserId.isNotBlank()) {
                notificationRepo.sendNotificationToAuthor(
                    authorId = req.requesterId,
                    type = "share",
                    title = "🔗 Your Blood Request was Shared",
                    message = "$currentUserName shared your emergency blood request for ${req.bloodGroup}.",
                    postId = req.id,
                    postType = "blood_request",
                    actorId = currentUserId,
                    actorName = currentUserName
                )
            }
        }

        // Call option
        b.btnBloodCallAction.setOnClickListener {
            dialPhone(context, req.contactPhone)
        }
        b.layoutBloodContact.setOnClickListener {
            dialPhone(context, req.contactPhone)
        }

        // Three-Dot More Options Menu: "Blood Donated" & "Report Post"
        b.btnMoreBloodOptions.setOnClickListener { v ->
            val popup = PopupMenu(context, v)
            popup.menu.add(0, 1, 0, "Blood Donated")
            popup.menu.add(0, 2, 1, "Report Post")

            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    1 -> {
                        onBloodDonatedClick(req)
                        true
                    }
                    2 -> {
                        onReportClick(req)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }

    private fun dialPhone(context: Context, phoneStr: String) {
        val digits = phoneStr.filter { it.isDigit() || it == '+' }
        if (digits.length >= 5) {
            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$digits"))
            context.startActivity(dialIntent)
        } else {
            Toast.makeText(context, "Contact: $phoneStr", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount(): Int = requests.size

    fun updateList(newRequests: List<BloodRequest>) {
        requests = newRequests
        notifyDataSetChanged()
    }

    fun setUserInfo(userId: String, userName: String) {
        currentUserId = userId
        currentUserName = userName.ifBlank { "Student" }
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
