import re

with open('app/src/main/java/com/shuaib/classmate/activities/AdminPanelActivity.kt', 'r') as f:
    content = f.read()

bad = """        if (user.canManageUsers()) {
            binding.cardManageUsers.visibility = View.VISIBLE
            binding.cardManageUsers.setOnClickListener {
                it.animateSpringScale(0.95f)
                startActivity(Intent(this, UserManagementActivity::class.java))
            }
        }"""
good = """        if (user.canManageUsers()) {
            binding.cardManageUsers.visibility = View.VISIBLE
            binding.cardManageUsers.setOnClickListener {
                it.animateSpringScale(0.95f)
                startActivity(Intent(this, UserManagementActivity::class.java).apply {
                    putExtra("MODE", "APPROVED")
                })
            }
            binding.cardPendingApprovals.visibility = View.VISIBLE
            binding.cardPendingApprovals.setOnClickListener {
                it.animateSpringScale(0.95f)
                startActivity(Intent(this, UserManagementActivity::class.java).apply {
                    putExtra("MODE", "PENDING")
                })
            }
        }"""
content = content.replace(bad, good)

with open('app/src/main/java/com/shuaib/classmate/activities/AdminPanelActivity.kt', 'w') as f:
    f.write(content)
