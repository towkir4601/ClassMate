import re

with open('app/src/main/java/com/shuaib/classmate/fragments/NoticeFragment.kt', 'r') as f:
    content = f.read()

# Add currentUserBatch and currentUserRole
bad_vars = """    private var currentUserId = ""
    private var currentUserName = ""
    private var currentUserStudentId = ""
    private val readNoticeCache = mutableSetOf<String>()"""
good_vars = """    private var currentUserId = ""
    private var currentUserName = ""
    private var currentUserStudentId = ""
    private var currentUserBatch = ""
    private var currentUserRole = "student"
    private val readNoticeCache = mutableSetOf<String>()"""
content = content.replace(bad_vars, good_vars)

# Fetch batch and role
bad_fetch = """                    if (doc.exists()) {
                        currentUserName = doc.getString("name") ?: ""
                        currentUserStudentId = doc.getString("studentId") ?: ""
                    }"""
good_fetch = """                    if (doc.exists()) {
                        currentUserName = doc.getString("name") ?: ""
                        currentUserStudentId = doc.getString("studentId") ?: ""
                        currentUserBatch = doc.getString("batch") ?: ""
                        currentUserRole = doc.getString("role") ?: "student"
                        renderFeed() // Re-render after fetching batch
                    }"""
content = content.replace(bad_fetch, good_fetch)

# Filter in renderFeed()
# Let's find renderFeed() first
bad_render = """    private fun renderFeed() {
        if (_binding == null) return

        var filteredNotices = allNotices.filter { !it.isDeleted }"""
good_render = """    private fun renderFeed() {
        if (_binding == null) return

        var filteredNotices = allNotices.filter { !it.isDeleted }.filter { 
            it.targetBatch == "all" || it.targetBatch == currentUserBatch || currentUserRole == "superadmin" 
        }"""
content = content.replace(bad_render, good_render)

# Also filter Polls
bad_polls = """        var filteredPolls = allPolls.toList()"""
good_polls = """        var filteredPolls = allPolls.filter { 
            it.targetBatch == "all" || it.targetBatch == currentUserBatch || currentUserRole == "superadmin" 
        }"""
content = content.replace(bad_polls, good_polls)

with open('app/src/main/java/com/shuaib/classmate/fragments/NoticeFragment.kt', 'w') as f:
    f.write(content)
