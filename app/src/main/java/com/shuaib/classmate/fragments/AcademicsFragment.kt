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
import com.shuaib.classmate.adapters.PeriodAdapter
import com.shuaib.classmate.databinding.FragmentAcademicsBinding
import com.shuaib.classmate.models.Period
import com.shuaib.classmate.repositories.AcademicCalendarRepository
import com.shuaib.classmate.repositories.TimetableRepository
import com.shuaib.classmate.utils.AppPreferences
import com.shuaib.classmate.utils.DateHelper
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

        binding.btnFullRoutineView.setOnClickListener {
            (activity as? MainActivity)?.openChildDestination(R.id.nav_academics, R.id.nav_timetable)
        }

        binding.swipeRefresh.setOnRefreshListener {
            loadPeriodsForDay(days[selectedDayIndex])
            viewLifecycleOwner.lifecycleScope.launch {
                kotlinx.coroutines.delay(800)
                _binding?.swipeRefresh?.isRefreshing = false
            }
        }
    }

    private fun setupUtilitiesGrid() {
        val mainActivity = activity as? MainActivity

        binding.cardCgpaCalc.setOnClickListener {
            val ctx = context ?: return@setOnClickListener
            startActivity(Intent(ctx, CgpaCalculatorActivity::class.java))
        }

        binding.cardQuestionBank.setOnClickListener {
            mainActivity?.openChildDestination(R.id.nav_academics, R.id.fragment_question_bank)
        }

        binding.cardPdfLibrary.setOnClickListener {
            mainActivity?.openChildDestination(R.id.nav_academics, R.id.nav_pdf)
        }

        binding.cardExamResults.setOnClickListener {
            val ctx = context ?: return@setOnClickListener
            startActivity(Intent(ctx, ResultViewerActivity::class.java))
        }

        binding.cardSeatPlan.setOnClickListener {
            val ctx = context ?: return@setOnClickListener
            startActivity(Intent(ctx, SeatPlanViewerActivity::class.java))
        }

        binding.cardAcademicCalendar.setOnClickListener {
            val ctx = context ?: return@setOnClickListener
            startActivity(Intent(ctx, AcademicCalendarActivity::class.java))
        }
    }

    private var calendarRepo = AcademicCalendarRepository()
    private var isExceptionToday = false
    private var periodAdapter: PeriodAdapter? = null

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
        val isToday = (index == getTodayIndex())
        periodAdapter?.setPausedByCalendarException(isExceptionToday && isToday)
        loadPeriodsForDay(days[index])
    }

    private fun loadPeriodsForDay(day: String) {
        val ctx = context ?: return
        val appContext = ctx.applicationContext
        val semester = AppPreferences(appContext).getUserSemester().ifBlank { "2-1" }

        calendarRepo.getExceptions(
            onSuccess = { exceptions ->
                if (_binding == null) return@getExceptions
                val today = LocalDate.now()
                isExceptionToday = exceptions.any { exc ->
                    if (!exc.isActive) return@any false
                    val start = try { LocalDate.parse(exc.startDate) } catch (_: Exception) { null }
                    val end = try { LocalDate.parse(exc.endDate) } catch (_: Exception) { null }
                    start != null && end != null && !today.isBefore(start) && !today.isAfter(end)
                }
                periodAdapter?.setPausedByCalendarException(isExceptionToday && (selectedDayIndex == getTodayIndex()))
            },
            onFailure = {}
        )

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val periods = TimetableRepository.getInstance(appContext).getPeriodsForDay(day)
                if (_binding == null) return@launch

                val filtered = periods.filter {
                    val b = if (it.batch == "14") "2-1" else it.batch
                    b.isEmpty() || b.equals("all", ignoreCase = true) || b == semester
                }.sortedBy { DateHelper.parseTimeToMinutes(it.startTime) }

                renderPeriods(filtered)
            } catch (e: Exception) {
                renderPeriods(emptyList())
            }
        }
    }

    private fun renderPeriods(periods: List<Period>) {
        if (_binding == null) return
        if (periods.isEmpty()) {
            binding.emptyStateRoutine.visibility = View.VISIBLE
            binding.rvPeriods.visibility = View.GONE
            return
        }

        binding.emptyStateRoutine.visibility = View.GONE
        binding.rvPeriods.visibility = View.VISIBLE

        val isToday = (selectedDayIndex == getTodayIndex())
        val isPast = (selectedDayIndex < getTodayIndex())
        val isPaused = isExceptionToday && isToday

        if (binding.rvPeriods.adapter == null) {
            val ctx = context ?: return
            binding.rvPeriods.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(ctx)
        }

        if (periodAdapter == null) {
            periodAdapter = PeriodAdapter(
                periods = periods,
                isPausedByCalendarException = isPaused,
                isViewingToday = isToday,
                isPastDay = isPast,
                onPeriodClick = { period ->
                    val bundle = androidx.core.os.bundleOf("subjectName" to period.subject)
                    (activity as? MainActivity)?.openChildDestination(
                        R.id.nav_pdf,
                        R.id.fragment_subject_pdf_list,
                        bundle
                    )
                }
            )
            binding.rvPeriods.adapter = periodAdapter
        } else {
            periodAdapter?.setPausedByCalendarException(isPaused)
            periodAdapter?.updateList(periods, isToday, isPast, resetAnimation = true)
        }
    }

    private fun getTodayIndex(): Int {
        return when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        periodAdapter = null
    }
}
