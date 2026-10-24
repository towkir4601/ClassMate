package com.shuaib.classmate.adapters

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.shuaib.classmate.R
import com.shuaib.classmate.databinding.ItemCalendarEventCardBinding
import com.shuaib.classmate.models.AcademicCalendarException
import com.shuaib.classmate.models.CampusEvent
import java.time.DayOfWeek
import java.time.LocalDate

sealed class CalendarFeedItem {
    data class HolidayItem(val exception: AcademicCalendarException) : CalendarFeedItem()
    data class EventItem(val event: CampusEvent) : CalendarFeedItem()
    data class WeekendItem(val date: LocalDate) : CalendarFeedItem()
}

class CalendarEventAdapter(
    private val onHolidayClick: (AcademicCalendarException) -> Unit,
    private val onHolidayLongClick: (AcademicCalendarException) -> Unit,
    private val onEventClick: (CampusEvent) -> Unit,
    private val onEventLongClick: (CampusEvent) -> Unit
) : RecyclerView.Adapter<CalendarEventAdapter.FeedViewHolder>() {

    private var items: List<CalendarFeedItem> = emptyList()

    inner class FeedViewHolder(val binding: ItemCalendarEventCardBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedViewHolder {
        val binding = ItemCalendarEventCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FeedViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FeedViewHolder, position: Int) {
        val item = items[position]
        val b = holder.binding
        val context = b.root.context

        when (item) {
            is CalendarFeedItem.HolidayItem -> {
                val ex = item.exception
                val isClassSuspended = ex.type == AcademicCalendarException.TYPE_CLASS_SUSPENDED
                val accentColor = ContextCompat.getColor(
                    context,
                    if (isClassSuspended) R.color.cal_class_holiday else R.color.cal_full_holiday
                )

                b.viewAccentStrip.backgroundTintList = ColorStateList.valueOf(accentColor)
                b.tvTitle.text = ex.title.ifBlank { if (isClassSuspended) "Classes Suspended" else "University Holiday" }

                val badgeText = when (ex.type) {
                    AcademicCalendarException.TYPE_CLASS_SUSPENDED -> "CLASSES SUSPENDED"
                    AcademicCalendarException.TYPE_HOLIDAY -> "UNIVERSITY HOLIDAY"
                    else -> "VACATION"
                }
                b.tvBadge.text = badgeText
                b.tvBadge.setTextColor(accentColor)
                b.tvBadge.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        context,
                        if (isClassSuspended) R.color.cal_class_holiday_bg else R.color.cal_full_holiday_bg
                    )
                )

                val dateStr = if (ex.startDate == ex.endDate) ex.startDate else "${ex.startDate} – ${ex.endDate}"
                b.tvDateTime.text = dateStr

                val scopeText = if (ex.targetBatches.isEmpty()) "• All Batches" else "• Semesters: ${ex.targetBatches.joinToString(", ")}"
                b.tvScope.text = scopeText
                b.tvScope.visibility = View.VISIBLE

                b.tvDescription.text = ex.reason.ifBlank { "Classes and university operations modified for this duration." }
                b.tvDescription.visibility = View.VISIBLE

                b.root.setOnClickListener { onHolidayClick(ex) }
                b.root.setOnLongClickListener {
                    onHolidayLongClick(ex)
                    true
                }
            }

            is CalendarFeedItem.EventItem -> {
                val ev = item.event
                val accentColor = ContextCompat.getColor(context, R.color.cal_event_amber)

                b.viewAccentStrip.backgroundTintList = ColorStateList.valueOf(accentColor)
                b.tvTitle.text = ev.title

                b.tvBadge.text = ev.status.ifBlank { "CAMPUS EVENT" }
                b.tvBadge.setTextColor(accentColor)
                b.tvBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#25F59E0B"))

                val timeStr = ev.dateTime.ifBlank {
                    if (ev.time.isNotBlank()) "${ev.date} • ${ev.time}" else ev.date
                }
                b.tvDateTime.text = timeStr

                val organizer = ev.organizer.ifBlank { "Campus" }
                b.tvScope.text = "• $organizer"
                b.tvScope.visibility = View.VISIBLE

                val details = buildString {
                    if (ev.location.isNotBlank()) {
                        append("📍 ${ev.location}")
                    }
                    if (ev.description.isNotBlank()) {
                        if (isNotEmpty()) append("\n")
                        append(ev.description)
                    }
                }
                b.tvDescription.text = details.ifBlank { "Campus club event / gathering." }
                b.tvDescription.visibility = View.VISIBLE

                b.root.setOnClickListener { onEventClick(ev) }
                b.root.setOnLongClickListener {
                    onEventLongClick(ev)
                    true
                }
            }

            is CalendarFeedItem.WeekendItem -> {
                val dayName = if (item.date.dayOfWeek == DayOfWeek.FRIDAY) "Friday" else "Saturday"
                val accentColor = ContextCompat.getColor(context, R.color.cal_full_holiday)

                b.viewAccentStrip.backgroundTintList = ColorStateList.valueOf(accentColor)
                b.tvTitle.text = "Weekly University Weekend ($dayName)"
                b.tvBadge.text = "FULL HOLIDAY"
                b.tvBadge.setTextColor(accentColor)
                b.tvBadge.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.cal_full_holiday_bg)
                )

                b.tvDateTime.text = item.date.toString()
                b.tvScope.text = "• All Departments"
                b.tvScope.visibility = View.VISIBLE

                b.tvDescription.text = "Official weekly university holiday. No regular classes or lab sessions."
                b.tvDescription.visibility = View.VISIBLE

                b.root.setOnClickListener(null)
                b.root.setOnLongClickListener(null)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun submitItems(newItems: List<CalendarFeedItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
