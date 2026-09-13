package com.shuaib.classmate.activities

import android.app.TimePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.shuaib.classmate.R
import com.shuaib.classmate.adapters.PeriodAdapter
import com.shuaib.classmate.databinding.ActivityTimetableManagementBinding
import com.shuaib.classmate.databinding.DialogAddPeriodBinding
import com.shuaib.classmate.models.Period
import com.shuaib.classmate.repositories.TimetableRepository
import com.shuaib.classmate.utils.DateHelper
import com.shuaib.classmate.utils.SubjectList
import com.shuaib.classmate.utils.ThemeColors
import com.shuaib.classmate.utils.WidgetUpdater
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.activity.result.contract.ActivityResultContracts
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Calendar

class TimetableManagementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTimetableManagementBinding
    private lateinit var firestore: FirebaseFirestore
    private lateinit var periodAdapter: PeriodAdapter
    private val periodList = mutableListOf<Period>()
    private var currentDay = "saturday"

    private val days = listOf("saturday", "sunday", "monday", "tuesday", "wednesday", "thursday", "friday")
    private val dayShort = listOf("SAT", "SUN", "MON", "TUE", "WED", "THU", "FRI")
    private val dayFull = listOf("Saturday", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday")

    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            extractRoutineFromUri(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTimetableManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firestore = FirebaseFirestore.getInstance()
        setupRecyclerView()
        setupDaySelector()
        setupSwipeToDelete()

        binding.toolbar.setNavigationOnClickListener { 
            finish()
        }

        binding.btnClearAll.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Delete All Classes?")
                .setMessage("Are you sure you want to delete ALL classes for all days? This cannot be undone.")
                .setPositiveButton("Delete All") { _, _ ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            for (day in days) {
                                val docs = firestore.collection("timetable").document(day).collection("periods").get().await()
                                val batch = firestore.batch()
                                for (doc in docs) batch.delete(doc.reference)
                                batch.commit().await()
                            }
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@TimetableManagementActivity, "All classes deleted", Toast.LENGTH_SHORT).show()
                                fetchTimetable(currentDay)
                                days.forEach { refreshWidgetAfterTimetableChange(it) }
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@TimetableManagementActivity, "Error deleting: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.fabAddPeriod.setOnClickListener { showPeriodDialog(null) }
    }

    private fun setupRecyclerView() {
        periodAdapter = PeriodAdapter(
            periods = periodList,
            onPeriodClick = { period -> showPeriodDialog(period) }
        )

        binding.rvTimetable.apply {
            layoutManager = LinearLayoutManager(this@TimetableManagementActivity)
            adapter = periodAdapter
        }
    }

    private fun setupDaySelector() {
        val todayIndex = getTodayIndex()
        currentDay = days[todayIndex]
        fetchTimetable(currentDay)

        val weekDates = getWeekDates()
        val margin = (6 * resources.displayMetrics.density).toInt()
        val inflater = LayoutInflater.from(this)

        binding.daySelector.removeAllViews()
        days.forEachIndexed { index, _ ->
            val cardView = inflater.inflate(R.layout.item_day_card, binding.daySelector, false)

            cardView.findViewById<TextView>(R.id.tvDayShort).text = dayShort[index]
            cardView.findViewById<TextView>(R.id.tvDayDate).text =
                String.format("%02d", weekDates[index])

            val params = cardView.layoutParams as LinearLayout.LayoutParams
            params.setMargins(margin, margin / 2, margin, margin / 2)
            cardView.layoutParams = params

            cardView.setOnClickListener {
                selectDay(index)
            }

            binding.daySelector.addView(cardView)
        }

        applyDayCardStyles(todayIndex)
    }

    private fun selectDay(index: Int) {
        currentDay = days[index]
        fetchTimetable(currentDay)
        applyDayCardStyles(index)
    }

    private fun applyDayCardStyles(selectedIndex: Int) {
        val todayIndex = getTodayIndex()
        for (i in 0 until binding.daySelector.childCount) {
            val cardView = binding.daySelector.getChildAt(i)
            val tvDayShort = cardView.findViewById<TextView>(R.id.tvDayShort)
            val tvDayDate = cardView.findViewById<TextView>(R.id.tvDayDate)
            val vIndicator = cardView.findViewById<View>(R.id.vDayIndicator)

            when (i) {
                selectedIndex -> {
                    cardView.setBackgroundResource(R.drawable.bg_day_card_selected)
                    cardView.elevation = 5 * resources.displayMetrics.density
                    tvDayShort.setTextColor(0xCCFFFFFF.toInt())
                    tvDayDate.setTextColor(Color.WHITE)
                    vIndicator.visibility = View.VISIBLE
                }
                todayIndex -> {
                    cardView.setBackgroundResource(R.drawable.bg_day_card_unselected)
                    cardView.elevation = 2 * resources.displayMetrics.density
                    tvDayShort.setTextColor(ThemeColors.primary(this))
                    tvDayDate.setTextColor(ThemeColors.primary(this))
                    vIndicator.visibility = View.INVISIBLE
                }
                else -> {
                    cardView.setBackgroundResource(R.drawable.bg_day_card_unselected)
                    cardView.elevation = 2 * resources.displayMetrics.density
                    tvDayShort.setTextColor(ThemeColors.textDisabled(this))
                    tvDayDate.setTextColor(ThemeColors.textPrimary(this))
                    vIndicator.visibility = View.INVISIBLE
                }
            }
        }
    }

    private fun getTodayIndex(): Int {
        val calendar = Calendar.getInstance()
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SATURDAY -> 0
            Calendar.SUNDAY -> 1
            Calendar.MONDAY -> 2
            Calendar.TUESDAY -> 3
            Calendar.WEDNESDAY -> 4
            Calendar.THURSDAY -> 5
            Calendar.FRIDAY -> 6
            else -> 0
        }
    }

    private fun getWeekDates(): IntArray {
        val today = LocalDate.now()
        val currentDayOfWeek = today.dayOfWeek.value
        
        val stepsToSaturday = when (currentDayOfWeek) {
            DayOfWeek.SATURDAY.value -> 0
            DayOfWeek.SUNDAY.value -> -1
            DayOfWeek.MONDAY.value -> -2
            DayOfWeek.TUESDAY.value -> -3
            DayOfWeek.WEDNESDAY.value -> -4
            DayOfWeek.THURSDAY.value -> -5
            DayOfWeek.FRIDAY.value -> -6
            else -> 0
        }
        
        val dates = IntArray(7)
        val saturdayDate = today.plusDays(stepsToSaturday.toLong())
        for (i in 0..6) {
            dates[i] = saturdayDate.plusDays(i.toLong()).dayOfMonth
        }
        return dates
    }

    private fun setupSwipeToDelete() {
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = false
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val period = periodList[position]
                showDeleteConfirmation(period, position)
            }
        }
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(binding.rvTimetable)
    }

    private fun showDeleteConfirmation(period: Period, position: Int) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Period")
            .setMessage("Are you sure you want to delete ${period.subject}?")
            .setPositiveButton("Delete") { _, _ -> deletePeriod(period) }
            .setNegativeButton("Cancel") { _, _ -> periodAdapter.notifyItemChanged(position) }
            .show()
    }

    private fun fetchTimetable(day: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmptyState.visibility = View.GONE

        firestore.collection("timetable").document(day)
            .collection("periods")
            .get()
            .addOnSuccessListener { documents ->
                binding.progressBar.visibility = View.GONE
                
                val dayIndex = days.indexOf(day.lowercase())
                val targetDateString = if (dayIndex != -1) getFullDateForIndex(dayIndex) else ""

                val fetchedPeriods = documents.mapNotNull { doc ->
                    val period = doc.toObject(Period::class.java).copy(id = doc.id)
                    if (period.isTemporary && period.temporaryDate.isNotBlank() && period.temporaryDate != targetDateString) {
                        firestore.collection("timetable").document(day).collection("periods").document(period.id).delete()
                        null
                    } else {
                        if (period.cancelDate == targetDateString) {
                            period.copy(isCancelled = true)
                        } else {
                            period.copy(isCancelled = false)
                        }
                    }
                }.sortedBy { it.startTime }

                periodList.clear()
                periodList.addAll(fetchedPeriods)

                if (periodList.isEmpty()) {
                    binding.tvEmptyState.visibility = View.VISIBLE
                    binding.rvTimetable.visibility = View.GONE
                } else {
                    binding.tvEmptyState.visibility = View.GONE
                    binding.rvTimetable.visibility = View.VISIBLE
                    val isToday = day == getTodayName()
                    val todayIndex = days.indexOf(getTodayName())
                    val isPastDay = days.indexOf(day.lowercase()) < todayIndex
                    periodAdapter.updateList(periodList, isToday, isPastDay)
                }
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun getTodayName(): String {
        return when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
            Calendar.SATURDAY -> "saturday"
            Calendar.SUNDAY -> "sunday"
            Calendar.MONDAY -> "monday"
            Calendar.TUESDAY -> "tuesday"
            Calendar.WEDNESDAY -> "wednesday"
            Calendar.THURSDAY -> "thursday"
            Calendar.FRIDAY -> "friday"
            else -> "saturday"
        }
    }

    private fun showPeriodDialog(period: Period?) {
        val dialogBinding = DialogAddPeriodBinding.inflate(LayoutInflater.from(this))
        val isEdit = period != null

        val subjectNames = SubjectList.subjects.map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, subjectNames)
                dialogBinding.dropdownSubject.setAdapter(adapter)
        
        if (period != null) {
            dialogBinding.layoutDaysSelection.visibility = View.GONE
            dialogBinding.dropdownSubject.setText("${period.subject}", false)
            dialogBinding.etTeacher.setText(period.teacher)
            dialogBinding.etRoom.setText(period.room)
            dialogBinding.etStartTime.setText(period.startTime)
            dialogBinding.etEndTime.setText(period.endTime)
            dialogBinding.cbTemporary.isChecked = period.isTemporary
        } else {
            dialogBinding.layoutDaysSelection.visibility = View.VISIBLE
            val chipId = when (currentDay) {
                "saturday" -> R.id.chipSat
                "sunday" -> R.id.chipSun
                "monday" -> R.id.chipMon
                "tuesday" -> R.id.chipTue
                "wednesday" -> R.id.chipWed
                "thursday" -> R.id.chipThu
                "friday" -> R.id.chipFri
                else -> R.id.chipSat
            }
            dialogBinding.chipGroupDays.check(chipId)
            
            dialogBinding.cbTemporary.setOnCheckedChangeListener { _, isChecked ->
                dialogBinding.layoutDaysSelection.visibility = if (isChecked) View.GONE else View.VISIBLE
            }
        }

        val batches = mutableListOf("all")
        for (i in 64 downTo 10) batches.add(i.toString())
        val batchAdapter = android.widget.ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, batches)
        dialogBinding.etBatch.setAdapter(batchAdapter)
        if (period == null) {
            val currentUserBatch = getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE).getString("current_batch", "all") ?: "all"
            dialogBinding.etBatch.setText(currentUserBatch, false)
        }
        dialogBinding.etStartTime.setOnClickListener {
            showTimePicker { time -> dialogBinding.etStartTime.setText(time) }
        }

        dialogBinding.etEndTime.setOnClickListener {
            showTimePicker { time -> dialogBinding.etEndTime.setText(time) }
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(if (isEdit) "Edit Period" else "Add New Period")
            .setView(dialogBinding.root)
            .setPositiveButton(if (isEdit) "Update" else "Add") { _, _ ->
                val selectedSubject = dialogBinding.dropdownSubject.text.toString().trim()
                val teacher = dialogBinding.etTeacher.text.toString().trim()
                val room = dialogBinding.etRoom.text.toString().trim()
                val start = dialogBinding.etStartTime.text.toString()
                val end = dialogBinding.etEndTime.text.toString()
                val batch = dialogBinding.etBatch.text.toString().trim()
                val isTemp = dialogBinding.cbTemporary.isChecked
                val tempDate = if (isTemp) getFullDateForIndex(days.indexOf(currentDay)) else ""

                if (selectedSubject.isNotEmpty() && teacher.isNotEmpty()) {
                    val updatedPeriod = period?.copy(
                        subject = selectedSubject,
                        teacher = teacher,
                        startTime = start,
                        endTime = end,
                        room = room,
                        batch = batch,
                        isTemporary = isTemp,
                        temporaryDate = tempDate
                    ) ?: Period(
                        id = "",
                        subject = selectedSubject,
                        teacher = teacher,
                        startTime = start,
                        endTime = end,
                        room = room,
                        batch = batch,
                        isTemporary = isTemp,
                        temporaryDate = tempDate
                    )
                    
                    com.shuaib.classmate.utils.NotificationSender.sendNoticeAlert(
                        title = "Routine Updated",
                        body = "The class routine has been updated."
                    )
                    
                    if (isEdit) {
                        savePeriod(updatedPeriod, true)
                    } else {
                        val selectedDays = mutableListOf<String>()
                        if (isTemp) {
                            selectedDays.add(currentDay)
                        } else {
                            val checkedIds = dialogBinding.chipGroupDays.checkedChipIds
                            for (id in checkedIds) {
                                when (id) {
                                    R.id.chipSat -> selectedDays.add("saturday")
                                    R.id.chipSun -> selectedDays.add("sunday")
                                    R.id.chipMon -> selectedDays.add("monday")
                                    R.id.chipTue -> selectedDays.add("tuesday")
                                    R.id.chipWed -> selectedDays.add("wednesday")
                                    R.id.chipThu -> selectedDays.add("thursday")
                                    R.id.chipFri -> selectedDays.add("friday")
                                }
                            }
                            if (selectedDays.isEmpty()) {
                                selectedDays.add(currentDay)
                            }
                        }
                        savePeriodsForMultipleDays(updatedPeriod, selectedDays)
                    }
                } else {
                    Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showTimePicker(onTimeSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        
        // Use 24-hour mode for the internal storage to keep sorting easy
        TimePickerDialog(this, { _, hour, minute ->
            val time24 = String.format("%02d:%02d", hour, minute)
            onTimeSelected(time24)
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show()
    }

    private fun timeToMinutes(timeStr: String): Int {
        return try {
            val format = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.US)
            val date = format.parse(timeStr)
            val calendar = java.util.Calendar.getInstance()
            calendar.time = date!!
            calendar.get(java.util.Calendar.HOUR_OF_DAY) * 60 + calendar.get(java.util.Calendar.MINUTE)
        } catch (e: Exception) {
            0
        }
    }

    private fun isOverlapping(start1: String, end1: String, start2: String, end2: String): Boolean {
        val s1 = timeToMinutes(start1)
        val e1 = timeToMinutes(end1)
        val s2 = timeToMinutes(start2)
        val e2 = timeToMinutes(end2)
        return (s1 < e2) && (s2 < e1)
    }

    private fun savePeriod(period: Period, isEdit: Boolean) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val collection = firestore.collection("timetable").document(currentDay).collection("periods")
                val existingDocs = collection.get().await()
                val batch = firestore.batch()

                for (doc in existingDocs) {
                    val existingPeriod = doc.toObject(Period::class.java)
                    if (isEdit && existingPeriod.id == period.id) continue
                    if (isOverlapping(period.startTime, period.endTime, existingPeriod.startTime, existingPeriod.endTime)) {
                        batch.delete(doc.reference)
                    }
                }

                val docRef = if (isEdit) collection.document(period.id) else collection.document()
                val periodToSave = if (isEdit) period else period.copy(id = docRef.id)
                batch.set(docRef, periodToSave)

                batch.commit().await()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@TimetableManagementActivity, if (isEdit) "Period updated" else "Period added", Toast.LENGTH_SHORT).show()
                    fetchTimetable(currentDay)
                    refreshWidgetAfterTimetableChange(currentDay)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@TimetableManagementActivity, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun savePeriodsForMultipleDays(period: Period, daysToSave: List<String>) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val batch = firestore.batch()
                for (day in daysToSave) {
                    val collection = firestore.collection("timetable").document(day).collection("periods")
                    val existingDocs = collection.get().await()

                    for (doc in existingDocs) {
                        val existingPeriod = doc.toObject(Period::class.java)
                        if (isOverlapping(period.startTime, period.endTime, existingPeriod.startTime, existingPeriod.endTime)) {
                            batch.delete(doc.reference)
                        }
                    }

                    val newDocRef = collection.document()
                    val periodToSave = period.copy(id = newDocRef.id)
                    batch.set(newDocRef, periodToSave)
                }

                batch.commit().await()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@TimetableManagementActivity, "Periods added successfully", Toast.LENGTH_SHORT).show()
                    fetchTimetable(currentDay)
                    daysToSave.forEach { refreshWidgetAfterTimetableChange(it) }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@TimetableManagementActivity, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun deletePeriod(period: Period) {
        firestore.collection("timetable").document(currentDay)
            .collection("periods").document(period.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Period deleted", Toast.LENGTH_SHORT).show()
                fetchTimetable(currentDay)
                refreshWidgetAfterTimetableChange(currentDay)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Delete failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun refreshWidgetAfterTimetableChange(day: String) {
        lifecycleScope.launch {
            runCatching {
                TimetableRepository.getInstance(this@TimetableManagementActivity)
                    .syncDayFromFirestore(day, Source.DEFAULT)
            }
            if (day == DateHelper.todayDayString()) {
                WidgetUpdater.refresh(this@TimetableManagementActivity, syncTodayTimetable = false)
            }
        }
    }

    private fun getFullDateForIndex(index: Int): String {
        val today = java.time.LocalDate.now()
        val todayIndex = getTodayIndex()
        val targetDate = if (todayIndex == 5 || todayIndex == 6) {
            val diff = (index - todayIndex + 7) % 7
            today.plusDays(diff.toLong())
        } else {
            val saturday = today.minusDays(todayIndex.toLong())
            saturday.plusDays(index.toLong())
        }
        return targetDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd", java.util.Locale.US))
    }

    private fun extractRoutineFromUri(uri: android.net.Uri) {
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            binding.tvEmptyState.text = "Extracting routine with AI...\nPlease wait."
            binding.tvEmptyState.visibility = View.VISIBLE
            periodList.clear()
            periodAdapter.notifyDataSetChanged()

            try {
                val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
                val inputStream = contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes()
                inputStream?.close()

                if (bytes == null) throw Exception("Failed to read file")
                
                val base64Data = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                val okHttpClient = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
                val aiProvider = com.shuaib.classmate.ai.GeminiAiProvider(okHttpClient, com.google.gson.Gson())
                val result = aiProvider.extractTimetable(base64Data, mimeType)
                
                result.onSuccess { routineMap ->
                    saveExtractedRoutine(routineMap)
                }.onFailure { e ->
                    Toast.makeText(this@TimetableManagementActivity, "Extraction failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@TimetableManagementActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                fetchTimetable(currentDay)
            }
        }
    }

    private suspend fun saveExtractedRoutine(routineMap: Map<String, List<Period>>) {
        try {
            val batch = firestore.batch()
            for (day in days) {
                val dayRef = firestore.collection("timetable").document(day).collection("periods")
                val existingDocs = dayRef.get().await()
                for (doc in existingDocs) {
                    batch.delete(doc.reference)
                }
                
                val newPeriods = routineMap[day] ?: emptyList()
                for (period in newPeriods) {
                    val newDocRef = dayRef.document()
                    val periodToSave = period.copy(id = newDocRef.id)
                    batch.set(newDocRef, periodToSave)
                }
            }
            batch.commit().await()
            withContext(Dispatchers.Main) {
                Toast.makeText(this@TimetableManagementActivity, "Class routine updated automatically!", Toast.LENGTH_LONG).show()
                refreshWidgetAfterTimetableChange(currentDay)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@TimetableManagementActivity, "Failed to save routine: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
