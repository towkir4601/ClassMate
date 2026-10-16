package com.shuaib.classmate.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.shuaib.classmate.R
import com.shuaib.classmate.databinding.ItemDeadlineEventBinding
import com.shuaib.classmate.models.Assignment
import com.shuaib.classmate.models.Notice

data class DeadlineItem(
    val id: String,
    val type: String, // assignment, quiz, exam, event, calendar
    val badgeText: String,
    val sourceText: String,
    val title: String,
    val subtitle: String,
    val dateString: String,
    val daysRemaining: Long,
    val formattedDue: String,
    val isUserCountdown: Boolean = false,
    val notice: Notice? = null,
    val assignment: Assignment? = null
)

class DeadlinesBottomSheetAdapter(
    private var items: List<DeadlineItem>,
    private val onDelete: (DeadlineItem) -> Unit,
    private val onViewNotice: (Notice) -> Unit,
    private val onItemClick: (DeadlineItem) -> Unit
) : RecyclerView.Adapter<DeadlinesBottomSheetAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemDeadlineEventBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDeadlineEventBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val b = holder.binding
        val ctx = holder.itemView.context

        b.tvDeadlineBadge.text = item.badgeText
        b.tvDeadlineSource.text = item.sourceText
        b.tvDeadlineTitle.text = item.title
        b.tvDeadlineSubtitle.text = item.subtitle
        b.tvDeadlineDueDate.text = item.formattedDue

        // Icon & tint
        val iconRes = when (item.type) {
            "quiz", "class_test" -> R.drawable.ic_clock
            "exam" -> R.drawable.ic_school
            "calendar", "event" -> R.drawable.ic_calendar
            else -> R.drawable.ic_assignment
        }
        b.ivDeadlineIcon.setImageResource(iconRes)

        // Urgency badge
        when {
            item.daysRemaining < 0L -> {
                b.tvDeadlineUrgency.text = "OVERDUE"
                b.tvDeadlineUrgency.setBackgroundResource(R.drawable.bg_badge_red)
                b.tvDeadlineUrgency.setTextColor(Color.parseColor("#EF4444"))
            }
            item.daysRemaining == 0L -> {
                b.tvDeadlineUrgency.text = "DUE TODAY"
                b.tvDeadlineUrgency.setBackgroundResource(R.drawable.bg_badge_red)
                b.tvDeadlineUrgency.setTextColor(Color.parseColor("#EF4444"))
            }
            item.daysRemaining == 1L -> {
                b.tvDeadlineUrgency.text = "DUE TOMORROW"
                b.tvDeadlineUrgency.setBackgroundResource(R.drawable.bg_badge_purple)
                b.tvDeadlineUrgency.setTextColor(Color.parseColor("#F59E0B"))
            }
            item.daysRemaining <= 3L -> {
                b.tvDeadlineUrgency.text = "IN ${item.daysRemaining} DAYS"
                b.tvDeadlineUrgency.setBackgroundResource(R.drawable.bg_badge_purple)
                b.tvDeadlineUrgency.setTextColor(Color.parseColor("#F59E0B"))
            }
            else -> {
                b.tvDeadlineUrgency.text = "IN ${item.daysRemaining} DAYS"
                b.tvDeadlineUrgency.setBackgroundResource(R.drawable.bg_badge_blue)
                b.tvDeadlineUrgency.setTextColor(Color.parseColor("#3B82F6"))
            }
        }

        // Action buttons
        if (item.isUserCountdown) {
            b.btnDeleteDeadline.visibility = View.VISIBLE
            b.btnViewAction.visibility = View.GONE
            b.btnDeleteDeadline.setOnClickListener { onDelete(item) }
        } else if (item.notice != null) {
            b.btnDeleteDeadline.visibility = View.GONE
            b.btnViewAction.visibility = View.VISIBLE
            b.btnViewAction.setOnClickListener { onViewNotice(item.notice) }
        } else {
            b.btnDeleteDeadline.visibility = View.GONE
            b.btnViewAction.visibility = View.GONE
        }

        b.root.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newItems: List<DeadlineItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
