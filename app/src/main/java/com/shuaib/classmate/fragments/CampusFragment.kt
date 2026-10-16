package com.shuaib.classmate.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.shuaib.classmate.R
import com.shuaib.classmate.activities.AcademicCalendarActivity
import com.shuaib.classmate.activities.BusScheduleManagementActivity
import com.shuaib.classmate.activities.ImportantContactsActivity
import com.shuaib.classmate.activities.MainActivity
import com.shuaib.classmate.databinding.FragmentCampusBinding
import com.shuaib.classmate.models.BusSchedule

class CampusFragment : Fragment() {

    private var _binding: FragmentCampusBinding? = null
    private val binding get() = _binding!!

    private val firestore = FirebaseFirestore.getInstance()
    private var busListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCampusBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupServicesGrid()
        loadShuttles()

        binding.btnManageShuttles.setOnClickListener {
            val ctx = context ?: return@setOnClickListener
            startActivity(Intent(ctx, BusScheduleManagementActivity::class.java))
        }

        binding.swipeRefresh.setOnRefreshListener {
            loadShuttles()
            viewLifecycleOwner.lifecycleScope.launch {
                kotlinx.coroutines.delay(800)
                _binding?.swipeRefresh?.isRefreshing = false
            }
        }
    }

    private fun setupServicesGrid() {
        val mainActivity = activity as? MainActivity

        binding.cardUniversityNotices.setOnClickListener {
            mainActivity?.openChildDestination(R.id.nav_campus, R.id.nav_notices)
        }

        binding.cardImportantContacts.setOnClickListener {
            val ctx = context ?: return@setOnClickListener
            startActivity(Intent(ctx, ImportantContactsActivity::class.java))
        }

        binding.cardCampusTransport.setOnClickListener {
            // Smooth scroll down to shuttle section
        }

        binding.cardCalendarBreak.setOnClickListener {
            val ctx = context ?: return@setOnClickListener
            startActivity(Intent(ctx, AcademicCalendarActivity::class.java))
        }
    }

    private fun loadShuttles() {
        busListener?.remove()
        busListener = firestore.collection("bus_schedules")
            .addSnapshotListener { snapshot, error ->
                if (_binding == null) return@addSnapshotListener
                binding.swipeRefresh.isRefreshing = false

                if (error != null || snapshot == null) {
                    renderShuttles(emptyList())
                    return@addSnapshotListener
                }

                val buses = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(BusSchedule::class.java)?.copy(id = doc.id)
                }

                renderShuttles(buses)
            }
    }

    private fun renderShuttles(buses: List<BusSchedule>) {
        if (_binding == null) return
        val ctx = context ?: return
        binding.containerShuttles.removeAllViews()

        if (buses.isEmpty()) {
            binding.emptyStateShuttles.visibility = View.VISIBLE
            binding.containerShuttles.visibility = View.GONE
            return
        }

        binding.emptyStateShuttles.visibility = View.GONE
        binding.containerShuttles.visibility = View.VISIBLE

        val inflater = LayoutInflater.from(ctx)
        for (bus in buses) {
            val card = inflater.inflate(R.layout.item_campus_shuttle, binding.containerShuttles, false)
            val tvName = card.findViewById<TextView>(R.id.tvShuttleName)
            val tvBadge = card.findViewById<TextView>(R.id.tvShuttleBadge)
            val tvRoute = card.findViewById<TextView>(R.id.tvShuttleRoute)
            val tvDeparture = card.findViewById<TextView>(R.id.tvShuttleDeparture)
            val btnView = card.findViewById<TextView>(R.id.btnViewRouteMap)

            tvName.text = bus.busName.ifBlank { "Campus Shuttle" }
            tvRoute.text = bus.route.ifBlank { "Campus ⇄ City Shuttle" }

            val depTime = bus.time.ifBlank { "08:30 AM" }
            tvDeparture.text = "🕒 Departing at $depTime"

            val isStaff = bus.busName.contains("teacher", true) || bus.busName.contains("staff", true)
            when {
                isStaff -> {
                    tvBadge.text = "RESERVED"
                    tvBadge.setBackgroundResource(R.drawable.bg_badge_purple)
                    tvBadge.setTextColor(ContextCompat.getColor(ctx, R.color.cm_warning))
                }
                bus.departureFrom.equals("Campus", true) -> {
                    tvBadge.text = "FROM CAMPUS"
                    tvBadge.setBackgroundResource(R.drawable.bg_badge_green)
                    tvBadge.setTextColor(ContextCompat.getColor(ctx, R.color.cm_success))
                }
                else -> {
                    tvBadge.text = "TO CAMPUS"
                    tvBadge.setBackgroundResource(R.drawable.bg_badge_blue)
                    tvBadge.setTextColor(ContextCompat.getColor(ctx, R.color.cm_primary))
                }
            }

            btnView.setOnClickListener {
                val c = context ?: return@setOnClickListener
                startActivity(Intent(c, BusScheduleManagementActivity::class.java))
            }

            binding.containerShuttles.addView(card)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        busListener?.remove()
        _binding = null
    }
}
