package com.shuaib.classmate.fragments

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.shuaib.classmate.R
import com.shuaib.classmate.activities.AcademicCalendarActivity
import com.shuaib.classmate.activities.CgpaCalculatorActivity
import com.shuaib.classmate.activities.MainActivity
import com.shuaib.classmate.activities.ResultViewerActivity
import com.shuaib.classmate.activities.SeatPlanViewerActivity
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.ListenerRegistration
import com.shuaib.classmate.adapters.PeriodAdapter
import com.shuaib.classmate.databinding.FragmentAcademicsBinding
import com.shuaib.classmate.models.AcademicCalendarException
import com.shuaib.classmate.models.Period
import com.shuaib.classmate.repositories.AcademicCalendarRepository
import com.shuaib.classmate.repositories.TimetableRepository
import com.shuaib.classmate.utils.AppPreferences
import com.shuaib.classmate.utils.DateHelper
import com.shuaib.classmate.utils.applyCardClickAnimation
import com.shuaib.classmate.utils.launchActivityWithCardAnimation
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Calendar

class AcademicsFragment : Fragment() {

    private var _binding: FragmentAcademicsBinding? = null
    private val binding get() = _binding!!

    private val days = listOf("saturday", "sunday", "monday", "tuesday", "wednesday", "thursday", "friday")
    private val dayShorts = listOf("SAT", "SUN", "MON", "TUE", "WED", "THU", "FRI")
    private var selectedDayIndex = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAcademicsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUtilitiesGrid()
        setupDayPills()

        selectedDayIndex = getTodayIndex()
        selectDay(selectedDayIndex)

        binding.calendarExceptionBanner.setOnClickListener {
            val ctx = context ?: return@setOnClickListener
            startActivity(Intent(ctx, AcademicCalendarActivity::class.java))
        }

        listenToCalendarExceptions()

        binding.btnFullRoutineView.setOnClickListener {
            (activity as? MainActivity)?.openChildDestination(R.id.nav_academics, R.id.nav_timetable)
        }

