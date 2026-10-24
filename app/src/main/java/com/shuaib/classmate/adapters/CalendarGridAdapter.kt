package com.shuaib.classmate.adapters

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.shuaib.classmate.R
import com.shuaib.classmate.databinding.ItemCalendarDayBinding
import com.shuaib.classmate.models.AcademicCalendarException
import com.shuaib.classmate.models.CampusEvent
import java.time.DayOfWeek
import java.time.LocalDate

data class CalendarDay(
    val date: LocalDate,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val isSelected: Boolean,
    val isWeekend: Boolean, // Friday or Saturday (Bangladesh weekend)
    val holiday: AcademicCalendarException?,
    val events: List<CampusEvent> = emptyList()
)

class CalendarGridAdapter(
    private val onDateClick: (LocalDate) -> Unit
) : RecyclerView.Adapter<CalendarGridAdapter.DayViewHolder>() {

    private var days: List<CalendarDay> = emptyList()

    inner class DayViewHolder(val binding: ItemCalendarDayBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val binding = ItemCalendarDayBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DayViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val item = days[position]
        val b = holder.binding
        val context = b.root.context

        b.tvDayNumber.text = item.date.dayOfMonth.toString()

        // 1. Reset states
        b.vSelection.visibility = if (item.isSelected) View.VISIBLE else View.GONE
        b.vTodayRing.visibility = if (item.isToday) View.VISIBLE else View.GONE
        b.dotEvent.visibility = if (item.events.isNotEmpty()) View.VISIBLE else View.GONE

        // 2. Styling according to month, holiday, weekend, and regular day
        if (!item.isCurrentMonth) {
            // Days from previous or next month (dimmed)
            b.vCellBg.setBackgroundColor(Color.TRANSPARENT)
            b.tvDayNumber.setTextColor(ContextCompat.getColor(context, R.color.cm_text_muted))
            b.tvDayNumber.alpha = 0.35f
            b.root.alpha = 0.4f
        } else {
            b.root.alpha = 1.0f
            b.tvDayNumber.alpha = 1.0f

            val holiday = item.holiday
            if (holiday != null && holiday.isActive) {
                if (holiday.type == AcademicCalendarException.TYPE_CLASS_SUSPENDED) {
                    // Class Holiday / Suspended -> Blue squircle (Pic 1 style)
                    b.vCellBg.setBackgroundResource(R.drawable.bg_calendar_class_holiday)
                    b.tvDayNumber.setTextColor(Color.WHITE)
                } else {
                    // Vacation or Full Holiday -> Red squircle (Pic 1 style)
                    b.vCellBg.setBackgroundResource(R.drawable.bg_calendar_full_holiday)
                    b.tvDayNumber.setTextColor(Color.WHITE)
                }
            } else if (item.isWeekend) {
                // University Weekend (Friday & Saturday) -> Red squircle with white text (Pic 1 GSTU Calendar)
                b.vCellBg.setBackgroundResource(R.drawable.bg_calendar_full_holiday)
                b.tvDayNumber.setTextColor(Color.WHITE)
            } else {
                // Regular working day -> Modern clean card surface
                b.vCellBg.setBackgroundResource(R.drawable.bg_calendar_cell_default)
                if (item.isToday) {
                    b.tvDayNumber.setTextColor(ContextCompat.getColor(context, R.color.cal_today_gold))
                } else {
                    b.tvDayNumber.setTextColor(ContextCompat.getColor(context, R.color.cm_text_primary))
                }
            }
        }

        b.root.setOnClickListener {
            onDateClick(item.date)
        }
    }

    override fun getItemCount(): Int = days.size

    fun submitDays(newDays: List<CalendarDay>) {
        days = newDays
        notifyDataSetChanged()
    }
}
