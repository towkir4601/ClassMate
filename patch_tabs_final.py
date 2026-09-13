with open("app/src/main/java/com/shuaib/classmate/chat/ChatTabsFragment.kt", "r") as f:
    content = f.read()

# Make sure isTeacher is added
if "private var isTeacher = false" not in content:
    content = content.replace("class ChatTabsFragment : Fragment() {", "class ChatTabsFragment : Fragment() {\n\n    private var isTeacher = false")
    
if "isTeacher = com.shuaib.classmate.chat.ChatRepository.isTeacher()" not in content:
    content = content.replace("override fun onViewCreated", """override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isTeacher = com.shuaib.classmate.chat.ChatRepository.isTeacher()
    }

    override fun onViewCreated""")

old_adapter = """    private inner class ChatPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> ChatFragment().apply { arguments = Bundle().apply { putBoolean("isEmbedded", true) } }
                1 -> GroupChatFragment().apply { arguments = Bundle().apply { putBoolean("isEmbedded", true) } }
                2 -> DmRoomsFragment()
                else -> throw IllegalArgumentException("Invalid position $position")
            }
        }
    }"""
new_adapter = """    private inner class ChatPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
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
    }"""
content = content.replace(old_adapter, new_adapter)

with open("app/src/main/java/com/shuaib/classmate/chat/ChatTabsFragment.kt", "w") as f:
    f.write(content)
