package com.shuaib.classmate.fragments

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.view.MotionEvent
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.shuaib.classmate.R
import com.shuaib.classmate.utils.applyCardClickAnimation
import com.shuaib.classmate.utils.applyFloatingPostAnimation
import com.shuaib.classmate.utils.launchActivityWithCardAnimation
import com.shuaib.classmate.activities.AcademicCalendarActivity
import com.shuaib.classmate.activities.BloodDonorDirectoryActivity
import com.shuaib.classmate.activities.CampusFeedActivity
import com.shuaib.classmate.activities.CampusFeedFeaturesActivity
import com.shuaib.classmate.adapters.CampusFeedAdapter
import com.shuaib.classmate.models.CampusFeedPost
import com.shuaib.classmate.repository.CampusFeedRepository
import com.shuaib.classmate.activities.CgpaCalculatorActivity
import com.shuaib.classmate.activities.CoverPageActivity
import com.shuaib.classmate.activities.CvMakerActivity
import com.shuaib.classmate.activities.EventsClubsActivity
import com.shuaib.classmate.activities.LostFoundActivity
import com.shuaib.classmate.activities.MainActivity
import com.shuaib.classmate.adapters.DeadlineItem
import com.shuaib.classmate.adapters.DeadlinesBottomSheetAdapter
import com.shuaib.classmate.adapters.HeroSlideAdapter
import com.shuaib.classmate.adapters.TopAlertAdapter
import com.shuaib.classmate.databinding.DialogBottomSheetDeadlinesBinding
import com.shuaib.classmate.databinding.FragmentHomeBinding
import com.shuaib.classmate.models.AcademicCalendarException
import com.shuaib.classmate.models.Assignment
import com.shuaib.classmate.models.BloodRequest
import com.shuaib.classmate.models.HeroSlide
import com.shuaib.classmate.models.Notice
import com.shuaib.classmate.models.Period
import com.shuaib.classmate.repositories.BloodRequestRepository
import com.shuaib.classmate.models.TopAlertItem
import com.shuaib.classmate.models.TopAlertType
import com.shuaib.classmate.repositories.AcademicCalendarRepository
import com.shuaib.classmate.repositories.AppNotificationRepository
import com.shuaib.classmate.repositories.LostFoundRepository
import com.shuaib.classmate.repositories.NoticeRepository
import com.shuaib.classmate.repositories.TimetableRepository
import com.shuaib.classmate.ui.NotificationsBottomSheet
import com.shuaib.classmate.utils.AppPreferences
import com.shuaib.classmate.utils.CountdownManager
import com.shuaib.classmate.utils.DateHelper
import com.shuaib.classmate.utils.UserRoleHelper
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val calendarRepo = AcademicCalendarRepository()
    private val lostFoundRepo = LostFoundRepository()
    private val bloodRequestRepo = BloodRequestRepository()
    private val appNotificationRepo = AppNotificationRepository()
    private var notificationListener: com.google.firebase.firestore.ListenerRegistration? = null

    // CampusFeed Beta role check & live adapter
    private var isAdminUser = false
    private var homeCampusFeedAdapter: CampusFeedAdapter? = null
    private var homeFeedListener: com.google.firebase.firestore.ListenerRegistration? = null

    private var todayPeriods = listOf<Period>()
    private var activeExceptions = listOf<AcademicCalendarException>()
    private var calendarExceptionsListener: ListenerRegistration? = null
    private var userAssignments = listOf<Assignment>()
    private var cachedNotices = listOf<Notice>()
    private var allDeadlineItems = listOf<DeadlineItem>()

    // Top dynamic alert banner carousel (Pic 2, 3, 4, 5)
    private val topAlerts = mutableListOf<TopAlertItem>()
    private val dismissedAlertIds = mutableSetOf<String>()
    private var topAlertAdapter: TopAlertAdapter? = null
    private var isTopAlertManuallyPaused = false
    private val topAlertHandler = Handler(Looper.getMainLooper())
    private val topAlertRunnable = object : Runnable {
        override fun run() {
            if (_binding != null && !isTopAlertManuallyPaused && topAlerts.size > 1) {
                val next = binding.vpTopAlerts.currentItem + 1
                binding.vpTopAlerts.setCurrentItem(next, true)
                topAlertHandler.postDelayed(this, 5000)
            }
        }
    }

    // Hero live class routine carousel (Pic 1)
    private var heroSlides = listOf<HeroSlide>()
    private var heroSlideAdapter: HeroSlideAdapter? = null
    private var isHeroManuallyPaused = false
    private val heroScrollHandler = Handler(Looper.getMainLooper())
    private val heroScrollRunnable = object : Runnable {
        override fun run() {
            if (_binding != null && !isHeroManuallyPaused && heroSlides.size > 1) {
                val next = binding.vpHeroLive.currentItem + 1
                binding.vpHeroLive.setCurrentItem(next, true)
                heroScrollHandler.postDelayed(this, 5000)
            }
        }
    }

    private var countdownListener: com.google.firebase.firestore.ListenerRegistration? = null

    private val deadlineFlipperHandler = Handler(Looper.getMainLooper())
    private val deadlineFlipperRunnable = object : Runnable {
        override fun run() {
            val ctx = context ?: return
            if (_binding != null && binding.flipperNextEvent.childCount > 1) {
                binding.flipperNextEvent.inAnimation = AnimationUtils.loadAnimation(ctx, R.anim.slide_down_in)
                binding.flipperNextEvent.outAnimation = AnimationUtils.loadAnimation(ctx, R.anim.slide_up_out)
                binding.flipperNextEvent.showNext()
                deadlineFlipperHandler.postDelayed(this, 5000)
            }
        }
    }

    private val holidayFlipperHandler = Handler(Looper.getMainLooper())
    private val holidayFlipperRunnable = object : Runnable {
        override fun run() {
            val ctx = context ?: return
            if (_binding != null && binding.flipperHoliday.childCount > 1) {
                binding.flipperHoliday.inAnimation = AnimationUtils.loadAnimation(ctx, R.anim.slide_down_in)
                binding.flipperHoliday.outAnimation = AnimationUtils.loadAnimation(ctx, R.anim.slide_up_out)
                binding.flipperHoliday.showNext()
                holidayFlipperHandler.postDelayed(this, 5500)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupHeader()
        setupClickListeners()
        setupTopAlerts()
        setupHeroCarousel()
        setupNotificationBadgeListener()
        loadData()

        binding.swipeRefresh.setOnRefreshListener {
            loadData()
            setupNotificationBadgeListener()
            viewLifecycleOwner.lifecycleScope.launch {
                kotlinx.coroutines.delay(1000)
                _binding?.swipeRefresh?.isRefreshing = false
            }
        }
    }

    private fun setupNotificationBadgeListener() {
        val ctx = context ?: return
        val uid = auth.currentUser?.uid ?: ""
        val role = UserRoleHelper.getEffectiveRole(ctx)
        val isAdmin = role.equals("admin", true) || role.equals("superadmin", true)

        notificationListener?.remove()
        notificationListener = appNotificationRepo.listenNotifications(uid, isAdmin) { notifs ->
            if (_binding == null) return@listenNotifications
            val unreadCount = notifs.count { !it.isRead && !it.readBy.contains(uid) }
            if (unreadCount > 0) {
                binding.tvNotificationBadge.visibility = View.VISIBLE
                binding.tvNotificationBadge.text = if (unreadCount > 9) "9+" else unreadCount.toString()
            } else {
                binding.tvNotificationBadge.visibility = View.GONE
            }
        }
    }

    private fun setupHeader() {
        val user = auth.currentUser
        val name = user?.displayName?.split(" ")?.firstOrNull() ?: "Student"
        binding.tvUserGreeting.text = "Welcome, $name"

        val uid = user?.uid
        val ctx = context
        if (ctx != null) {
            val cachedRole = UserRoleHelper.getEffectiveRole(ctx)
            val r = cachedRole.trim().lowercase()
            isAdminUser = (r == "superadmin" || r == "admin" || r == "university_admin")
            updateCampusFeedVisibility(isAdminUser, uid ?: "", name)
        }

        if (uid != null) {
            firestore.collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    if (_binding == null) return@addOnSuccessListener
                    val fullName = doc.getString("fullName") ?: doc.getString("name") ?: ""
                    if (fullName.isNotBlank()) {
                        binding.tvUserGreeting.text = "Welcome, ${fullName.split(" ").first()}"
                    }
                    val photoUrl = doc.getString("photoUrl")
                    if (!photoUrl.isNullOrBlank()) {
                        Glide.with(this)
                            .load(photoUrl)
                            .placeholder(R.drawable.ic_default_avatar)
                            .circleCrop()
                            .into(binding.ivUserAvatarHeader)
                    }

                    // Check role: superadmin / admin get the Live CampusFeed Preview
                    val safeCtx = context ?: return@addOnSuccessListener
                    val docRole = doc.getString("role")
                    val effectiveRole = UserRoleHelper.getEffectiveRole(safeCtx, docRole)
                    val r = effectiveRole.trim().lowercase()
                    isAdminUser = (r == "superadmin" || r == "admin" || r == "university_admin")
                    updateCampusFeedVisibility(isAdminUser, uid, fullName.ifBlank { name })
                }
                .addOnFailureListener {
                    if (_binding == null) return@addOnFailureListener
                    val safeCtx = context ?: return@addOnFailureListener
                    val cachedRole = UserRoleHelper.getEffectiveRole(safeCtx)
                    val r = cachedRole.trim().lowercase()
                    isAdminUser = (r == "superadmin" || r == "admin" || r == "university_admin")
                    updateCampusFeedVisibility(isAdminUser, uid, name)
                }
        }
    }

    private fun updateCampusFeedVisibility(isAdmin: Boolean, uid: String, name: String) {
        if (_binding == null) return
        if (isAdmin) {
            binding.layoutCampusFeedLiveAdmin.isVisible = true
            binding.layoutCampusFeedComingSoon.isVisible = false
            if (uid.isNotEmpty()) {
                setupHomeCampusFeed(uid, name)
            }
        } else {
            binding.layoutCampusFeedLiveAdmin.isVisible = false
            binding.layoutCampusFeedComingSoon.isVisible = true
        }
    }

    private fun setupClickListeners() {
        val mainActivity = activity as? MainActivity

        binding.ivUserAvatarHeader.setOnClickListener {
            mainActivity?.showMainTab(R.id.nav_profile)
        }

        binding.btnSearchGlobal.setOnClickListener {
            mainActivity?.openChildDestination(R.id.nav_home, R.id.fragment_library_search)
        }

        binding.btnNotificationHeader.setOnClickListener {
            NotificationsBottomSheet.newInstance().show(childFragmentManager, NotificationsBottomSheet.TAG)
        }

        binding.cardNextEvent.setOnClickListener {
            showDeadlinesBottomSheet()
        }

        binding.cardScholarshipHoliday.setOnClickListener {
            val ctx = context ?: return@setOnClickListener
            startActivity(Intent(ctx, AcademicCalendarActivity::class.java))
        }

        // Quick Access Tiles (Matching Pic 3)
        // Row 1
        binding.tileClubEvent.setOnClickListener { view ->
            val ctx = context ?: return@setOnClickListener
            view.launchActivityWithCardAnimation(Intent(ctx, EventsClubsActivity::class.java))
        }

        binding.tileTransport.setOnClickListener { view ->
            view.applyCardClickAnimation {
                mainActivity?.showMainTab(R.id.nav_campus)
            }
        }

        binding.tileLostFound.setOnClickListener { view ->
            val ctx = context ?: return@setOnClickListener
            view.launchActivityWithCardAnimation(Intent(ctx, LostFoundActivity::class.java))
        }

        binding.tileBloodBank.setOnClickListener { view ->
            val ctx = context ?: return@setOnClickListener
            view.launchActivityWithCardAnimation(Intent(ctx, BloodDonorDirectoryActivity::class.java))
        }

        // Row 2
        binding.tileRoutine.setOnClickListener { view ->
            view.applyCardClickAnimation {
                mainActivity?.showMainTab(R.id.nav_academics)
            }
        }

        binding.tileCoverPage.setOnClickListener { view ->
            val ctx = context ?: return@setOnClickListener
            view.launchActivityWithCardAnimation(Intent(ctx, CoverPageActivity::class.java))
        }

        binding.tileNotice.setOnClickListener { view ->
            view.applyCardClickAnimation {
                val mainActivity = activity as? MainActivity
                mainActivity?.openChildDestination(R.id.nav_home, R.id.nav_notices)
            }
        }

        binding.tileMore.setOnClickListener { view ->
            view.applyCardClickAnimation {
                showMoreToolsBottomSheet()
            }
        }

        binding.cardCampusFeed.setOnClickListener {
            val ctx = context ?: return@setOnClickListener
            if (isAdminUser) {
                startActivity(Intent(ctx, CampusFeedActivity::class.java))
            } else {
                startActivity(Intent(ctx, CampusFeedFeaturesActivity::class.java))
            }
        }

        binding.btnViewFullFeatures.setOnClickListener {
            val ctx = context ?: return@setOnClickListener
            startActivity(Intent(ctx, CampusFeedFeaturesActivity::class.java))
        }

        binding.btnViewFullFeaturesAdmin.setOnClickListener {
            val ctx = context ?: return@setOnClickListener
            startActivity(Intent(ctx, CampusFeedFeaturesActivity::class.java))
        }

        binding.bannerAdminFeatures.setOnClickListener {
            val ctx = context ?: return@setOnClickListener
            startActivity(Intent(ctx, CampusFeedFeaturesActivity::class.java))
        }

        binding.btnAdminFeaturesChip.setOnClickListener {
            val ctx = context ?: return@setOnClickListener
            startActivity(Intent(ctx, CampusFeedFeaturesActivity::class.java))
        }

        binding.btnCampusFeedShowMore.setOnClickListener {
            val ctx = context ?: return@setOnClickListener
            if (isAdminUser) {
                startActivity(Intent(ctx, CampusFeedActivity::class.java))
            } else {
                startActivity(Intent(ctx, CampusFeedFeaturesActivity::class.java))
            }
        }
    }

    private fun showCampusFeedComingSoonDialog() {
        val ctx = context ?: return
        val dialog = BottomSheetDialog(ctx)
        val dialogView = LayoutInflater.from(ctx).inflate(R.layout.dialog_campus_feed_preview, null)
        dialog.setContentView(dialogView)

        dialogView.findViewById<View>(R.id.btnCloseCampusFeedDialog)?.setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<View>(R.id.btnViewFullFeaturesDialog)?.setOnClickListener {
            dialog.dismiss()
            startActivity(Intent(ctx, CampusFeedFeaturesActivity::class.java))
        }

        dialogView.findViewById<View>(R.id.btnNotifyCampusFeed)?.setOnClickListener {
            dialog.dismiss()
            Toast.makeText(ctx, "🚀 Thanks for your interest! We'll notify you as soon as CampusFeed launches.", Toast.LENGTH_LONG).show()
        }

        dialog.show()
    }

    private fun showMoreToolsBottomSheet() {
        val ctx = context ?: return
        val dialog = Dialog(ctx)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val mainActivity = activity as? MainActivity

        val view = LayoutInflater.from(ctx).inflate(R.layout.dialog_more_tools, null)
        dialog.setContentView(view)
        dialog.applyFloatingPostAnimation(ctx, 0.94, 0.78)

        view.findViewById<ImageButton>(R.id.btnCloseMoreTools)?.setOnClickListener {
            dialog.dismiss()
        }

        view.findViewById<View>(R.id.toolItemLibrary).setOnClickListener { v ->
            v.applyCardClickAnimation {
                dialog.dismiss()
                mainActivity?.openChildDestination(R.id.nav_home, R.id.nav_pdf)
            }
        }

        view.findViewById<View>(R.id.toolItemQuestionBank).setOnClickListener { v ->
            v.applyCardClickAnimation {
                dialog.dismiss()
                mainActivity?.openChildDestination(R.id.nav_home, R.id.fragment_question_bank)
            }
        }

        view.findViewById<View>(R.id.toolItemCgpa).setOnClickListener { v ->
            v.launchActivityWithCardAnimation(Intent(ctx, CgpaCalculatorActivity::class.java))
            dialog.dismiss()
        }

        view.findViewById<View>(R.id.toolItemCvMaker)?.setOnClickListener { v ->
            v.applyCardClickAnimation {
                dialog.dismiss()
                com.shuaib.classmate.utils.UpcomingFeaturesHelper.showCvMakerDialog(ctx)
            }
        }

        view.findViewById<View>(R.id.toolItemLiveBus)?.setOnClickListener { v ->
            v.applyCardClickAnimation {
                dialog.dismiss()
                com.shuaib.classmate.utils.UpcomingFeaturesHelper.showLiveBusTrackingDialog(ctx)
            }
        }

        view.findViewById<View>(R.id.toolItemCafeteria)?.setOnClickListener { v ->
            v.applyCardClickAnimation {
                dialog.dismiss()
                com.shuaib.classmate.utils.UpcomingFeaturesHelper.showCafeteriaMenuDialog(ctx)
            }
        }

        view.findViewById<View>(R.id.toolItemHall)?.setOnClickListener { v ->
            v.applyCardClickAnimation {
                dialog.dismiss()
                com.shuaib.classmate.utils.UpcomingFeaturesHelper.showHallAlumniDialog(ctx)
            }
        }

        view.findViewById<View>(R.id.toolItemPolls).setOnClickListener { v ->
            v.applyCardClickAnimation {
                dialog.dismiss()
                mainActivity?.openChildDestination(R.id.nav_home, R.id.nav_polls)
            }
        }

        view.findViewById<View>(R.id.toolItemCalendar).setOnClickListener { v ->
            v.launchActivityWithCardAnimation(Intent(ctx, AcademicCalendarActivity::class.java))
            dialog.dismiss()
        }

        view.findViewById<View>(R.id.toolItemCampusFeed).setOnClickListener { v ->
            v.applyCardClickAnimation {
                dialog.dismiss()
                if (isAdminUser) {
                    v.launchActivityWithCardAnimation(Intent(ctx, CampusFeedActivity::class.java))
                } else {
                    v.launchActivityWithCardAnimation(Intent(ctx, CampusFeedFeaturesActivity::class.java))
                }
            }
        }

        dialog.show()
    }

    private fun setupTopAlerts() {
        topAlertAdapter = TopAlertAdapter(topAlerts)
        binding.vpTopAlerts.adapter = topAlertAdapter

        val density = resources.displayMetrics.density
        binding.vpTopAlerts.setPageTransformer(androidx.viewpager2.widget.MarginPageTransformer((10 * density).toInt()))

        val alertRv = binding.vpTopAlerts.getChildAt(0) as? RecyclerView
        alertRv?.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                when (e.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        (activity as? MainActivity)?.setMainPageSwipeEnabled(false)
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        (activity as? MainActivity)?.setMainPageSwipeEnabled(true)
                    }
                }
                return false
            }
        })

        binding.vpTopAlerts.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val realPos = if (topAlerts.isNotEmpty()) position % topAlerts.size else 0
                updateTopAlertDots(topAlerts.size, realPos)
            }

            override fun onPageScrollStateChanged(state: Int) {
                super.onPageScrollStateChanged(state)
                if (state == ViewPager2.SCROLL_STATE_DRAGGING) {
                    topAlertHandler.removeCallbacks(topAlertRunnable)
                    (activity as? MainActivity)?.setMainPageSwipeEnabled(false)
                } else if (state == ViewPager2.SCROLL_STATE_IDLE) {
                    (activity as? MainActivity)?.setMainPageSwipeEnabled(true)
                    if (!isTopAlertManuallyPaused) {
                        restartTopAlertTimer(6000)
                    }
                }
            }
        })

        binding.btnTopAlertPausePlay.setOnClickListener {
            toggleTopAlertPause()
        }
    }

    private fun toggleTopAlertPause() {
        isTopAlertManuallyPaused = !isTopAlertManuallyPaused
        updateTopAlertPausePlayButton()
        if (isTopAlertManuallyPaused) {
            topAlertHandler.removeCallbacks(topAlertRunnable)
        } else {
            restartTopAlertTimer(4500)
        }
    }

    private fun updateTopAlertPausePlayButton() {
        if (_binding == null) return
        if (isTopAlertManuallyPaused) {
            binding.btnTopAlertPausePlay.setImageResource(R.drawable.ic_play_arrow)
            binding.btnTopAlertPausePlay.contentDescription = "Resume alert animation"
        } else {
            binding.btnTopAlertPausePlay.setImageResource(R.drawable.ic_carousel_pause)
            binding.btnTopAlertPausePlay.contentDescription = "Pause alert animation"
        }
    }

    private fun restartTopAlertTimer(delayMs: Long = 5000) {
        topAlertHandler.removeCallbacks(topAlertRunnable)
        if (_binding != null && !isTopAlertManuallyPaused && topAlerts.size > 1) {
            topAlertHandler.postDelayed(topAlertRunnable, delayMs)
        }
    }

    private fun setupHeroCarousel() {
        heroSlideAdapter = HeroSlideAdapter(heroSlides)
        binding.vpHeroLive.adapter = heroSlideAdapter

        val density = resources.displayMetrics.density
        binding.vpHeroLive.setPageTransformer(androidx.viewpager2.widget.MarginPageTransformer((12 * density).toInt()))

        val heroRv = binding.vpHeroLive.getChildAt(0) as? RecyclerView
        heroRv?.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                when (e.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        (activity as? MainActivity)?.setMainPageSwipeEnabled(false)
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        (activity as? MainActivity)?.setMainPageSwipeEnabled(true)
                    }
                }
                return false
            }
        })

        binding.vpHeroLive.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val realPos = if (heroSlides.isNotEmpty()) position % heroSlides.size else 0
                updateHeroDots(heroSlides.size, realPos)
            }

            override fun onPageScrollStateChanged(state: Int) {
                super.onPageScrollStateChanged(state)
                if (state == ViewPager2.SCROLL_STATE_DRAGGING) {
                    heroScrollHandler.removeCallbacks(heroScrollRunnable)
                    (activity as? MainActivity)?.setMainPageSwipeEnabled(false)
                } else if (state == ViewPager2.SCROLL_STATE_IDLE) {
                    (activity as? MainActivity)?.setMainPageSwipeEnabled(true)
                    if (!isHeroManuallyPaused) {
                        restartHeroTimer(6500)
                    }
                }
            }
        })

        binding.btnHeroPausePlay.setOnClickListener {
            toggleHeroPause()
        }
    }

    private fun toggleHeroPause() {
        isHeroManuallyPaused = !isHeroManuallyPaused
        updateHeroPausePlayButton()
        if (isHeroManuallyPaused) {
            heroScrollHandler.removeCallbacks(heroScrollRunnable)
        } else {
            restartHeroTimer(5000)
        }
    }

    private fun updateHeroPausePlayButton() {
        if (_binding == null) return
        if (isHeroManuallyPaused) {
            binding.btnHeroPausePlay.setImageResource(R.drawable.ic_play_arrow)
            binding.btnHeroPausePlay.contentDescription = "Resume card animation"
        } else {
            binding.btnHeroPausePlay.setImageResource(R.drawable.ic_carousel_pause)
            binding.btnHeroPausePlay.contentDescription = "Pause card animation"
        }
    }

    private fun restartHeroTimer(delayMs: Long = 5000) {
        heroScrollHandler.removeCallbacks(heroScrollRunnable)
        if (_binding != null && !isHeroManuallyPaused && heroSlides.size > 1) {
            heroScrollHandler.postDelayed(heroScrollRunnable, delayMs)
        }
    }

    private fun loadData() {
        loadUpcomingClasses()
        loadDeadlinesAndCalendar()
        loadTopAlerts()
    }

    private fun loadUpcomingClasses() {
        val todayStr = DateHelper.todayDayString()
        val ctx = context ?: return
        val appContext = ctx.applicationContext
        val semester = AppPreferences(appContext).getUserSemester().ifBlank { "2-1" }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                if (activeExceptions.isEmpty()) {
                    try {
                        val activeExp = calendarRepo.getActiveExceptionForDate(LocalDate.now())
                        if (activeExp != null && activeExceptions.isEmpty()) {
                            activeExceptions = listOf(activeExp)
                        }
                    } catch (_: Exception) {}
                }

                val periods = TimetableRepository.getInstance(appContext).getPeriodsForDay(todayStr)
                if (_binding == null) return@launch

                val isRoutineAdded = com.shuaib.classmate.utils.GuestUserHelper.isDepartmentRoutineAdded(appContext)

                todayPeriods = if (!isRoutineAdded) {
                    emptyList()
                } else {
                    periods.filter { period ->
                        val deptMatches = com.shuaib.classmate.utils.GuestUserHelper.doesPeriodMatchUserDepartment(
                            appContext,
                            period.departmentId,
                            period.department
                        )
                        if (!deptMatches) return@filter false

                        val b = if (period.batch == "14") "2-1" else period.batch
                        b.isEmpty() || b.equals("all", ignoreCase = true) || b == semester
                    }.sortedBy { DateHelper.parseTimeToMinutes(it.startTime) }
                }

                updateHeroCarousel()
            } catch (e: Exception) {
                todayPeriods = emptyList()
                updateHeroCarousel()
            }
        }
    }

    private fun loadDeadlinesAndCalendar() {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            countdownListener?.remove()
            countdownListener = CountdownManager.getUserCountdowns { assignments ->
                if (_binding == null) return@getUserCountdowns
                userAssignments = assignments
                rebuildDeadlinesAndCalendar()
            }
        }

        calendarExceptionsListener?.remove()
        calendarExceptionsListener = calendarRepo.getExceptions(
            onSuccess = { exceptions ->
                if (_binding == null) return@getExceptions
                activeExceptions = exceptions
                rebuildDeadlinesAndCalendar()
                loadTopAlerts()
                updateHeroCarousel()
            },
            onFailure = {}
        )

        val ctx = context ?: return
        val noticeRepo = NoticeRepository.getInstance(ctx)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val notices = noticeRepo.getNoticesLocal()
                if (_binding == null) return@launch
                cachedNotices = notices
                rebuildDeadlinesAndCalendar()
            } catch (_: Exception) {}
        }
    }

    private fun parseDaysRemaining(dateStr: String): Long {
        if (dateStr.isBlank()) return 999L
        return try {
            val target = when {
                dateStr.length >= 10 && dateStr[4] == '-' -> LocalDate.parse(dateStr.take(10))
                else -> {
                    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.US)
                    LocalDate.parse(dateStr, formatter)
                }
            }
            ChronoUnit.DAYS.between(LocalDate.now(), target)
        } catch (_: Exception) {
            999L
        }
    }

    private fun formatDueDate(daysRemaining: Long, dateStr: String): String {
        return when {
            daysRemaining < 0L -> "Overdue (${-daysRemaining}d ago)"
            daysRemaining == 0L -> "Due Today ($dateStr)"
            daysRemaining == 1L -> "Due Tomorrow ($dateStr)"
            daysRemaining <= 7L -> "Due in $daysRemaining days ($dateStr)"
            else -> "Due $dateStr"
        }
    }

    private fun rebuildDeadlinesAndCalendar() {
        val items = mutableListOf<DeadlineItem>()

        // 1. User countdowns (Only upcoming items: days >= 0L)
        for (a in userAssignments) {
            val days = parseDaysRemaining(a.submissionDate)
            if (days >= 0L) {
                val type = if (a.type.isNotBlank()) a.type.lowercase() else "assignment"
                val badge = when (type) {
                    "quiz", "class_test" -> "QUIZ"
                    "exam" -> "EXAM"
                    "project" -> "PROJECT"
                    else -> "ASSIGNMENT"
                }
                items.add(
                    DeadlineItem(
                        id = a.id,
                        type = type,
                        badgeText = badge,
                        sourceText = "My Deadlines",
                        title = a.subject.ifBlank { "Course Deadline" },
                        subtitle = a.topic.ifBlank { "Submission required" },
                        dateString = a.submissionDate,
                        daysRemaining = days,
                        formattedDue = formatDueDate(days, a.submissionDate),
                        isUserCountdown = true,
                        assignment = a
                    )
                )
            }
        }

        // 2. Class / Notice deadlines (Only upcoming items: days >= 0L)
        for (n in cachedNotices) {
            // Strictly exclude routine class cancellation notices or substitute notices
            if (n.isCancel || n.isSub) continue
            val isExplicitDeadline = n.isAssignment || n.isClassTest ||
                    n.type.equals("assignment", ignoreCase = true) ||
                    n.type.equals("deadline", ignoreCase = true) ||
                    n.type.equals("exam", ignoreCase = true) ||
                    (n.submissionDate.isNotBlank() && n.deadlineType in listOf("quiz", "exam", "class_test", "assignment"))
            if (isExplicitDeadline) {
                val dateStr = n.submissionDate.ifBlank {
                    n.deadlineAt?.toDate()?.let {
                        java.time.Instant.ofEpochMilli(it.time)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate().toString()
                    } ?: ""
                }
                if (dateStr.isNotBlank()) {
                    val days = parseDaysRemaining(dateStr)
                    if (days >= 0L) {
                        val badge = when {
                            n.isClassTest || n.deadlineType == "exam" -> "EXAM"
                            n.deadlineType == "quiz" -> "QUIZ"
                            else -> "ASSIGNMENT"
                        }
                        items.add(
                            DeadlineItem(
                                id = n.id,
                                type = if (n.isClassTest) "exam" else "assignment",
                                badgeText = badge,
                                sourceText = n.createdByName.ifBlank { "Class Notice" },
                                title = if (n.subject.isNotBlank()) n.subject else n.title,
                                subtitle = if (n.topic.isNotBlank()) n.topic else n.body.take(60),
                                dateString = dateStr,
                                daysRemaining = days,
                                formattedDue = formatDueDate(days, dateStr),
                                isUserCountdown = false,
                                notice = n
                            )
                        )
                    }
                }
            }
        }

        // 3. Academic calendar exceptions (upcoming events/holidays - strictly upcoming)
        val today = LocalDate.now()
        for (e in activeExceptions) {
            val sDate = try { LocalDate.parse(e.startDate) } catch (_: Exception) { null }
            val eDate = try { LocalDate.parse(e.endDate) } catch (_: Exception) { sDate }
            if (sDate != null && eDate != null) {
                // Filter out past holidays/events that ended before today
                if (eDate.isBefore(today)) continue

                val isCurrentlyActive = (!today.isBefore(sDate) && !today.isAfter(eDate))
                val daysUntilStart = ChronoUnit.DAYS.between(today, sDate)
                if (isCurrentlyActive || daysUntilStart in 0L..30L) {
                    val formatted = when {
                        isCurrentlyActive -> "Active Now (${e.startDate} - ${e.endDate})"
                        daysUntilStart == 0L -> "Starts Today (${e.startDate})"
                        daysUntilStart == 1L -> "Starts Tomorrow (${e.startDate})"
                        else -> "In $daysUntilStart days (${e.startDate})"
                    }
                    items.add(
                        DeadlineItem(
                            id = e.id,
                            type = "calendar",
                            badgeText = "EVENT",
                            sourceText = "Academic Calendar",
                            title = e.title.ifBlank { "University Event" },
                            subtitle = e.reason.ifBlank { "Campus Schedule" },
                            dateString = e.startDate,
                            daysRemaining = if (isCurrentlyActive) 0L else Math.max(0L, daysUntilStart),
                            formattedDue = formatted,
                            isUserCountdown = false
                        )
                    )
                }
            }
        }

        allDeadlineItems = items.sortedBy { it.daysRemaining }

        updateNextEventCard()
        updateHolidayCard()
        updateHeroCarousel()
    }

    private fun updateNextEventCard() {
        if (_binding == null) return
        val ctx = context ?: return
        val flipper = binding.flipperNextEvent
        flipper.removeAllViews()
        deadlineFlipperHandler.removeCallbacks(deadlineFlipperRunnable)

        if (allDeadlineItems.isEmpty()) {
            val itemLayout = LinearLayout(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                orientation = LinearLayout.VERTICAL
                val titleView = TextView(ctx).apply {
                    text = "No Deadlines Today"
                    setTextColor(ContextCompat.getColor(ctx, R.color.cm_text_primary))
                    textSize = 14f
                    maxLines = 1
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                }
                val subView = TextView(ctx).apply {
                    text = "All assignments caught up!"
                    setTextColor(ContextCompat.getColor(ctx, R.color.cm_text_muted))
                    textSize = 12f
                    maxLines = 1
                }
                addView(titleView)
                addView(subView)
            }
            flipper.addView(itemLayout)
            binding.tvNextEventAction.text = "Add deadline >"
            return
        }

        binding.tvNextEventAction.text = "${allDeadlineItems.size} events >"

        for (item in allDeadlineItems.take(5)) {
            val itemLayout = LinearLayout(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                orientation = LinearLayout.VERTICAL
                val titleView = TextView(ctx).apply {
                    text = "${item.badgeText}: ${item.title}"
                    setTextColor(ContextCompat.getColor(ctx, R.color.cm_text_primary))
                    textSize = 14f
                    maxLines = 1
                    ellipsize = android.text.TextUtils.TruncateAt.END
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                }
                val subView = TextView(ctx).apply {
                    text = "${item.formattedDue} · ${item.subtitle}"
                    setTextColor(ContextCompat.getColor(ctx, R.color.cm_text_muted))
                    textSize = 12f
                    maxLines = 1
                    ellipsize = android.text.TextUtils.TruncateAt.END
                }
                addView(titleView)
                addView(subView)
            }
            flipper.addView(itemLayout)
        }

        if (flipper.childCount > 1) {
            flipper.inAnimation = AnimationUtils.loadAnimation(ctx, R.anim.slide_down_in)
            flipper.outAnimation = AnimationUtils.loadAnimation(ctx, R.anim.slide_up_out)
            deadlineFlipperHandler.postDelayed(deadlineFlipperRunnable, 4500)
        }
    }

    private fun updateHolidayCard() {
        if (_binding == null) return
        val ctx = context ?: return
        val flipper = binding.flipperHoliday
        flipper.removeAllViews()
        holidayFlipperHandler.removeCallbacks(holidayFlipperRunnable)

        if (activeExceptions.isEmpty()) {
            val itemLayout = LinearLayout(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                orientation = LinearLayout.VERTICAL
                val titleView = TextView(ctx).apply {
                    text = "Regular Schedule"
                    setTextColor(ContextCompat.getColor(ctx, R.color.cm_text_primary))
                    textSize = 14f
                    maxLines = 1
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                }
                val subView = TextView(ctx).apply {
                    text = "University classes in session"
                    setTextColor(ContextCompat.getColor(ctx, R.color.cm_text_muted))
                    textSize = 12f
                    maxLines = 1
                }
                addView(titleView)
                addView(subView)
            }
            flipper.addView(itemLayout)
            return
        }

        for (exc in activeExceptions.take(5)) {
            val itemLayout = LinearLayout(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                orientation = LinearLayout.VERTICAL
                val titleView = TextView(ctx).apply {
                    text = exc.title.ifBlank { "Campus Break" }
                    setTextColor(ContextCompat.getColor(ctx, R.color.cm_text_primary))
                    textSize = 14f
                    maxLines = 1
                    ellipsize = android.text.TextUtils.TruncateAt.END
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                }
                val subView = TextView(ctx).apply {
                    text = "${exc.reason.ifBlank { "Classes suspended" }} (${exc.startDate})"
                    setTextColor(ContextCompat.getColor(ctx, R.color.cm_text_muted))
                    textSize = 12f
                    maxLines = 1
                    ellipsize = android.text.TextUtils.TruncateAt.END
                }
                addView(titleView)
                addView(subView)
            }
            flipper.addView(itemLayout)
        }

        if (flipper.childCount > 1) {
            flipper.inAnimation = AnimationUtils.loadAnimation(ctx, R.anim.slide_down_in)
            flipper.outAnimation = AnimationUtils.loadAnimation(ctx, R.anim.slide_up_out)
            holidayFlipperHandler.postDelayed(holidayFlipperRunnable, 5000)
        }
    }

    private fun showDeadlinesBottomSheet() {
        val ctx = context ?: return
        val dialog = BottomSheetDialog(ctx)
        val sheetBinding = DialogBottomSheetDeadlinesBinding.inflate(layoutInflater)
        dialog.setContentView(sheetBinding.root)

        val mainActivity = activity as? MainActivity

        if (allDeadlineItems.isEmpty()) {
            sheetBinding.layoutDeadlinesEmpty.visibility = View.VISIBLE
            sheetBinding.rvDeadlinesList.visibility = View.GONE
            sheetBinding.tvDeadlinesCountSubtitle.text = "0 upcoming deadlines"
        } else {
            sheetBinding.layoutDeadlinesEmpty.visibility = View.GONE
            sheetBinding.rvDeadlinesList.visibility = View.VISIBLE
            sheetBinding.tvDeadlinesCountSubtitle.text =
                "${allDeadlineItems.size} upcoming deadline${if (allDeadlineItems.size > 1) "s" else ""}"

            val adapter = DeadlinesBottomSheetAdapter(
                items = allDeadlineItems,
                onDelete = { item ->
                    CountdownManager.removeCountdown(
                        item.id,
                        onSuccess = {
                            Toast.makeText(ctx, "Deadline removed", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                        },
                        onFailure = { err ->
                            Toast.makeText(ctx, "Failed: $err", Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                onViewNotice = { notice ->
                    dialog.dismiss()
                    mainActivity?.openChildDestination(R.id.nav_home, R.id.nav_notices)
                },
                onItemClick = { item ->
                    if (item.notice != null) {
                        dialog.dismiss()
                        mainActivity?.openChildDestination(R.id.nav_home, R.id.nav_notices)
                    } else if (item.type == "calendar") {
                        dialog.dismiss()
                        startActivity(Intent(ctx, AcademicCalendarActivity::class.java))
                    }
                }
            )

            sheetBinding.rvDeadlinesList.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(ctx)
            sheetBinding.rvDeadlinesList.adapter = adapter
        }

        sheetBinding.btnAddNewDeadline.setOnClickListener {
            dialog.dismiss()
            showAddCountdownDialog()
        }

        sheetBinding.layoutFooterPolls.setOnClickListener {
            dialog.dismiss()
            mainActivity?.openChildDestination(R.id.nav_home, R.id.nav_polls)
        }

        dialog.show()
    }

    private fun showAddCountdownDialog() {
        val ctx = context ?: return
        val dialogView = LayoutInflater.from(ctx).inflate(R.layout.dialog_add_countdown, null)
        val etSubject = dialogView.findViewById<android.widget.EditText>(R.id.etCountdownSubject)
        val etTopic = dialogView.findViewById<android.widget.EditText>(R.id.etCountdownTopic)
        val etDate = dialogView.findViewById<android.widget.EditText>(R.id.etCountdownDate)
        val etTime = dialogView.findViewById<android.widget.EditText>(R.id.etCountdownTime)

        etDate.isFocusable = false
        etDate.setOnClickListener {
            val cal = Calendar.getInstance()
            android.app.DatePickerDialog(ctx, { _, year, month, dayOfMonth ->
                etDate.setText(String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth))
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        etTime.isFocusable = false
        etTime.setOnClickListener {
            val cal = Calendar.getInstance()
            android.app.TimePickerDialog(ctx, { _, hour, minute ->
                etTime.setText(String.format(Locale.US, "%02d:%02d", hour, minute))
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
        }

        androidx.appcompat.app.AlertDialog.Builder(ctx)
            .setTitle("Add Upcoming Deadline")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val subject = etSubject.text.toString().trim()
                val topic = etTopic.text.toString().trim()
                val date = etDate.text.toString().trim()
                val time = etTime.text.toString().trim()

                if (subject.isNotEmpty() && topic.isNotEmpty() && date.isNotEmpty()) {
                    val assignment = Assignment(
                        subject = subject,
                        topic = topic,
                        submissionDate = date,
                        submissionTime = if (time.isNotEmpty()) time else "23:59",
                        postedBy = auth.currentUser?.uid ?: ""
                    )
                    CountdownManager.addCountdown(
                        assignment,
                        onSuccess = {
                            Toast.makeText(ctx, "Deadline added!", Toast.LENGTH_SHORT).show()
                        },
                        onFailure = { err ->
                            Toast.makeText(ctx, "Failed: $err", Toast.LENGTH_SHORT).show()
                        }
                    )
                } else {
                    Toast.makeText(ctx, "Please enter subject, topic and date", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun getActiveHolidayException(): AcademicCalendarException? {
        val today = LocalDate.now()
        val ctx = context ?: return null
        val semester = AppPreferences(ctx.applicationContext).getUserSemester().ifBlank { "2-1" }
        return activeExceptions
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
                val sDate = try { LocalDate.parse(ex.startDate) } catch (_: Exception) { null }
                val eDate = try { LocalDate.parse(ex.endDate) } catch (_: Exception) { sDate }
                sDate != null && eDate != null && !today.isBefore(sDate) && !today.isAfter(eDate)
            }
    }

    private fun updateHeroCarousel() {
        if (_binding == null) return
        val ctx = context ?: return
        val inflater = LayoutInflater.from(ctx)
        val mainActivity = activity as? MainActivity

        val appContext = ctx.applicationContext
        val semester = AppPreferences(appContext).getUserSemester().ifBlank { "2-1" }
        val nowSec = LocalTime.now().let { it.hour * 3600 + it.minute * 60 + it.second }

        val slides = mutableListOf<HeroSlide>()

        val isRoutineUninitialized = !com.shuaib.classmate.utils.GuestUserHelper.isDepartmentRoutineAdded(appContext)

        // If routine is not initialized or user is anonymous / other uninitialized department:
        if (isRoutineUninitialized) {
            slides.add(
                HeroSlide(
                    categoryTitle = "📅 CLASS ROUTINE",
                    categoryIconRes = R.drawable.ic_timetable,
                    categoryColor = Color.parseColor("#93C5FD"),
                    actionText = "Routine >",
                    statusBadge = "ROUTINE",
                    contextBadge = "GSTU Go",
                    title = "Class Routine Appears Here...",
                    detail1 = "🕒 Real-time class schedule",
                    detail2 = "📍 Department synchronized",
                    detail3 = "",
                    bgDrawableRes = R.drawable.bg_hero_gradient_indigo,
                    onClick = { mainActivity?.showMainTab(R.id.nav_profile) }
                )
            )
            slides.add(
                HeroSlide(
                    categoryTitle = "📢 ROUTINE REQUEST",
                    categoryIconRes = R.drawable.ic_building,
                    categoryColor = Color.parseColor("#FDE047"),
                    actionText = "Routine >",
                    statusBadge = "SUPPORT",
                    contextBadge = "Department",
                    title = "Contact Us through your department",
                    detail1 = "🤝 Reach out to your CR",
                    detail2 = "✨ We will configure your department timetable",
                    detail3 = "",
                    bgDrawableRes = R.drawable.bg_hero_gradient_purple,
                    onClick = { mainActivity?.showMainTab(R.id.nav_profile) }
                )
            )
        } else {
            // 0. Active Holiday / Academic Exception check:
            // When a holiday/vacation/suspension is active today, do NOT show regular classes
            val activeHoliday = getActiveHolidayException()

            if (activeHoliday != null) {
                val holidayTitle = activeHoliday.title.ifBlank { "University Holiday" }
                val holidayReason = activeHoliday.reason.ifBlank { "Classes are suspended today for academic holiday" }
                val dateRangeStr = if (activeHoliday.startDate.isNotBlank() && activeHoliday.endDate.isNotBlank()) {
                    "${activeHoliday.startDate} to ${activeHoliday.endDate}"
                } else "All Day"

                slides.add(
                    HeroSlide(
                        categoryTitle = "🏖️ ${holidayTitle.uppercase()} - DAY OFF",
                        categoryIconRes = R.drawable.ic_calendar,
                        categoryColor = Color.parseColor("#A7F3D0"),
                        actionText = "Full routine >",
                        statusBadge = "OFF TODAY",
                        contextBadge = "Sem $semester",
                        title = holidayTitle,
                        detail1 = "🌴 $holidayReason",
                        detail2 = "📅 $dateRangeStr",
                        detail3 = "No regular classes scheduled today",
                        bgDrawableRes = R.drawable.bg_hero_gradient_green,
                        onClick = { mainActivity?.showMainTab(R.id.nav_academics) }
                    )
                )
            } else {
                // 1. Live Class (if any is active right now)
                val livePeriod = todayPeriods.firstOrNull { period ->
                    if (period.isCancelled) return@firstOrNull false
                    val startSec = parseTimeToSeconds(period.startTime)
                    val endSec = parseTimeToSeconds(period.endTime)
                    nowSec in startSec..endSec
                }

                if (livePeriod != null) {
                    val endSec = parseTimeToSeconds(livePeriod.endTime)
                    val remainingMins = Math.max(1, (endSec - nowSec) / 60)
                    slides.add(
                        HeroSlide(
                            categoryTitle = "🔴 LIVE CLASS NOW",
                            categoryIconRes = R.drawable.ic_clock,
                            categoryColor = Color.parseColor("#FBBF24"),
                            actionText = "Full routine >",
                            statusBadge = "LIVE NOW",
                            contextBadge = "Ends in ${remainingMins}m",
                            title = livePeriod.subject,
                            detail1 = if (livePeriod.room.isNotBlank()) "📍 Room ${livePeriod.room}" else "📍 Hall",
                            detail2 = "🕒 ${livePeriod.startTime} - ${livePeriod.endTime}",
                            detail3 = "👤 ${livePeriod.teacher.ifBlank { "Faculty" }}",
                            bgDrawableRes = R.drawable.bg_hero_gradient_purple,
                            onClick = { mainActivity?.showMainTab(R.id.nav_academics) }
                        )
                    )
                }

                // 2. Next Upcoming Class and other upcoming classes today
                val upcomingClassesToday = todayPeriods.filter { period ->
                    if (period.isCancelled) return@filter false
                    val startSec = parseTimeToSeconds(period.startTime)
                    nowSec < startSec
                }.sortedBy { parseTimeToSeconds(it.startTime) }

                if (upcomingClassesToday.isNotEmpty()) {
                    val nextPeriod = upcomingClassesToday.first()
                    val nextStartSec = parseTimeToSeconds(nextPeriod.startTime)
                    val diffMins = Math.max(0, (nextStartSec - nowSec) / 60)
                    val startsInText = if (diffMins < 60) "Starts in ${diffMins}m" else "Starts in ${diffMins / 60}h ${diffMins % 60}m"

                    slides.add(
                        HeroSlide(
                            categoryTitle = "⏰ NEXT CLASS TODAY",
                            categoryIconRes = R.drawable.ic_clock,
                            categoryColor = Color.parseColor("#93C5FD"),
                            actionText = "Full routine >",
                            statusBadge = "UPCOMING",
                            contextBadge = startsInText,
                            title = nextPeriod.subject,
                            detail1 = if (nextPeriod.room.isNotBlank()) "📍 Room ${nextPeriod.room}" else "📍 Hall",
                            detail2 = "🕒 ${nextPeriod.startTime} - ${nextPeriod.endTime}",
                            detail3 = "👤 ${nextPeriod.teacher.ifBlank { "Faculty" }}",
                            bgDrawableRes = R.drawable.bg_hero_gradient_purple,
                            onClick = { mainActivity?.showMainTab(R.id.nav_academics) }
                        )
                    )

                    // Add additional upcoming classes today if any
                    for (period in upcomingClassesToday.drop(1).take(2)) {
                        slides.add(
                            HeroSlide(
                                categoryTitle = "🕒 LATER TODAY",
                                categoryIconRes = R.drawable.ic_clock,
                                categoryColor = Color.parseColor("#A5B4FC"),
                                actionText = "Full routine >",
                                statusBadge = "SCHEDULED",
                                contextBadge = "Sem $semester",
                                title = period.subject,
                                detail1 = if (period.room.isNotBlank()) "📍 Room ${period.room}" else "📍 Hall",
                                detail2 = "🕒 ${period.startTime} - ${period.endTime}",
                                detail3 = "👤 ${period.teacher.ifBlank { "Faculty" }}",
                                bgDrawableRes = R.drawable.bg_hero_gradient_purple,
                                onClick = { mainActivity?.showMainTab(R.id.nav_academics) }
                            )
                        )
                    }
                }
            }

            // 3. Upcoming Deadlines (Strictly live, next, and upcoming deadlines in bottom hero)
            val upcomingDeadlines = allDeadlineItems.filter { it.type != "calendar" && it.daysRemaining >= 0L }
            for (upcoming in upcomingDeadlines.take(3)) {
                slides.add(
                    HeroSlide(
                        categoryTitle = "📌 UPCOMING DEADLINE",
                        categoryIconRes = R.drawable.ic_calendar,
                        categoryColor = Color.parseColor("#C4B5FD"),
                        actionText = "All deadlines >",
                        statusBadge = upcoming.badgeText,
                        contextBadge = upcoming.formattedDue,
                        title = upcoming.title,
                        detail1 = "🗒️ ${upcoming.subtitle}",
                        detail2 = "🕒 ${upcoming.dateString}",
                        detail3 = "Tap to view details",
                        bgDrawableRes = R.drawable.bg_hero_gradient_indigo,
                        onClick = { showDeadlinesBottomSheet() }
                    )
                )
            }

            // 4. Fallback if no live class, no next class, and no upcoming deadlines
            if (slides.isEmpty()) {
                if (todayPeriods.isEmpty()) {
                    slides.add(
                        HeroSlide(
                            categoryTitle = "✨ FREE DAY! NO CLASSES TODAY",
                            categoryIconRes = R.drawable.ic_calendar,
                            categoryColor = Color.parseColor("#A7F3D0"),
                            actionText = "Full routine >",
                            statusBadge = "OFF TODAY",
                            contextBadge = "Sem $semester",
                            title = "Enjoy your day off!",
                            detail1 = "📍 Campus",
                            detail2 = "🕒 All day",
                            detail3 = "Review library resources",
                            bgDrawableRes = R.drawable.bg_hero_gradient_green,
                            onClick = { mainActivity?.showMainTab(R.id.nav_academics) }
                        )
                    )
                } else {
                    slides.add(
                        HeroSlide(
                            categoryTitle = "✨ ALL CLASSES COMPLETED",
                            categoryIconRes = R.drawable.ic_timetable,
                            categoryColor = Color.parseColor("#A7F3D0"),
                            actionText = "Full routine >",
                            statusBadge = "ALL DONE",
                            contextBadge = "Sem $semester",
                            title = "You're done for today!",
                            detail1 = "🎉 Finished all ${todayPeriods.size} scheduled classes",
                            detail2 = "Have a restful evening",
                            detail3 = "",
                            bgDrawableRes = R.drawable.bg_hero_gradient_green,
                            onClick = { mainActivity?.showMainTab(R.id.nav_academics) }
                        )
                    )
                }
            }
        }

        heroSlides = slides
        heroSlideAdapter?.updateList(heroSlides)
        if (heroSlides.size > 1) {
            val initialPos = (1000 / 2) * heroSlides.size
            binding.vpHeroLive.setCurrentItem(initialPos, false)
        }
        setupHeroDots(heroSlides.size)
    }

    private fun setupHeroDots(count: Int) {
        if (_binding == null) return
        val ctx = context ?: return
        binding.layoutHeroDots.removeAllViews()
        heroScrollHandler.removeCallbacks(heroScrollRunnable)

        if (count <= 1) {
            binding.layoutHeroControls.visibility = View.GONE
            return
        }

        binding.layoutHeroControls.visibility = View.VISIBLE
        updateHeroPausePlayButton()
        val density = resources.displayMetrics.density
        val currentReal = if (count > 0) binding.vpHeroLive.currentItem % count else 0

        for (i in 0 until count) {
            val dot = View(ctx).apply {
                val isSelected = (i == currentReal)
                val dotWidth = if (isSelected) (16 * density).toInt() else (6 * density).toInt()
                val dotHeight = (5 * density).toInt()
                val lp = LinearLayout.LayoutParams(dotWidth, dotHeight).apply {
                    setMargins((2.5f * density).toInt(), 0, (2.5f * density).toInt(), 0)
                }
                layoutParams = lp
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 3 * density
                    setColor(Color.WHITE)
                }
                alpha = if (isSelected) 1f else 0.45f
                setOnClickListener {
                    val cur = binding.vpHeroLive.currentItem
                    val curReal = if (count > 0) cur % count else 0
                    val diff = i - curReal
                    binding.vpHeroLive.setCurrentItem(cur + diff, true)
                }
            }
            binding.layoutHeroDots.addView(dot)
        }

        if (!isHeroManuallyPaused) {
            heroScrollHandler.postDelayed(heroScrollRunnable, 5000)
        }
    }

    private fun updateHeroDots(count: Int, selectedIndex: Int) {
        if (_binding == null || count <= 1) return
        val density = resources.displayMetrics.density
        for (i in 0 until count) {
            val dot = binding.layoutHeroDots.getChildAt(i) ?: continue
            val isSelected = (i == selectedIndex)
            val targetWidth = if (isSelected) (16 * density).toInt() else (6 * density).toInt()
            val lp = dot.layoutParams
            lp.width = targetWidth
            dot.layoutParams = lp
            dot.alpha = if (isSelected) 1f else 0.45f
        }
    }

    private fun setupTopAlertDots(count: Int) {
        if (_binding == null) return
        val ctx = context ?: return
        binding.layoutTopAlertDots.removeAllViews()
        topAlertHandler.removeCallbacks(topAlertRunnable)

        if (count <= 1) {
            binding.layoutTopAlertControls.visibility = View.GONE
            return
        }

        binding.layoutTopAlertControls.visibility = View.VISIBLE
        updateTopAlertPausePlayButton()
        val density = resources.displayMetrics.density
        val currentReal = if (count > 0) binding.vpTopAlerts.currentItem % count else 0

        for (i in 0 until count) {
            val dot = View(ctx).apply {
                val isSelected = (i == currentReal)
                val dotWidth = if (isSelected) (16 * density).toInt() else (6 * density).toInt()
                val dotHeight = (5 * density).toInt()
                val lp = LinearLayout.LayoutParams(dotWidth, dotHeight).apply {
                    setMargins((2.5f * density).toInt(), 0, (2.5f * density).toInt(), 0)
                }
                layoutParams = lp
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 3 * density
                    setColor(ContextCompat.getColor(ctx, R.color.cm_text_muted))
                }
                alpha = if (isSelected) 1f else 0.4f
                setOnClickListener {
                    val cur = binding.vpTopAlerts.currentItem
                    val curReal = if (count > 0) cur % count else 0
                    val diff = i - curReal
                    binding.vpTopAlerts.setCurrentItem(cur + diff, true)
                }
            }
            binding.layoutTopAlertDots.addView(dot)
        }

        if (!isTopAlertManuallyPaused) {
            topAlertHandler.postDelayed(topAlertRunnable, 5000)
        }
    }

    private fun updateTopAlertDots(count: Int, selectedIndex: Int) {
        if (_binding == null || count <= 1) return
        val density = resources.displayMetrics.density
        for (i in 0 until count) {
            val dot = binding.layoutTopAlertDots.getChildAt(i) ?: continue
            val isSelected = (i == selectedIndex)
            val targetWidth = if (isSelected) (16 * density).toInt() else (6 * density).toInt()
            val lp = dot.layoutParams
            lp.width = targetWidth
            dot.layoutParams = lp
            dot.alpha = if (isSelected) 1f else 0.4f
        }
    }

    private fun loadTopAlerts() {
        val alerts = mutableListOf<TopAlertItem>()
        val ctx = context ?: return
        val today = LocalDate.now()

        // 1. Academic Holidays / Campus Suspensions (strictly active or upcoming, never past)
        for (exc in activeExceptions) {
            val alertId = "holiday_${exc.id}"
            if (dismissedAlertIds.contains(alertId)) continue
            val sDate = try { LocalDate.parse(exc.startDate) } catch (_: Exception) { null }
            val eDate = try { LocalDate.parse(exc.endDate) } catch (_: Exception) { sDate }
            if (sDate != null && eDate != null) {
                // If the holiday ended before today, skip it (do not show past items)
                if (eDate.isBefore(today)) continue

                val isCurrent = !today.isBefore(sDate) && !today.isAfter(eDate)
                val daysToStart = ChronoUnit.DAYS.between(today, sDate)
                if (isCurrent || daysToStart in 0L..14L) {
                    val timeTag = when {
                        isCurrent -> "Active Now"
                        daysToStart == 0L -> "Starts Today"
                        daysToStart == 1L -> "Starts Tomorrow"
                        else -> "In ${daysToStart}d"
                    }
                    alerts.add(
                        TopAlertItem(
                            id = alertId,
                            type = TopAlertType.HOLIDAY,
                            title = "🏖️ ${exc.title.ifBlank { "University Holiday" }}",
                            subtitle = "${exc.reason.ifBlank { "Classes suspended" }} (${exc.startDate} to ${exc.endDate})",
                            timeTag = timeTag,
                            bgDrawableRes = R.drawable.bg_alert_banner_orange,
                            iconRes = R.drawable.ic_school,
                            onClick = {
                                val c = context ?: return@TopAlertItem
                                startActivity(Intent(c, AcademicCalendarActivity::class.java))
                            },
                            onDismiss = {
                                dismissTopAlert(alertId)
                            }
                        )
                    )
                }
            }
        }

        // 2. Class Cancellation alerts for today (from routine)
        val cancelledPeriods = todayPeriods.filter { it.isCancelled }
        for (c in cancelledPeriods) {
            val alertId = "cancelled_${c.id.ifBlank { c.subject }}"
            if (dismissedAlertIds.contains(alertId)) continue
            alerts.add(
                TopAlertItem(
                    id = alertId,
                    type = TopAlertType.NOTICE,
                    title = "⚠️ Class Cancelled: ${c.subject}",
                    subtitle = "${c.startTime} - ${c.endTime} · ${c.teacher.ifBlank { "Faculty" }} (Room ${c.room.ifBlank { "-" }})",
                    timeTag = "Today",
                    bgDrawableRes = R.drawable.bg_alert_banner_red,
                    iconRes = R.drawable.ic_close,
                    onClick = {
                        (activity as? MainActivity)?.showMainTab(R.id.nav_academics)
                    },
                    onDismiss = {
                        dismissTopAlert(alertId)
                    }
                )
            )
        }

        val noticeRepo = NoticeRepository.getInstance(ctx)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val notices = noticeRepo.getNoticesLocal()
                val activeNotices = notices.filter { !dismissedAlertIds.contains(it.id) }

                // 3. Campus Notices (excluding past deadlines/expired notices)
                for (n in activeNotices.take(4)) {
                    val isPast = if (n.submissionDate.isNotBlank()) {
                        parseDaysRemaining(n.submissionDate) < 0L
                    } else false
                    if (isPast) continue

                    val isUrgent = n.priority.equals("urgent", ignoreCase = true) || n.isCancel
                    alerts.add(
                        TopAlertItem(
                            id = n.id,
                            type = TopAlertType.NOTICE,
                            title = n.title.ifBlank { "Campus Notice" },
                            subtitle = n.body.take(65) + if (n.body.length > 65) "..." else "",
                            timeTag = formatTimeAgo(n.createdAt?.toDate()?.time ?: System.currentTimeMillis()),
                            bgDrawableRes = if (isUrgent) R.drawable.bg_alert_banner_orange else R.drawable.bg_alert_banner_purple,
                            iconRes = R.drawable.ic_notice,
                            onClick = {
                                (activity as? MainActivity)?.openChildDestination(R.id.nav_home, R.id.nav_notices)
                            },
                            onDismiss = {
                                dismissTopAlert(n.id)
                            }
                        )
                    )
                }
            } catch (_: Exception) {}

            // 4. Real urgent blood requests from Firestore (Red banner)
            bloodRequestRepo.getActiveRequests(3) { bloodRequests: List<BloodRequest> ->
                if (_binding == null) return@getActiveRequests
                for (req in bloodRequests) {
                    if (dismissedAlertIds.contains(req.id)) continue
                    val bagsText = if (req.bagsNeeded > 1) "${req.bagsNeeded} bags" else "1 bag"
                    val subText = "${req.hospitalName} · $bagsText · ${req.urgencyNote.ifBlank { "Contact: " + req.contactPhone }}"
                    alerts.add(
                        TopAlertItem(
                            id = req.id,
                            type = TopAlertType.BLOOD,
                            title = "Urgent ${req.bloodGroup} Blood Needed",
                            subtitle = subText,
                            timeTag = formatTimeAgo(req.timestamp?.toDate()?.time ?: System.currentTimeMillis()),
                            bgDrawableRes = R.drawable.bg_alert_banner_red,
                            iconRes = R.drawable.ic_blood_drop,
                            onClick = {
                                val c = context ?: return@TopAlertItem
                                val intent = Intent(c, BloodDonorDirectoryActivity::class.java).apply {
                                    putExtra("filter_blood_group", req.bloodGroup)
                                    putExtra("open_requests_tab", true)
                                }
                                startActivity(intent)
                            },
                            onDismiss = {
                                dismissTopAlert(req.id)
                            }
                        )
                    )
                }

                // 5. Lost & Found items (Blue banner - within 15 days, excluding deleted)
                AppPreferences(ctx).getDeletedLostFoundIds().forEach { id ->
                    LostFoundRepository.markDeletedLocally(id)
                }
                lostFoundRepo.getRecentActiveItems(3) { lfItems ->
                    if (_binding == null) return@getRecentActiveItems
                    for (item in lfItems) {
                        if (dismissedAlertIds.contains(item.id)) continue
                        val prefix = if (item.type.equals("found", ignoreCase = true)) "Found: " else "Lost: "
                        val titleText = if (item.title.startsWith("Found", ignoreCase = true) || item.title.startsWith("Lost", ignoreCase = true)) {
                            item.title
                        } else {
                            prefix + item.title
                        }
                        val subtitleText = if (item.location.isNotBlank()) item.location else item.description.take(50)
                        alerts.add(
                            TopAlertItem(
                                id = item.id,
                                type = TopAlertType.LOST_FOUND,
                                title = titleText,
                                subtitle = subtitleText,
                                timeTag = "Lost & Found",
                                bgDrawableRes = R.drawable.bg_alert_banner_blue,
                                iconRes = R.drawable.ic_location_pin,
                                onClick = {
                                    val c = context ?: return@TopAlertItem
                                    startActivity(Intent(c, LostFoundActivity::class.java))
                                },
                                onDismiss = {
                                    dismissTopAlert(item.id)
                                }
                            )
                        )
                    }

                    updateTopAlertsList(alerts)
                }
            }
        }
    }

    private fun dismissTopAlert(id: String) {
        dismissedAlertIds.add(id)
        val idx = topAlerts.indexOfFirst { it.id == id }
        if (idx != -1) {
            topAlerts.removeAt(idx)
            topAlertAdapter?.updateList(topAlerts.toList())
            setupTopAlertDots(topAlerts.size)
            if (topAlerts.isEmpty()) {
                binding.layoutTopAlertContainer.visibility = View.GONE
                topAlertHandler.removeCallbacks(topAlertRunnable)
            }
        }
    }

    private fun updateTopAlertsList(newList: List<TopAlertItem>) {
        if (_binding == null) return
        topAlerts.clear()
        topAlerts.addAll(newList)
        if (topAlerts.isEmpty()) {
            binding.layoutTopAlertContainer.visibility = View.GONE
            topAlertHandler.removeCallbacks(topAlertRunnable)
        } else {
            binding.layoutTopAlertContainer.visibility = View.VISIBLE
            topAlertAdapter?.updateList(topAlerts.toList())
            if (topAlerts.size > 1) {
                val initialPos = (1000 / 2) * topAlerts.size
                binding.vpTopAlerts.setCurrentItem(initialPos, false)
            }
            setupTopAlertDots(topAlerts.size)
        }
    }

    private fun formatTimeAgo(timeMillis: Long): String {
        val diff = System.currentTimeMillis() - timeMillis
        val minutes = diff / (60 * 1000)
        val hours = diff / (60 * 60 * 1000)
        val days = diff / (24 * 60 * 60 * 1000)
        return when {
            minutes < 5 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days == 1L -> "Yesterday"
            else -> "${days}d ago"
        }
    }

    private fun parseTimeToSeconds(timeStr: String): Int {
        return DateHelper.parseTimeToSeconds(timeStr)
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) {
            if (heroSlides.size > 1 && !isHeroManuallyPaused) {
                restartHeroTimer()
            }
            if (topAlerts.size > 1 && !isTopAlertManuallyPaused) {
                restartTopAlertTimer()
            }
            if (binding.flipperNextEvent.childCount > 1) {
                deadlineFlipperHandler.removeCallbacks(deadlineFlipperRunnable)
                deadlineFlipperHandler.postDelayed(deadlineFlipperRunnable, 4500)
            }
            if (binding.flipperHoliday.childCount > 1) {
                holidayFlipperHandler.removeCallbacks(holidayFlipperRunnable)
                holidayFlipperHandler.postDelayed(holidayFlipperRunnable, 5000)
            }
            setupNotificationBadgeListener()
        }
    }

    override fun onPause() {
        super.onPause()
        (activity as? MainActivity)?.setMainPageSwipeEnabled(true)
        heroScrollHandler.removeCallbacks(heroScrollRunnable)
        topAlertHandler.removeCallbacks(topAlertRunnable)
        deadlineFlipperHandler.removeCallbacks(deadlineFlipperRunnable)
        holidayFlipperHandler.removeCallbacks(holidayFlipperRunnable)
    }

    private fun setupHomeCampusFeed(uid: String, name: String) {
        val ctx = context ?: return
        val feedRepo = CampusFeedRepository()
        val prefs = AppPreferences(ctx)
        val userDept = prefs.getUserDepartment()
        val userBatch = prefs.getUserBatch()

        if (homeCampusFeedAdapter == null) {
            homeCampusFeedAdapter = CampusFeedAdapter(
                currentUserId = uid,
                currentUserName = name,
                onLoveClick = { post, willLove ->
                    feedRepo.toggleLove(post.id, uid, willLove)
                },
                onPollVote = { post, option ->
                    feedRepo.votePoll(post.id, uid, option)
                },
                onPetitionSign = { post ->
                    feedRepo.signPetition(post.id, uid)
                },
                onCommentClick = { _ ->
                    if (isAdminUser) {
                        startActivity(Intent(ctx, CampusFeedActivity::class.java))
                    } else {
                        startActivity(Intent(ctx, CampusFeedFeaturesActivity::class.java))
                    }
                },
                onShareClick = { post ->
                    feedRepo.recordShare(post.id)
                    val uniqueLink = "https://gstu-go.web.app/feed?id=${post.id}"
                    val shareText = "🗣️ GSTU CampusFeed — ${post.authorName}:\n\"${post.content}\"\n\n🔗 Open on GSTU Go:\n$uniqueLink"
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, shareText)
                        type = "text/plain"
                    }
                    startActivity(Intent.createChooser(sendIntent, "Share Campus Post"))
                },
                onReportClick = { post ->
                    feedRepo.reportPost(post.id, uid, "Admin review")
                    Toast.makeText(ctx, "Post reported for moderation.", Toast.LENGTH_SHORT).show()
                }
            )
            binding.rvHomeCampusFeed.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(ctx)
            binding.rvHomeCampusFeed.adapter = homeCampusFeedAdapter
        }

        homeFeedListener?.remove()
        homeFeedListener = feedRepo.getForYouQuery().limit(10)
            .addSnapshotListener { snapshot, error ->
                if (_binding == null) return@addSnapshotListener
                if (error != null) {
                    android.util.Log.w("HomeFragment", "CampusFeed hot query error: ${error.message}, falling back to simple query")
                    feedRepo.getSimpleFeedQuery().limit(10).get().addOnSuccessListener { fbSnap ->
                        if (_binding == null) return@addOnSuccessListener
                        val fallbackPosts = fbSnap.documents.mapNotNull { doc ->
                            doc.toObject(CampusFeedPost::class.java)?.copy(id = doc.id)
                        }
                        val sorted = feedRepo.sortPosts(fallbackPosts, CampusFeedRepository.TAB_FOR_YOU, userDept, userBatch, uid)
                        homeCampusFeedAdapter?.submitList(sorted.take(3))
                        binding.layoutHomeFeedEmpty.isVisible = sorted.isEmpty()
                    }
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val posts = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(CampusFeedPost::class.java)?.copy(id = doc.id)
                    }
                    val sorted = feedRepo.sortPosts(posts, CampusFeedRepository.TAB_FOR_YOU, userDept, userBatch, uid)
                    homeCampusFeedAdapter?.submitList(sorted.take(3))
                    binding.layoutHomeFeedEmpty.isVisible = sorted.isEmpty()
                }
            }
    }

    override fun onDestroyView() {
        calendarExceptionsListener?.remove()
        calendarExceptionsListener = null
        homeFeedListener?.remove()
        homeFeedListener = null
        notificationListener?.remove()
        notificationListener = null
        countdownListener?.remove()
        countdownListener = null
        (activity as? MainActivity)?.setMainPageSwipeEnabled(true)
        heroScrollHandler.removeCallbacks(heroScrollRunnable)
        topAlertHandler.removeCallbacks(topAlertRunnable)
        deadlineFlipperHandler.removeCallbacks(deadlineFlipperRunnable)
        holidayFlipperHandler.removeCallbacks(holidayFlipperRunnable)
        super.onDestroyView()
        _binding = null
    }
}
