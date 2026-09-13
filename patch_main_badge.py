with open("app/src/main/java/com/shuaib/classmate/activities/MainActivity.kt", "r") as f:
    content = f.read()

old_badge = """    private fun observeChatUnreadBadge() {
        // AI Chat tab does not require unread badges from old WebSocket rooms
    }"""
new_badge = """    private fun observeChatUnreadBadge() {
        lifecycleScope.launch {
            com.shuaib.classmate.chat.ChatRepository.rooms.collect { rooms ->
                val unread = com.shuaib.classmate.chat.ChatUnreadManager.getTotalUnread(rooms)
                val badge = binding.bottomNav.getOrCreateBadge(R.id.nav_chat)
                if (unread > 0) {
                    badge.isVisible = true
                    badge.number = unread
                } else {
                    badge.isVisible = false
                }
            }
        }
    }"""

content = content.replace(old_badge, new_badge)

with open("app/src/main/java/com/shuaib/classmate/activities/MainActivity.kt", "w") as f:
    f.write(content)
