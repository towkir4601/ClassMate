package com.shuaib.classmate.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.shuaib.classmate.R
import com.shuaib.classmate.activities.BloodDonorDirectoryActivity
import com.shuaib.classmate.activities.MainActivity
import com.shuaib.classmate.databinding.FragmentCommunityBinding
import com.shuaib.classmate.utils.applyCardClickAnimation
import com.shuaib.classmate.utils.launchActivityWithCardAnimation

class CommunityFragment : Fragment() {

    private var _binding: FragmentCommunityBinding? = null
    private val binding get() = _binding!!

    private val firestore = FirebaseFirestore.getInstance()
    private var pollListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCommunityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupGrid()
        loadCommunityData()

        binding.btnViewDonorDirectory.setOnClickListener {
            val ctx = context ?: return@setOnClickListener
            startActivity(Intent(ctx, BloodDonorDirectoryActivity::class.java))
        }

        binding.cardEmergencyRequest.setOnClickListener {
            val ctx = context ?: return@setOnClickListener
            startActivity(Intent(ctx, BloodDonorDirectoryActivity::class.java))
        }

        binding.cardRecognition.setOnClickListener {
            (activity as? MainActivity)?.openChildDestination(R.id.nav_community, R.id.nav_notices)
        }

        binding.swipeRefresh.setOnRefreshListener {
            loadCommunityData()
            viewLifecycleOwner.lifecycleScope.launch {
                kotlinx.coroutines.delay(800)
                _binding?.swipeRefresh?.isRefreshing = false
            }
        }
    }

    private fun setupGrid() {
        val mainActivity = activity as? MainActivity

        binding.cardPollsEvents.setOnClickListener { view ->
            view.applyCardClickAnimation {
                mainActivity?.openChildDestination(R.id.nav_community, R.id.nav_polls)
            }
        }

        binding.cardGroupChat.setOnClickListener { view ->
            view.applyCardClickAnimation {
                val bundle = Bundle().apply {
                    putInt("initialTab", 1)
                }
                mainActivity?.openChildDestination(R.id.nav_community, R.id.nav_chat, bundle)
            }
        }

        binding.cardNoticeDiscussions.setOnClickListener { view ->
            view.applyCardClickAnimation {
                mainActivity?.openChildDestination(R.id.nav_community, R.id.nav_notices)
            }
        }

        binding.cardBloodBank.setOnClickListener { view ->
            val ctx = context ?: return@setOnClickListener
            view.launchActivityWithCardAnimation(Intent(ctx, BloodDonorDirectoryActivity::class.java))
        }
    }

    private fun loadCommunityData() {
        pollListener?.remove()
        pollListener = firestore.collection("polls")
            .whereEqualTo("isActive", true)
            .addSnapshotListener { snapshot, _ ->
                if (_binding == null) return@addSnapshotListener
                binding.swipeRefresh.isRefreshing = false
                val count = snapshot?.size() ?: 0
                if (count > 0) {
                    binding.tvPollsBadge.text = "$count ACTIVE"
                } else {
                    binding.tvPollsBadge.text = "POLLS"
                }
            }

        viewLifecycleOwner.lifecycleScope.launch {
            com.shuaib.classmate.chat.ChatRepository.rooms.collect { rooms ->
                if (_binding == null) return@collect
                val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
                val unread = com.shuaib.classmate.chat.ChatUnreadManager.getTotalUnread(rooms, currentUid)
                val ctx = context ?: return@collect
                if (unread > 0) {
                    binding.tvChatBadge.text = "$unread UNREAD"
                    binding.tvChatBadge.setBackgroundResource(R.drawable.bg_badge_accent_pill)
                    binding.tvChatBadge.setTextColor(androidx.core.content.ContextCompat.getColor(ctx, R.color.cm_accent))
                } else {
                    binding.tvChatBadge.text = "ONLINE"
                    binding.tvChatBadge.setBackgroundResource(R.drawable.bg_badge_green)
                    binding.tvChatBadge.setTextColor(androidx.core.content.ContextCompat.getColor(ctx, R.color.cm_success))
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        pollListener?.remove()
        _binding = null
    }
}
