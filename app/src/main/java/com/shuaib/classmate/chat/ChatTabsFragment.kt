package com.shuaib.classmate.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.shuaib.classmate.R
import com.shuaib.classmate.databinding.FragmentChatTabsBinding
import kotlinx.coroutines.launch

class ChatTabsFragment : Fragment() {

    private var isTeacher = false

    private var _binding: FragmentChatTabsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatTabsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isTeacher = com.shuaib.classmate.chat.ChatRepository.isTeacher()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = ChatPagerAdapter(this)
        binding.viewPager.adapter = adapter
        
        // Disable swipe to avoid conflict with chat gestures (like swipe-to-reply)
        binding.viewPager.isUserInputEnabled = false

        binding.btnHeaderCamera.setOnClickListener {
            (requireActivity() as? com.shuaib.classmate.activities.MainActivity)?.openChildDestination(
                R.id.nav_chat,
                R.id.fragment_dm_list
            )
        }

        binding.btnHeaderSearch.setOnClickListener {
            (requireActivity() as? com.shuaib.classmate.activities.MainActivity)?.openChildDestination(
                R.id.nav_chat,
                R.id.fragment_dm_list
            )
        }

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            if (isTeacher) {
                tab.text = "Chats"
            } else {
                tab.text = when (position) {
                    0 -> "Ask AI"
                    1 -> "Class Group"
                    2 -> "Chats"
                    else -> ""
                }
            }
        }.attach()

        val initialTab = arguments?.getInt("initialTab", if (isTeacher) 0 else 1) ?: (if (isTeacher) 0 else 1)
        if (initialTab in 0 until adapter.itemCount) {
            binding.viewPager.post {
                if (_binding != null) {
                    binding.viewPager.currentItem = initialTab
                }
            }
        }

        binding.viewPager.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (!isTeacher && position == 1) {
                    ChatRepository.enterRoom(ChatRepository.groupRoomId)
                    ChatUnreadManager.markRoomSeen(ChatRepository.groupRoomId)
                    binding.tabLayout.getTabAt(1)?.removeBadge()
                }
            }
        })

        viewLifecycleOwner.lifecycleScope.launch {
            ChatRepository.rooms.collect { rooms ->
                if (_binding == null) return@collect
                val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
                if (!isTeacher) {
                    val isGroupTabActive = binding.viewPager.currentItem == 1
                    if (isGroupTabActive) {
                        ChatUnreadManager.markRoomSeen(ChatRepository.groupRoomId)
                    }
                    val groupUnread = if (isGroupTabActive) 0 else {
                        rooms.filter { it.type == "group" }
                            .count { it.lastSenderId.isNotBlank() && it.lastSenderId != uid && ChatUnreadManager.getUnreadCount(it.id, it.lastMessageTime) > 0 }
                    }
                    val groupTab = binding.tabLayout.getTabAt(1)
                    if (groupUnread > 0) {
                        val badge = groupTab?.orCreateBadge
                        badge?.isVisible = true
                        badge?.number = groupUnread
                    } else {
                        groupTab?.removeBadge()
                    }

                    val dmUnread = rooms.filter { it.type != "group" }
                        .count { it.lastSenderId.isNotBlank() && it.lastSenderId != uid && ChatUnreadManager.getUnreadCount(it.id, it.lastMessageTime) > 0 }
                    val dmTab = binding.tabLayout.getTabAt(2)
                    if (dmUnread > 0) {
                        val badge = dmTab?.orCreateBadge
                        badge?.isVisible = true
                        badge?.number = dmUnread
                    } else {
                        dmTab?.removeBadge()
                    }
                }
            }
        }
    }

    fun switchToTab(position: Int) {
        binding.viewPager.currentItem = position
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private inner class ChatPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = if (isTeacher) 1 else 3

        override fun createFragment(position: Int): Fragment {
            if (isTeacher) {
                return DmRoomsFragment()
            }
            return when (position) {
                0 -> ChatFragment().apply { arguments = Bundle().apply { putBoolean("isEmbedded", true) } }
                1 -> GroupChatFragment().apply { arguments = Bundle().apply { putBoolean("isEmbedded", true) } }
                2 -> DmRoomsFragment()
                else -> throw IllegalArgumentException("Invalid position $position")
            }
        }
    }
}
