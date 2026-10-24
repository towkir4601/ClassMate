package com.shuaib.classmate.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.shuaib.classmate.databinding.ItemRoutineAssignmentBinding
import com.shuaib.classmate.models.ScheduleAssignment
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class RoutineAssignmentAdapter : ListAdapter<ScheduleAssignment, RoutineAssignmentAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRoutineAssignmentBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemRoutineAssignmentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ScheduleAssignment) {
            val startStr = formatSlotTime(item.slot_start)
            val endStr = formatSlotTime(item.slot_end)
            val start12 = formatTo12Hour(startStr)
            val end12 = formatTo12Hour(endStr)

            val dayLabel = item.day.uppercase(Locale.US)
            binding.tvAssignmentTime.text = "$dayLabel · $start12 - $end12"
            binding.tvAssignmentBatch.text = item.year_sem.ifBlank { "Batch" }

            binding.tvCourseCode.text = item.course_code.ifBlank { "Unassigned" }
            if (!item.course_name.isNullOrBlank() && item.course_name != item.course_code) {
                binding.tvCourseName.isVisible = true
                binding.tvCourseName.text = item.course_name
            } else {
                binding.tvCourseName.isVisible = false
            }

            binding.tvTeacher.text = item.teacher_abbr.ifBlank { "TBD" }
            binding.tvRoom.text = if (item.room_id.isNotBlank()) "Room ${item.room_id}" else "Room TBD"
        }

        private fun formatSlotTime(raw: Any?): String {
            if (raw == null) return "00:00"
            if (raw is Number) {
                val mins = raw.toInt()
                val h = mins / 60
                val m = mins % 60
                return String.format(Locale.US, "%02d:%02d", h, m)
            }
            val s = raw.toString().trim()
            if (s.matches(Regex("^\\d+$"))) {
                val mins = s.toIntOrNull() ?: 0
                val h = mins / 60
                val m = mins % 60
                return String.format(Locale.US, "%02d:%02d", h, m)
            }
            if (s.contains(":")) {
                val parts = s.split(":")
                val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
                val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
                return String.format(Locale.US, "%02d:%02d", h, m)
            }
            return s
        }

        private fun formatTo12Hour(time24: String): String {
            return try {
                val time = LocalTime.parse(time24)
                val formatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.US)
                time.format(formatter)
            } catch (_: Exception) {
                time24
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<ScheduleAssignment>() {
        override fun areItemsTheSame(oldItem: ScheduleAssignment, newItem: ScheduleAssignment): Boolean {
            return oldItem.course_code == newItem.course_code &&
                    oldItem.day == newItem.day &&
                    oldItem.slot_start.toString() == newItem.slot_start.toString() &&
                    oldItem.year_sem == newItem.year_sem
        }

        override fun areContentsTheSame(oldItem: ScheduleAssignment, newItem: ScheduleAssignment): Boolean {
            return oldItem == newItem
        }
    }
}
