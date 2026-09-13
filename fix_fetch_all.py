import re

with open('app/src/main/java/com/shuaib/classmate/activities/UserManagementActivity.kt', 'r') as f:
    content = f.read()

pattern = r'private fun fetchAllUsers\(\) \{.*?\n    \}\n'
replacement = """private fun fetchAllUsers() {
        binding.progressBar.visibility = View.VISIBLE
        firestore.collection("users")
            .addSnapshotListener { snapshot, e ->
                binding.progressBar.visibility = View.GONE
                if (e != null) {
                    Log.w("UserManagement", "Listen failed.", e)
                    return@addSnapshotListener
                }
                pendingList.clear()
                approvedList.clear()
                snapshot?.documents?.forEach { doc ->
                    val user = parseUserSafely(doc)
                    if (user != null) {
                        if (user.approved) {
                            approvedList.add(user)
                        } else {
                            pendingList.add(user)
                        }
                    }
                }
                
                // Sort both lists, newest first
                pendingList.sortByDescending { it.createdAt?.seconds ?: 0L }
                approvedList.sortByDescending { it.createdAt?.seconds ?: 0L }
                
                pendingAdapter.notifyDataSetChanged()
                approvedAdapter.notifyDataSetChanged()
                
                binding.tvPendingTitle.visibility = if (pendingList.isNotEmpty()) android.view.View.VISIBLE else android.view.View.GONE
                binding.rvPendingUsers.visibility = if (pendingList.isNotEmpty()) android.view.View.VISIBLE else android.view.View.GONE
                binding.tvApprovedTitle.visibility = if (approvedList.isNotEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            }
    }
"""

content = re.sub(pattern, replacement, content, flags=re.DOTALL)

with open('app/src/main/java/com/shuaib/classmate/activities/UserManagementActivity.kt', 'w') as f:
    f.write(content)
