package com.shuaib.classmate.activities

import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.TypedValue
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.shuaib.classmate.R
import com.shuaib.classmate.adapters.CalendarDay
import com.shuaib.classmate.adapters.CalendarEventAdapter
import com.shuaib.classmate.adapters.CalendarFeedItem
import com.shuaib.classmate.adapters.CalendarGridAdapter
import com.shuaib.classmate.databinding.ActivityAcademicCalendarBinding
import com.shuaib.classmate.databinding.DialogAcademicCalendarExceptionBinding
import com.shuaib.classmate.databinding.DialogCalendarAddChoiceBinding
import com.shuaib.classmate.models.AcademicCalendarException
import com.shuaib.classmate.models.CampusClub
import com.shuaib.classmate.models.CampusEvent
import com.shuaib.classmate.repositories.AcademicCalendarRepository
import com.shuaib.classmate.repositories.EventsClubsRepository
import com.shuaib.classmate.utils.ClassReminderWorkCoordinator
import com.shuaib.classmate.utils.NotificationSender
import com.shuaib.classmate.utils.applyComposerBounceAnimation
import com.shuaib.classmate.utils.applyFloatingPostAnimation
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Calendar
import java.util.Locale

class AcademicCalendarActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAcademicCalendarBinding
    private lateinit var gridAdapter: CalendarGridAdapter
    private lateinit var eventAdapter: CalendarEventAdapter

    private val calendarRepo = AcademicCalendarRepository()
    private val eventsRepo = EventsClubsRepository()

    private var calendarRegistration: ListenerRegistration? = null
    private var eventsRegistration: ListenerRegistration? = null

    private var currentYearMonth: YearMonth = YearMonth.now()
    private var selectedDate: LocalDate = LocalDate.now()
    private var showFullMonth: Boolean = false

    private var exceptionsList: List<AcademicCalendarException> = emptyList()
    private var eventsList: List<CampusEvent> = emptyList()

    private val currentUserId: String
        get() = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

    private var isSuperAdmin: Boolean = false
    private var currentUserRole: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAcademicCalendarBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViews()
        fetchUserRole()
        startRealtimeObservers()
    }

    private fun initViews() {
        binding.btnBack.setOnClickListener { finish() }

        // Grid Adapter (7 columns for Sunday to Saturday)
        gridAdapter = CalendarGridAdapter { clickedDate ->
            selectedDate = clickedDate
            if (clickedDate.month != currentYearMonth.month || clickedDate.year != currentYearMonth.year) {
                currentYearMonth = YearMonth.from(clickedDate)
            }
            binding.chipFilterScope.isChecked = false
            showFullMonth = false
            updateCalendarUi()
        }
        binding.rvCalendarGrid.layoutManager = GridLayoutManager(this, 7)
        binding.rvCalendarGrid.adapter = gridAdapter

        // Feed Adapter (Events & Holidays below the calendar)
        eventAdapter = CalendarEventAdapter(
            onHolidayClick = { showExceptionDialog(it) },
            onHolidayLongClick = { showHolidayActionsDialog(it) },
            onEventClick = { showEventDetailsDialog(it) },
            onEventLongClick = { showEventActionsDialog(it) }
        )
        binding.rvEventsList.layoutManager = LinearLayoutManager(this)
        binding.rvEventsList.adapter = eventAdapter

        // Month Navigation
        binding.btnPrevMonth.setOnClickListener {
            goToPrevMonth()
        }

        binding.btnNextMonth.setOnClickListener {
            goToNextMonth()
        }

        setupCalendarSwipeGestures()

        // Today Jump Button
        binding.btnToday.setOnClickListener {
            selectedDate = LocalDate.now()
            currentYearMonth = YearMonth.now()
            binding.chipFilterScope.isChecked = false
            showFullMonth = false
            updateCalendarUi()
        }

        // Refresh Button
        binding.btnRefresh.setOnClickListener {
            startRealtimeObservers()
            Toast.makeText(this, "Calendar synchronized in real-time", Toast.LENGTH_SHORT).show()
        }

        // Filter Chip: Selected Day vs All Month
        binding.chipFilterScope.setOnCheckedChangeListener { _, isChecked ->
            showFullMonth = isChecked
            updateFeedList()
        }

        // FAB to add holiday or campus event
        binding.fabAdd.setOnClickListener { view ->
            view.applyComposerBounceAnimation {
                showAddChoiceDialog()
            }
        }
    }

    private fun goToPrevMonth() {
        currentYearMonth = currentYearMonth.minusMonths(1)
        syncSelectedDateToMonth()
        animateMonthTransition(isNext = false)
        updateCalendarUi()
    }

    private fun goToNextMonth() {
        currentYearMonth = currentYearMonth.plusMonths(1)
        syncSelectedDateToMonth()
        animateMonthTransition(isNext = true)
        updateCalendarUi()
    }

    private fun syncSelectedDateToMonth() {
        selectedDate = if (currentYearMonth == YearMonth.now()) {
            LocalDate.now()
        } else {
            currentYearMonth.atDay(1)
        }
    }

    private fun animateMonthTransition(isNext: Boolean) {
        val slideOffset = if (isNext) 100f else -100f
        binding.rvCalendarGrid.translationX = slideOffset
        binding.rvCalendarGrid.alpha = 0.3f
        binding.rvCalendarGrid.animate()
            .translationX(0f)
            .alpha(1f)
            .setDuration(220)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()

        binding.tvMonthYear.alpha = 0.3f
        binding.tvMonthYear.animate()
            .alpha(1f)
            .setDuration(200)
            .start()
    }

    private fun setupCalendarSwipeGestures() {
        val thresholdPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            50f,
            resources.displayMetrics
        )
        val velocityThresholdPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            100f,
            resources.displayMetrics
        )

        var startX = 0f
        var startY = 0f
        var hasHandledSwipe = false

        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean {
                hasHandledSwipe = false
                return true
            }

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (hasHandledSwipe || e1 == null) return false
                val diffX = e2.x - e1.x
                val diffY = e2.y - e1.y
                if (kotlin.math.abs(diffX) > kotlin.math.abs(diffY) &&
                    kotlin.math.abs(diffX) > thresholdPx &&
                    kotlin.math.abs(velocityX) > velocityThresholdPx
                ) {
                    hasHandledSwipe = true
                    if (diffX < 0) {
                        goToNextMonth()
                    } else {
                        goToPrevMonth()
                    }
                    return true
                }
                return false
            }
        })

        val touchListener = View.OnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.x
                    startY = event.y
                    hasHandledSwipe = false
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = kotlin.math.abs(event.x - startX)
                    val dy = kotlin.math.abs(event.y - startY)
                    if (dx > dy && dx > 20) {
                        v.parent?.requestDisallowInterceptTouchEvent(true)
                    }
                }
                MotionEvent.ACTION_UP -> {
                    if (!hasHandledSwipe) {
                        val diffX = event.x - startX
                        val diffY = kotlin.math.abs(event.y - startY)
                        if (kotlin.math.abs(diffX) > diffY && kotlin.math.abs(diffX) > thresholdPx) {
                            hasHandledSwipe = true
                            if (diffX < 0) {
                                goToNextMonth()
                            } else {
                                goToPrevMonth()
                            }
                        }
                    }
                }
            }
            gestureDetector.onTouchEvent(event)
            false
        }

        binding.layoutCalendarMonthCard.setOnTouchListener(touchListener)

        binding.rvCalendarGrid.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                when (e.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = e.x
                        startY = e.y
                        hasHandledSwipe = false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = kotlin.math.abs(e.x - startX)
                        val dy = kotlin.math.abs(e.y - startY)
                        if (dx > dy && dx > 20) {
                            rv.parent?.requestDisallowInterceptTouchEvent(true)
                            if (!hasHandledSwipe && dx > thresholdPx) {
                                hasHandledSwipe = true
                                if (e.x - startX < 0) {
                                    goToNextMonth()
                                } else {
                                    goToPrevMonth()
                                }
                                return true
                            }
                        }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        if (!hasHandledSwipe) {
                            val diffX = e.x - startX
                            val diffY = kotlin.math.abs(e.y - startY)
                            if (kotlin.math.abs(diffX) > diffY && kotlin.math.abs(diffX) > thresholdPx) {
                                hasHandledSwipe = true
                                if (diffX < 0) {
                                    goToNextMonth()
                                } else {
                                    goToPrevMonth()
                                }
                                return true
                            }
                        }
                    }
                }
                gestureDetector.onTouchEvent(e)
                return false
            }

            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
                gestureDetector.onTouchEvent(e)
            }
        })
    }

    private fun fetchUserRole() {
        if (currentUserId.isBlank()) return
        FirebaseFirestore.getInstance().collection("users").document(currentUserId).get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    currentUserRole = doc.getString("role").orEmpty()
                    isSuperAdmin = currentUserRole.equals("superadmin", ignoreCase = true)
                }
            }
    }

    /**
     * Real-time listeners for both academic calendar exceptions and campus events.
     * When any user or admin creates/modifies an event/holiday, it updates immediately!
     */
    private fun startRealtimeObservers() {
        calendarRegistration?.remove()
        eventsRegistration?.remove()

        // 1. Observe Academic Calendar Exceptions (Holidays, Vacations, Class Suspended)
        calendarRegistration = calendarRepo.observeAllExceptions(
            onResult = { exceptions ->
                exceptionsList = exceptions
                updateCalendarUi()
            },
            onError = {
                Toast.makeText(this, "Failed to load calendar records", Toast.LENGTH_SHORT).show()
            }
        )

        // 2. Observe Campus Events in Real-Time
        eventsRegistration = eventsRepo.observeEvents(
            onResult = { events ->
                eventsList = events
                updateCalendarUi()
            },
            onError = {
                // Non-blocking
            }
        )
    }

    /**
     * Rebuilds the month grid and the day's event feed.
     */
    private fun updateCalendarUi() {
        // Month Title Header (e.g. "October 2026")
        val monthName = currentYearMonth.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
        binding.tvMonthYear.text = "$monthName ${currentYearMonth.year}"

        // Calculate days for the 7-column grid
        val today = LocalDate.now()
        val firstDayOfMonth = currentYearMonth.atDay(1)
        val daysInMonth = currentYearMonth.lengthOfMonth()

        // Sunday is 0, Monday is 1, ..., Saturday is 6
        val sundayBasedOffset = if (firstDayOfMonth.dayOfWeek == DayOfWeek.SUNDAY) 0 else firstDayOfMonth.dayOfWeek.value
        val totalCellsNeeded = sundayBasedOffset + daysInMonth
        val totalGridCells = if (totalCellsNeeded <= 35) 35 else 42

        val gridStartDate = firstDayOfMonth.minusDays(sundayBasedOffset.toLong())
        val calendarDays = mutableListOf<CalendarDay>()

        for (i in 0 until totalGridCells) {
            val cellDate = gridStartDate.plusDays(i.toLong())
            val isCurrentMonth = cellDate.month == currentYearMonth.month && cellDate.year == currentYearMonth.year
            val isToday = cellDate == today
            val isSelected = cellDate == selectedDate
            val isWeekend = cellDate.dayOfWeek == DayOfWeek.FRIDAY || cellDate.dayOfWeek == DayOfWeek.SATURDAY

            val matchingHoliday = exceptionsList.firstOrNull { ex ->
                ex.isActive && isDateInException(cellDate, ex)
            }
            val matchingEvents = eventsList.filter { ev ->
                isDateInEvent(cellDate, ev)
            }

            calendarDays.add(
                CalendarDay(
                    date = cellDate,
                    isCurrentMonth = isCurrentMonth,
                    isToday = isToday,
                    isSelected = isSelected,
                    isWeekend = isWeekend,
                    holiday = matchingHoliday,
                    events = matchingEvents
                )
            )
        }

        gridAdapter.submitDays(calendarDays)
        updateFeedList()
    }

    /**
     * Updates the feed list below the calendar grid.
     */
    private fun updateFeedList() {
        val feedItems = mutableListOf<CalendarFeedItem>()

        if (showFullMonth) {
            // Show all items for the month
            val monthName = currentYearMonth.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
            binding.tvSelectedDateTitle.text = "$monthName ${currentYearMonth.year} Schedule"

            // 1. Month holidays
            val monthHolidays = exceptionsList.filter { ex ->
                ex.isActive && exceptionCoversMonth(ex, currentYearMonth)
            }.sortedBy { it.startDate }

            for (ex in monthHolidays) {
                feedItems.add(CalendarFeedItem.HolidayItem(ex))
            }

            // 2. Month campus events
            val monthEvents = eventsList.filter { ev ->
                eventCoversMonth(ev, currentYearMonth)
            }.sortedBy { it.eventDateMillis }

            for (ev in monthEvents) {
                feedItems.add(CalendarFeedItem.EventItem(ev))
            }
        } else {
            // Show items for selectedDate
            val formatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH)
            val dateTitle = if (selectedDate == LocalDate.now()) {
                "Today • ${selectedDate.format(formatter)}"
            } else {
                selectedDate.format(formatter)
            }
            binding.tvSelectedDateTitle.text = dateTitle

            // 1. Holidays covering selectedDate
            val dateHolidays = exceptionsList.filter { ex ->
                ex.isActive && isDateInException(selectedDate, ex)
            }
            for (ex in dateHolidays) {
                feedItems.add(CalendarFeedItem.HolidayItem(ex))
            }

            // 2. Campus events on selectedDate
            val dateEvents = eventsList.filter { ev ->
                isDateInEvent(selectedDate, ev)
            }
            for (ev in dateEvents) {
                feedItems.add(CalendarFeedItem.EventItem(ev))
            }

            // 3. If selectedDate is Friday or Saturday and has no holiday, display Weekend card
            val isWeekend = selectedDate.dayOfWeek == DayOfWeek.FRIDAY || selectedDate.dayOfWeek == DayOfWeek.SATURDAY
            if (isWeekend && dateHolidays.isEmpty()) {
                feedItems.add(CalendarFeedItem.WeekendItem(selectedDate))
            }
        }

        eventAdapter.submitItems(feedItems)

        val isEmpty = feedItems.isEmpty()
        binding.llEmptyState.isVisible = isEmpty
        binding.rvEventsList.isVisible = !isEmpty

        if (isEmpty) {
            binding.tvEmptyTitle.text = if (showFullMonth) "No events scheduled this month" else "No events scheduled for this day"
            binding.tvEmptySubtitle.text = "Regular academic classes scheduled as usual"
        }
    }

    private fun isDateInException(date: LocalDate, exception: AcademicCalendarException): Boolean {
        val start = try { LocalDate.parse(exception.startDate) } catch (_: Exception) { return false }
        val end = try { LocalDate.parse(exception.endDate) } catch (_: Exception) { return false }
        return !date.isBefore(start) && !date.isAfter(end)
    }

    private fun exceptionCoversMonth(exception: AcademicCalendarException, ym: YearMonth): Boolean {
        val start = try { LocalDate.parse(exception.startDate) } catch (_: Exception) { return false }
        val end = try { LocalDate.parse(exception.endDate) } catch (_: Exception) { return false }
        val monthStart = ym.atDay(1)
        val monthEnd = ym.atEndOfMonth()
        return !end.isBefore(monthStart) && !start.isAfter(monthEnd)
    }

    private fun isDateInEvent(targetDate: LocalDate, event: CampusEvent): Boolean {
        // 1. Try ISO date "yyyy-MM-dd"
        if (event.date.isNotBlank()) {
            try {
                val parsed = LocalDate.parse(event.date.trim())
                if (parsed == targetDate) return true
            } catch (_: Exception) {}
        }

        // 2. Try eventDateMillis
        if (event.eventDateMillis > 0) {
            try {
                val eventLocal = Instant.ofEpochMilli(event.eventDateMillis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                if (eventLocal == targetDate) return true
            } catch (_: Exception) {}
        }

        // 3. Try date strings from dateTime or date
        val datePart = event.dateTime.split("•").firstOrNull()?.trim() ?: event.date
        if (datePart.isNotBlank()) {
            val formatters = listOf(
                DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US),
                DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US),
                DateTimeFormatter.ofPattern("MMMM dd, yyyy", Locale.US),
                DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.US),
                DateTimeFormatter.ofPattern("d MMM yyyy", Locale.US)
            )
            for (fmt in formatters) {
                try {
                    val parsed = LocalDate.parse(datePart, fmt)
                    if (parsed == targetDate) return true
                } catch (_: Exception) {}
            }
        }

        return false
    }

    private fun eventCoversMonth(event: CampusEvent, ym: YearMonth): Boolean {
        if (event.eventDateMillis > 0) {
            try {
                val eventLocal = Instant.ofEpochMilli(event.eventDateMillis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                return eventLocal.year == ym.year && eventLocal.month == ym.month
            } catch (_: Exception) {}
        }
        val datePart = event.dateTime.split("•").firstOrNull()?.trim() ?: event.date
        if (datePart.isNotBlank()) {
            val formatters = listOf(
                DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US),
                DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US),
                DateTimeFormatter.ofPattern("MMMM dd, yyyy", Locale.US),
                DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.US)
            )
            for (fmt in formatters) {
                try {
                    val parsed = LocalDate.parse(datePart, fmt)
                    return parsed.year == ym.year && parsed.month == ym.month
                } catch (_: Exception) {}
            }
        }
        return false
    }

    // ==========================================
    // ADD CHOICE DIALOG (Holiday vs Campus Event)
    // ==========================================

    private fun showAddChoiceDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val choiceBinding = DialogCalendarAddChoiceBinding.inflate(LayoutInflater.from(this))
        dialog.setContentView(choiceBinding.root)

        choiceBinding.btnCloseDialog.setOnClickListener { dialog.dismiss() }
        choiceBinding.btnCancel.setOnClickListener { dialog.dismiss() }

        choiceBinding.cardAddHoliday.setOnClickListener {
            dialog.dismiss()
            showExceptionDialog(null)
        }

        choiceBinding.cardAddCampusEvent.setOnClickListener {
            dialog.dismiss()
            showAddCampusEventDialog()
        }

        dialog.show()
        dialog.applyFloatingPostAnimation(this, 0.92, 0.45, isWrapHeight = true)
    }

    // ==========================================
    // HOLIDAY / EXCEPTION MANAGEMENT FLOW
    // ==========================================

    private fun showExceptionDialog(existing: AcademicCalendarException?) {
        val dialogBinding = DialogAcademicCalendarExceptionBinding.inflate(LayoutInflater.from(this))
        val typeLabels = listOf("Vacation", "Holiday", "Classes Suspended")
        dialogBinding.dropdownType.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, typeLabels)
        )

        val typeLabel = when (existing?.type) {
            AcademicCalendarException.TYPE_HOLIDAY -> "Holiday"
            AcademicCalendarException.TYPE_CLASS_SUSPENDED -> "Classes Suspended"
            else -> "Vacation"
        }
        dialogBinding.dropdownType.setText(typeLabel, false)
        dialogBinding.etTitle.setText(existing?.title ?: "")
        dialogBinding.etStartDate.setText(existing?.startDate ?: selectedDate.toString())
        dialogBinding.etEndDate.setText(existing?.endDate ?: selectedDate.toString())
        dialogBinding.etReason.setText(existing?.reason ?: "")
        dialogBinding.switchHolidayBriefing.isChecked = existing?.showHolidayBriefing ?: false
        dialogBinding.switchActive.isChecked = existing?.isActive ?: true

        // Semester selection
        val allBatches = arrayOf("1-1", "1-2", "2-1", "2-2", "3-1", "3-2", "4-1", "4-2")
        val selectedBatches = existing?.targetBatches?.toMutableList() ?: mutableListOf()
        val checkedItems = BooleanArray(allBatches.size) { i -> selectedBatches.contains(allBatches[i]) }

        fun updateBatchBtnText() {
            if (selectedBatches.isEmpty()) {
                dialogBinding.btnSelectBatches.text = "Target: All Semesters (Click to change)"
            } else {
                dialogBinding.btnSelectBatches.text = "Target: ${selectedBatches.joinToString(", ")}"
            }
        }
        updateBatchBtnText()

        dialogBinding.btnSelectBatches.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Select Target Semesters")
                .setMultiChoiceItems(allBatches, checkedItems) { _, which, isChecked ->
                    val batch = allBatches[which]
                    if (isChecked) {
                        if (!selectedBatches.contains(batch)) selectedBatches.add(batch)
                    } else {
                        selectedBatches.remove(batch)
                    }
                }
                .setPositiveButton("OK") { _, _ -> updateBatchBtnText() }
                .setNeutralButton("Clear All") { _, _ ->
                    selectedBatches.clear()
                    checkedItems.fill(false)
                    updateBatchBtnText()
                }
                .show()
        }

        dialogBinding.etStartDate.setOnClickListener {
            showDatePicker(dialogBinding.etStartDate.text?.toString().orEmpty()) { date ->
                dialogBinding.etStartDate.setText(date)
            }
        }
        dialogBinding.etEndDate.setOnClickListener {
            showDatePicker(dialogBinding.etEndDate.text?.toString().orEmpty()) { date ->
                dialogBinding.etEndDate.setText(date)
            }
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(if (existing == null) "Add Class Off Day" else "Edit Class Off Day")
            .setView(dialogBinding.root)
            .setPositiveButton(if (existing == null) "Save" else "Update", null)
            .setNegativeButton("Cancel", null)
            .create()
            .apply {
                setOnShowListener {
                    getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        saveFromDialog(existing, dialogBinding, this, selectedBatches)
                    }
                }
                show()
                applyFloatingPostAnimation(this@AcademicCalendarActivity, 0.94, 0.75, isWrapHeight = true)
            }
    }

    private fun saveFromDialog(
        existing: AcademicCalendarException?,
        dialogBinding: DialogAcademicCalendarExceptionBinding,
        dialog: AlertDialog,
        selectedBatches: List<String>
    ) {
        val type = when (dialogBinding.dropdownType.text.toString()) {
            "Holiday" -> AcademicCalendarException.TYPE_HOLIDAY
            "Classes Suspended" -> AcademicCalendarException.TYPE_CLASS_SUSPENDED
            else -> AcademicCalendarException.TYPE_VACATION
        }
        val scope = if (selectedBatches.isEmpty()) AcademicCalendarException.SCOPE_ALL_CLASSES else AcademicCalendarException.SCOPE_SPECIFIC_BATCHES
        val exception = AcademicCalendarException(
            id = existing?.id.orEmpty(),
            title = dialogBinding.etTitle.text?.toString()?.trim().orEmpty(),
            type = type,
            startDate = dialogBinding.etStartDate.text?.toString()?.trim().orEmpty(),
            endDate = dialogBinding.etEndDate.text?.toString()?.trim().orEmpty(),
            scope = scope,
            targetBatches = selectedBatches,
            reason = dialogBinding.etReason.text?.toString()?.trim().orEmpty(),
            isActive = dialogBinding.switchActive.isChecked,
            showHolidayBriefing = dialogBinding.switchHolidayBriefing.isChecked,
            createdBy = existing?.createdBy?.ifBlank { currentUserId } ?: currentUserId,
            createdAt = existing?.createdAt
        )

        lifecycleScope.launch {
            val result = calendarRepo.saveException(exception)
            if (result.isSuccess) {
                if (exception.isActive && coversToday(exception)) {
                    ClassReminderWorkCoordinator.cancelTodayClassReminders(this@AcademicCalendarActivity)
                } else {
                    ClassReminderWorkCoordinator.refreshTodayClassReminders(this@AcademicCalendarActivity)
                }

                if (exception.isActive && exception.showHolidayBriefing && existing == null) {
                    val dateRange = if (exception.startDate == exception.endDate) exception.startDate else "${exception.startDate} to ${exception.endDate}"
                    val title = exception.title.ifBlank {
                        if (exception.type == AcademicCalendarException.TYPE_HOLIDAY) "University Holiday"
                        else if (exception.type == AcademicCalendarException.TYPE_CLASS_SUSPENDED) "Classes Suspended"
                        else "Vacation"
                    }
                    NotificationSender.sendHolidayAlert(
                        title = title,
                        reason = exception.reason,
                        dateRange = dateRange
                    )
                }

                Toast.makeText(this@AcademicCalendarActivity, "Calendar exception saved", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            } else {
                Toast.makeText(
                    this@AcademicCalendarActivity,
                    result.exceptionOrNull()?.message ?: "Save failed",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showHolidayActionsDialog(exception: AcademicCalendarException) {
        val activeLabel = if (exception.isActive) "Deactivate" else "Reactivate"
        MaterialAlertDialogBuilder(this)
            .setTitle(exception.title)
            .setItems(arrayOf("Edit", activeLabel, "Delete")) { _, which ->
                when (which) {
                    0 -> showExceptionDialog(exception)
                    1 -> setActive(exception, !exception.isActive)
                    2 -> deleteException(exception)
                }
            }
            .show()
    }

    private fun setActive(exception: AcademicCalendarException, active: Boolean) {
        lifecycleScope.launch {
            val result = calendarRepo.setActive(exception.id, active)
            if (result.isSuccess) {
                if (active && coversToday(exception.copy(isActive = true))) {
                    ClassReminderWorkCoordinator.cancelTodayClassReminders(this@AcademicCalendarActivity)
                } else {
                    ClassReminderWorkCoordinator.refreshTodayClassReminders(this@AcademicCalendarActivity)
                }
            } else {
                Toast.makeText(this@AcademicCalendarActivity, "Update failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteException(exception: AcademicCalendarException) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete record?")
            .setMessage("This removes '${exception.title}' from the academic calendar.")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    val result = calendarRepo.deleteException(exception.id)
                    if (result.isSuccess) {
                        ClassReminderWorkCoordinator.refreshTodayClassReminders(this@AcademicCalendarActivity)
                    } else {
                        Toast.makeText(this@AcademicCalendarActivity, "Delete failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ==========================================
    // CAMPUS EVENT CREATION & ACTIONS
    // ==========================================

    private fun showAddCampusEventDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_create_campus_event, null)
        dialog.setContentView(view)
        dialog.applyFloatingPostAnimation(this, 0.94, 0.82)

        val etTitle = view.findViewById<EditText>(R.id.etNewEventTitle)
        val etCategory = view.findViewById<EditText>(R.id.etNewEventCategory)
        val btnPickDate = view.findViewById<View>(R.id.btnPickDate)
        val tvSelectedDate = view.findViewById<TextView>(R.id.tvSelectedDate)
        val btnPickTime = view.findViewById<View>(R.id.btnPickTime)
        val tvSelectedTime = view.findViewById<TextView>(R.id.tvSelectedTime)
        val etLocation = view.findViewById<EditText>(R.id.etNewEventLocation)
        val etDesc = view.findViewById<EditText>(R.id.etNewEventDescription)
        val btnPublish = view.findViewById<MaterialButton>(R.id.btnPublishEvent)
        val btnClose = view.findViewById<ImageButton>(R.id.btnCloseCreateEvent)

        btnClose.setOnClickListener { dialog.dismiss() }

        // Pre-fill date with currently selected calendar date
        var selectedDateStr = selectedDate.toString()
        val sdfDisplay = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        val cal = Calendar.getInstance().apply {
            set(selectedDate.year, selectedDate.monthValue - 1, selectedDate.dayOfMonth)
        }
        var selectedDateDisplay = sdfDisplay.format(cal.time)
        var selectedDateMillis = cal.timeInMillis
        var selectedTimeStr = "10:00 AM"

        tvSelectedDate.text = selectedDateDisplay
        tvSelectedTime.text = selectedTimeStr

        btnPickDate.setOnClickListener {
            val dateCal = Calendar.getInstance().apply {
                if (selectedDateMillis > 0) timeInMillis = selectedDateMillis
            }
            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                val c = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
                val sdfValue = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                selectedDateStr = sdfValue.format(c.time)
                selectedDateDisplay = sdfDisplay.format(c.time)
                selectedDateMillis = c.timeInMillis
                tvSelectedDate.text = selectedDateDisplay
            }, dateCal.get(Calendar.YEAR), dateCal.get(Calendar.MONTH), dateCal.get(Calendar.DAY_OF_MONTH)).show()
        }

        btnPickTime.setOnClickListener {
            TimePickerDialog(this, { _, hourOfDay, minute ->
                val isPm = hourOfDay >= 12
                val h = if (hourOfDay % 12 == 0) 12 else hourOfDay % 12
                val m = String.format(Locale.US, "%02d", minute)
                val amPm = if (isPm) "PM" else "AM"
                selectedTimeStr = String.format(Locale.US, "%02d:%s %s", h, m, amPm)
                tvSelectedTime.text = selectedTimeStr
            }, 10, 0, false).show()
        }

        btnPublish.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val category = etCategory.text.toString().trim().ifBlank { "Campus" }
            val location = etLocation.text.toString().trim()
            val desc = etDesc.text.toString().trim()

            if (title.isBlank()) {
                etTitle.error = "Event title is required"
                return@setOnClickListener
            }

            btnPublish.isEnabled = false
            btnPublish.text = "Publishing..."

            val newEvent = CampusEvent(
                id = "",
                clubId = "campus_general",
                title = title,
                organizer = "Campus Community",
                date = selectedDateStr,
                time = selectedTimeStr,
                dateTime = "$selectedDateDisplay • $selectedTimeStr",
                eventDateMillis = selectedDateMillis,
                location = location.ifBlank { "GSTU Campus" },
                category = category,
                status = "REGISTRATION OPEN",
                isRegistrationOpen = true,
                description = desc,
                creatorId = currentUserId,
                creatorRole = currentUserRole,
                createdAt = System.currentTimeMillis()
            )

            eventsRepo.addEvent(
                event = newEvent,
                onSuccess = {
                    dialog.dismiss()
                    Toast.makeText(this, "Campus event published in real-time! 🎉", Toast.LENGTH_SHORT).show()
                },
                onFailure = { e ->
                    btnPublish.isEnabled = true
                    btnPublish.text = "Publish Event"
                    Toast.makeText(this, "Failed to publish: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }

        dialog.show()
    }

    private fun showEventDetailsDialog(event: CampusEvent) {
        MaterialAlertDialogBuilder(this)
            .setTitle(event.title)
            .setMessage("${event.description}\n\n📍 Location: ${event.location}\n🕒 Time: ${event.dateTime}\n🏛 Organizer: ${event.organizer}\n\nStatus: ${event.status}")
            .setPositiveButton("Close", null)
            .show()
    }

    private fun showEventActionsDialog(event: CampusEvent) {
        val options = mutableListOf("View Details")
        if (isSuperAdmin || event.creatorId == currentUserId) {
            options.add("Delete Event")
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(event.title)
            .setItems(options.toTypedArray()) { _, which ->
                when (options[which]) {
                    "View Details" -> showEventDetailsDialog(event)
                    "Delete Event" -> confirmDeleteEvent(event)
                }
            }
            .show()
    }

    private fun confirmDeleteEvent(event: CampusEvent) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Event?")
            .setMessage("Are you sure you want to delete '${event.title}'? This action will remove it from the calendar.")
            .setPositiveButton("Delete") { _, _ ->
                eventsRepo.deleteEvent(
                    eventId = event.id,
                    onSuccess = {
                        Toast.makeText(this, "Event deleted successfully", Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { e ->
                        Toast.makeText(this, "Error deleting event: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDatePicker(initial: String, onDate: (String) -> Unit) {
        val date = try {
            LocalDate.parse(initial)
        } catch (_: Exception) {
            LocalDate.now()
        }
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                onDate(LocalDate.of(year, month + 1, dayOfMonth).toString())
            },
            date.year,
            date.monthValue - 1,
            date.dayOfMonth
        ).show()
    }

    private fun coversToday(exception: AcademicCalendarException): Boolean {
        val today = LocalDate.now()
        val start = try { LocalDate.parse(exception.startDate) } catch (_: Exception) { return false }
        val end = try { LocalDate.parse(exception.endDate) } catch (_: Exception) { return false }
        return !today.isBefore(start) && !today.isAfter(end)
    }

    override fun onDestroy() {
        calendarRegistration?.remove()
        eventsRegistration?.remove()
        super.onDestroy()
    }
}
