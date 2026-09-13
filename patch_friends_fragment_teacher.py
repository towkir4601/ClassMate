import re

with open('app/src/main/java/com/shuaib/classmate/fragments/FriendsFragment.kt', 'r') as f:
    content = f.read()

bad1 = """    private var showingMatches = false

    override fun onCreateView("""
good1 = """    private var showingMatches = false
    private var isTeacherMode = false

    override fun onCreateView("""
content = content.replace(bad1, good1)

bad2 = """        setupRecyclerView()
        setupSearch()
        setupFilters()"""
good2 = """        isTeacherMode = arguments?.getBoolean("isTeacherMode") ?: false
        if (isTeacherMode) {
            binding.chipBatch?.visibility = android.view.View.GONE
            binding.chipMyMatches.visibility = android.view.View.GONE
            binding.tilSearch.hint = "Search name or department..."
        }
        
        setupRecyclerView()
        setupSearch()
        setupFilters()"""
content = content.replace(bad2, good2)

bad3 = """                                uid = doc.id,
                                batch = userBatch,"""
good3 = """                                uid = doc.id,
                                batch = userBatch,"""

bad4 = """                    allUsersList = snapshot.documents.mapNotNull { doc ->
                        try {
                            val userBatch = doc.getString("batch").orEmpty()
                            val userRole = doc.getString("role") ?: "student"
                            
                            User("""
good4 = """                    allUsersList = snapshot.documents.mapNotNull { doc ->
                        try {
                            val userBatch = doc.getString("batch").orEmpty()
                            val userRole = doc.getString("role") ?: "student"
                            
                            if (isTeacherMode && userRole != "teacher" && userRole != "admin" && userRole != "superadmin") {
                                // If in Teacher Mode, only show teachers (or admins/superadmins if they are considered staff).
                                // Actually, let's just show 'teacher' role.
                                if (userRole != "teacher") return@mapNotNull null
                            }
                            
                            if (!isTeacherMode && userRole == "teacher") {
                                // If in Student mode, hide teachers
                                return@mapNotNull null
                            }
                            
                            User("""
content = content.replace(bad4, good4)

with open('app/src/main/java/com/shuaib/classmate/fragments/FriendsFragment.kt', 'w') as f:
    f.write(content)
