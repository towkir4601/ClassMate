package com.shuaib.classmate.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.shuaib.classmate.databinding.FragmentChatTabsBinding

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

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            if (isTeacher) {
                tab.text = "Messages"
            } else {
                tab.text = when (position) {
                    0 -> "Ask AI"
                    1 -> "Class Group"
                    2 -> "Messages"
                    else -> ""
                }
            }
        }.attach()
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
