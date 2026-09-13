with open("app/src/main/java/com/shuaib/classmate/chat/ChatTabsFragment.kt", "r") as f:
    content = f.read()

old_adapter = """    private inner class ChatPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> com.shuaib.classmate.fragments.AiChatFragment()
                1 -> GroupChatFragment()
                2 -> DmRoomsFragment()
                else -> throw IllegalArgumentException("Invalid position")
            }
        }
    }"""
new_adapter = """    private var isTeacher = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = requireContext().getSharedPreferences("classmate_prefs", android.content.Context.MODE_PRIVATE)
        isTeacher = prefs.getString("user_role", "student") == "teacher"
    }

    private inner class ChatPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = if (isTeacher) 1 else 3

        override fun createFragment(position: Int): Fragment {
            if (isTeacher) {
                return DmRoomsFragment()
            }
            return when (position) {
                0 -> com.shuaib.classmate.fragments.AiChatFragment()
                1 -> GroupChatFragment()
                2 -> DmRoomsFragment()
                else -> throw IllegalArgumentException("Invalid position")
            }
        }
    }"""

old_mediator = """        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Ask AI"
                1 -> "Class Group"
                2 -> "Messages"
                else -> ""
            }
        }.attach()"""
new_mediator = """        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
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
        }.attach()"""

content = content.replace(old_adapter, new_adapter)
content = content.replace(old_mediator, new_mediator)

with open("app/src/main/java/com/shuaib/classmate/chat/ChatTabsFragment.kt", "w") as f:
    f.write(content)
