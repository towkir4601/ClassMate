package com.shuaib.classmate.ui

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.shuaib.classmate.R
import com.shuaib.classmate.activities.BloodDonorDirectoryActivity
import com.shuaib.classmate.activities.LostFoundActivity
import com.shuaib.classmate.adapters.NotificationsAdapter
import com.shuaib.classmate.databinding.BottomSheetNotificationsBinding
import com.shuaib.classmate.models.AppNotification
import com.shuaib.classmate.repositories.AppNotificationRepository
import com.shuaib.classmate.utils.UserRoleHelper

class NotificationsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetNotificationsBinding? = null
    private val binding get() = _binding!!

    private val notificationRepo = AppNotificationRepository()
    private var listener: ListenerRegistration? = null
    private lateinit var adapter: NotificationsAdapter
    private var allNotifications = listOf<AppNotification>()
    private var currentFilter = "all" // "all", "admin", "user"
    private var currentUserId = ""
    private var isAdmin = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ctx = requireContext()
        currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        val role = UserRoleHelper.getEffectiveRole(ctx)
        isAdmin = role.equals("admin", ignoreCase = true) || role.equals("superadmin", ignoreCase = true)

        adapter = NotificationsAdapter(
            notifications = emptyList(),
            currentUserId = currentUserId,
            onItemClick = { n ->
                notificationRepo.markAsRead(n.id, currentUserId)
                handleNotificationClick(n)
            }
        )

        binding.rvNotifications.layoutManager = LinearLayoutManager(ctx)
        binding.rvNotifications.adapter = adapter

        binding.btnCloseNotifications.setOnClickListener { dismiss() }

        binding.btnMarkAllRead.setOnClickListener {
            notificationRepo.markAllAsRead(allNotifications, currentUserId)
            allNotifications = allNotifications.map { it.copy(isRead = true) }
            applyFilter()
        }

        if (isAdmin) {
            binding.layoutAdminFilterTabs.visibility = View.VISIBLE
            setupFilterTabs()
        } else {
            binding.layoutAdminFilterTabs.visibility = View.GONE
        }

        loadNotifications()
    }

    private fun setupFilterTabs() {
        binding.tabAllNotifs.setOnClickListener { setTab("all") }
        binding.tabAdminNotifs.setOnClickListener { setTab("admin") }
        binding.tabUserNotifs.setOnClickListener { setTab("user") }
        setTab("all")
    }

    private fun setTab(tab: String) {
        currentFilter = tab
        val selectedBg = R.drawable.bg_badge_accent_pill
        val selectedTint = ColorStateList.valueOf(Color.parseColor("#312E81"))
        val white = Color.WHITE
        val textMuted = Color.parseColor("#94A3B8")

        // Reset all
        binding.tabAllNotifs.background = null
        binding.tabAllNotifs.setTextColor(textMuted)
        binding.tabAdminNotifs.background = null
        binding.tabAdminNotifs.setTextColor(textMuted)
        binding.tabUserNotifs.background = null
        binding.tabUserNotifs.setTextColor(textMuted)

        when (tab) {
            "all" -> {
                binding.tabAllNotifs.setBackgroundResource(selectedBg)
                binding.tabAllNotifs.backgroundTintList = selectedTint
                binding.tabAllNotifs.setTextColor(white)
            }
            "admin" -> {
                binding.tabAdminNotifs.setBackgroundResource(selectedBg)
                binding.tabAdminNotifs.backgroundTintList = selectedTint
                binding.tabAdminNotifs.setTextColor(white)
            }
            "user" -> {
                binding.tabUserNotifs.setBackgroundResource(selectedBg)
                binding.tabUserNotifs.backgroundTintList = selectedTint
                binding.tabUserNotifs.setTextColor(white)
            }
        }
        applyFilter()
    }

    private fun loadNotifications() {
        binding.pbNotifications.visibility = View.VISIBLE
        listener?.remove()

        listener = notificationRepo.listenNotifications(currentUserId, isAdmin) { list ->
            if (_binding == null) return@listenNotifications
            binding.pbNotifications.visibility = View.GONE
            allNotifications = list
            applyFilter()
        }
    }

    private fun applyFilter() {
        val filtered = when (currentFilter) {
            "admin" -> allNotifications.filter { it.isAdminAlert }
            "user" -> allNotifications.filter { !it.isAdminAlert }
            else -> allNotifications
        }
        adapter.updateList(filtered)

        if (filtered.isEmpty()) {
            binding.layoutEmptyNotifications.visibility = View.VISIBLE
            binding.rvNotifications.visibility = View.GONE
        } else {
            binding.layoutEmptyNotifications.visibility = View.GONE
            binding.rvNotifications.visibility = View.VISIBLE
        }
    }

    private fun handleNotificationClick(n: AppNotification) {
        dismiss()
        val ctx = context ?: return
        when (n.postType) {
            "lost_found" -> {
                startActivity(Intent(ctx, LostFoundActivity::class.java))
            }
            "blood_request" -> {
                startActivity(Intent(ctx, BloodDonorDirectoryActivity::class.java))
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listener?.remove()
        _binding = null
    }

    companion object {
        const val TAG = "NotificationsBottomSheet"
        fun newInstance() = NotificationsBottomSheet()
    }
}
