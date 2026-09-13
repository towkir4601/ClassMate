with open("app/src/main/java/com/shuaib/classmate/chat/ChatTabsFragment.kt", "r") as f:
    content = f.read()

old_create = """    private var isTeacher = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = requireContext().getSharedPreferences("classmate_prefs", android.content.Context.MODE_PRIVATE)
        isTeacher = prefs.getString("user_role", "student") == "teacher"
    }"""
new_create = """    private var isTeacher = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isTeacher = com.shuaib.classmate.chat.ChatRepository.isTeacher()
    }"""
content = content.replace(old_create, new_create)

with open("app/src/main/java/com/shuaib/classmate/chat/ChatTabsFragment.kt", "w") as f:
    f.write(content)