        binding.swipeRefresh.setOnRefreshListener {
            val day = days[selectedDayIndex]
            val repo = TimetableRepository.getInstance(requireContext().applicationContext)
            repo.enqueueNetworkSync()
            viewLifecycleOwner.lifecycleScope.launch {
                runCatching { repo.syncDayFromFirestore(day, com.google.firebase.firestore.Source.SERVER) }
                _binding?.swipeRefresh?.isRefreshing = false
            }
            loadPeriodsForDay(day)
        }
    }

    private fun setupUtilitiesGrid() {
        val mainActivity = activity as? MainActivity

        binding.cardCgpaCalc.setOnClickListener { view ->
            val ctx = context ?: return@setOnClickListener
            view.launchActivityWithCardAnimation(Intent(ctx, CgpaCalculatorActivity::class.java))
        }

        binding.cardQuestionBank.setOnClickListener { view ->
            view.applyCardClickAnimation {
                mainActivity?.openChildDestination(R.id.nav_academics, R.id.fragment_question_bank)
            }
        }

        binding.cardPdfLibrary.setOnClickListener { view ->
            view.applyCardClickAnimation {
                mainActivity?.openChildDestination(R.id.nav_academics, R.id.nav_pdf)
            }
        }

        binding.cardExamResults.setOnClickListener { view ->
            val ctx = context ?: return@setOnClickListener
            view.launchActivityWithCardAnimation(Intent(ctx, ResultViewerActivity::class.java))
        }

        binding.cardSeatPlan.setOnClickListener { view ->
            val ctx = context ?: return@setOnClickListener
            view.launchActivityWithCardAnimation(Intent(ctx, SeatPlanViewerActivity::class.java))
        }

        binding.cardAcademicCalendar.setOnClickListener { view ->
            val ctx = context ?: return@setOnClickListener
            view.launchActivityWithCardAnimation(Intent(ctx, AcademicCalendarActivity::class.java))
        }
    }

    private val calendarRepo = AcademicCalendarRepository()
    private var activeExceptionsList: List<AcademicCalendarException> = emptyList()
    private var calendarListener: ListenerRegistration? = null
    private var periodAdapter: PeriodAdapter? = null

    private fun listenToCalendarExceptions() {
        calendarListener?.remove()
        calendarListener = calendarRepo.getExceptions(
            onSuccess = { exceptions ->
                if (_binding == null) return@getExceptions
                activeExceptionsList = exceptions
                loadPeriodsForDay(days[selectedDayIndex])
            },
            onFailure = {}
        )
    }

    private fun getTargetDateForIndex(index: Int): LocalDate {
        val today = LocalDate.now()
        val todayIndex = getTodayIndex()
        return if (todayIndex == 5 || todayIndex == 6) {
            val diff = (index - todayIndex + 7) % 7
            today.plusDays(diff.toLong())
        } else {
            val saturday = today.minusDays(todayIndex.toLong())
            saturday.plusDays(index.toLong())
        }
    }

    private fun getFullDateForIndex(index: Int): String {
        return getTargetDateForIndex(index).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd", java.util.Locale.US))
    }

    private fun getDateForDayIndex(targetIndex: Int): LocalDate {
        return getTargetDateForIndex(targetIndex)
    }

    private fun getExceptionForSelectedDay(): AcademicCalendarException? {
        val targetDate = getDateForDayIndex(selectedDayIndex)
        val ctx = context ?: return null
        val semester = AppPreferences(ctx.applicationContext).getUserSemester().ifBlank { "2-1" }
        return activeExceptionsList
            .filter { it.isActive && it.type in AcademicCalendarException.SUPPORTED_TYPES }
            .filter { ex ->
                when (ex.scope) {
                    AcademicCalendarException.SCOPE_ALL_CLASSES -> true
                    AcademicCalendarException.SCOPE_SPECIFIC_BATCHES -> {
                        ex.targetBatches.isEmpty() || ex.targetBatches.contains(semester) || ex.targetBatches.contains("All")
                    }
                    else -> true
                }
            }
            .firstOrNull { ex ->
                val start = try { LocalDate.parse(ex.startDate) } catch (_: Exception) { null }
                val end = try { LocalDate.parse(ex.endDate) } catch (_: Exception) { start }
                start != null && end != null && !targetDate.isBefore(start) && !targetDate.isAfter(end)
            }
    }

    private fun setupDayPills() {
        val ctx = context ?: return
        binding.layoutDayPills.removeAllViews()
        val density = resources.displayMetrics.density
        val margin = (4 * density).toInt()

        dayShorts.forEachIndexed { index, shortName ->
            val pill = TextView(ctx).apply {
                text = shortName
                textSize = 13f
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                setPadding((16 * density).toInt(), (10 * density).toInt(), (16 * density).toInt(), (10 * density).toInt())
                val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(margin, 0, margin, 0)
                }
                layoutParams = lp
                setOnClickListener { selectDay(index) }
            }
            binding.layoutDayPills.addView(pill)
        }
    }

    private fun selectDay(index: Int) {
        val ctx = context ?: return
        selectedDayIndex = index
        for (i in 0 until binding.layoutDayPills.childCount) {
            val pill = binding.layoutDayPills.getChildAt(i) as TextView
            if (i == index) {
                pill.setBackgroundResource(R.drawable.bg_pill_day_selected)
                pill.setTextColor(Color.WHITE)
            } else {
                pill.setBackgroundResource(R.drawable.bg_pill_day_unselected)
                pill.setTextColor(ContextCompat.getColor(ctx, R.color.cm_text_primary))
            }
        }
        loadPeriodsForDay(days[index])
    }

    private var timetableRealtimeRegistration: ListenerRegistration? = null
    private var observePeriodsJob: kotlinx.coroutines.Job? = null

    private fun loadPeriodsForDay(day: String) {
        val ctx = context ?: return
        val appContext = ctx.applicationContext
        val semester = AppPreferences(appContext).getUserSemester().ifBlank { "2-1" }
        val userDeptId = AppPreferences(appContext).getUserDepartmentId()

        observePeriodsJob?.cancel()
        timetableRealtimeRegistration?.remove()

        timetableRealtimeRegistration = TimetableRepository.getInstance(appContext)
            .startRealtimeDaySync(day, viewLifecycleOwner.lifecycleScope)

        val targetDateString = getFullDateForIndex(selectedDayIndex)

        observePeriodsJob = viewLifecycleOwner.lifecycleScope.launch {
            try {
                if (activeExceptionsList.isEmpty()) {
                    try {
                        val activeExp = calendarRepo.getActiveExceptionForDate(LocalDate.now())
                        if (activeExp != null && activeExceptionsList.isEmpty()) {
                            activeExceptionsList = listOf(activeExp)
                        }
                    } catch (_: Exception) {}
                }

                val exception = getExceptionForSelectedDay()
                if (exception != null) {
                    renderPeriods(emptyList())
                    return@launch
                }

                TimetableRepository.getInstance(appContext)
                    .observePeriods(day, targetDateString)
                    .collect { rawPeriods ->
                        if (_binding == null) return@collect

                        val currentException = getExceptionForSelectedDay()
                        if (currentException != null) {
                            renderPeriods(emptyList())
                            return@collect
                        }

                        val filtered = rawPeriods
                            .filter { period ->
                                val deptMatches = com.shuaib.classmate.utils.GuestUserHelper.doesPeriodMatchUserDepartment(
                                    appContext,
                                    period.departmentId,
                                    period.department
                                )
                                if (!deptMatches) return@filter false

                                !period.isTemporary || period.temporaryDate.isBlank() || period.temporaryDate == targetDateString
                            }
                            .filter { period ->
                                val b = if (period.batch == "14") "2-1" else period.batch
                                b.isEmpty() || b.equals("all", ignoreCase = true) || b == semester
                            }
                            .map { period ->
                                val isCancelledForDay = period.cancelDate == targetDateString
                                val isSubForDay = period.substituteDate == targetDateString && period.substituteTeacher.isNotBlank()
                                val isOverriddenForDay = period.overrideDate == targetDateString
                                period.copy(
                                    isCancelled = isCancelledForDay,
                                    isSubstitute = isSubForDay,
                                    substituteTeacher = if (isSubForDay) period.substituteTeacher else "",
                                    isDynamicOverride = isOverriddenForDay,
                                    startTime = if (isOverriddenForDay && period.overrideStartTime.isNotBlank()) period.overrideStartTime else period.startTime,
                                    endTime = if (isOverriddenForDay && period.overrideEndTime.isNotBlank()) period.overrideEndTime else period.endTime,
                                    room = if (isOverriddenForDay && period.overrideRoom.isNotBlank()) period.overrideRoom else period.room
                                )
                            }
                            .sortedBy { DateHelper.parseTimeToMinutes(it.startTime) }

                        renderPeriods(filtered)
                    }
            } catch (e: Exception) {
                renderPeriods(emptyList())
            }
        }
    }

    private fun renderPeriods(periods: List<Period>) {
        if (_binding == null) return

        val exception = getExceptionForSelectedDay()
        if (exception != null) {
            // Holiday / Vacation active for this day - suppress classes completely and show prominent banner
            binding.rvPeriods.visibility = View.GONE
            binding.emptyStateRoutine.visibility = View.GONE
            binding.calendarExceptionBanner.visibility = View.VISIBLE

            binding.tvExceptionEmoji.text = when (exception.type) {
                AcademicCalendarException.TYPE_VACATION -> "🏖️"
                AcademicCalendarException.TYPE_HOLIDAY -> "🎉"
                AcademicCalendarException.TYPE_CLASS_SUSPENDED -> "🛑"
                else -> "🔔"
            }
            binding.tvExceptionBadge.text = when (exception.type) {
                AcademicCalendarException.TYPE_VACATION -> "VACATION ACTIVE"
                AcademicCalendarException.TYPE_HOLIDAY -> "UNIVERSITY HOLIDAY"
                AcademicCalendarException.TYPE_CLASS_SUSPENDED -> "CLASSES SUSPENDED"
                else -> "OFF TODAY"
            }
            binding.tvExceptionDateRange.text = if (exception.startDate.isNotBlank() && exception.endDate.isNotBlank()) {
                "${exception.startDate} to ${exception.endDate}"
            } else "All Day"
            binding.tvExceptionTitle.text = exception.title.ifBlank { "University Holiday" }
            binding.tvExceptionMessage.text = exception.reason.ifBlank { "All classes are suspended today. Enjoy your break!" }
            return
        }

        binding.calendarExceptionBanner.visibility = View.GONE

        if (periods.isEmpty()) {
            binding.emptyStateRoutine.visibility = View.VISIBLE
            binding.rvPeriods.visibility = View.GONE
            return
        }

        binding.emptyStateRoutine.visibility = View.GONE
        binding.rvPeriods.visibility = View.VISIBLE

        val isToday = (selectedDayIndex == getTodayIndex())
        val isPast = (selectedDayIndex < getTodayIndex())

        if (binding.rvPeriods.adapter == null) {
            val ctx = context ?: return
            binding.rvPeriods.layoutManager = LinearLayoutManager(ctx)
        }

        if (periodAdapter == null) {
            periodAdapter = PeriodAdapter(
                periods = periods,
                isPausedByCalendarException = false,
                isViewingToday = isToday,
                isPastDay = isPast,
                onPeriodClick = { period ->
                    val bundle = bundleOf("subjectName" to period.subject)
                    (activity as? MainActivity)?.openChildDestination(
                        R.id.nav_pdf,
                        R.id.fragment_subject_pdf_list,
                        bundle
                    )
                }
            )
            binding.rvPeriods.adapter = periodAdapter
        } else {
            periodAdapter?.setPausedByCalendarException(false)
            periodAdapter?.updateList(periods, isToday, isPast, resetAnimation = true)
        }
    }

    private fun getTodayIndex(): Int {
        return when (LocalDate.now().dayOfWeek) {
            java.time.DayOfWeek.SATURDAY  -> 0
            java.time.DayOfWeek.SUNDAY    -> 1
            java.time.DayOfWeek.MONDAY    -> 2
            java.time.DayOfWeek.TUESDAY   -> 3
            java.time.DayOfWeek.WEDNESDAY -> 4
            java.time.DayOfWeek.THURSDAY  -> 5
            java.time.DayOfWeek.FRIDAY    -> 6
            else                          -> 0
        }
    }

    override fun onDestroyView() {
        observePeriodsJob?.cancel()
        observePeriodsJob = null
        timetableRealtimeRegistration?.remove()
        timetableRealtimeRegistration = null
        calendarListener?.remove()
        calendarListener = null
        super.onDestroyView()
        _binding = null
        periodAdapter = null
    }
}
