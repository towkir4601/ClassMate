import re

with open('app/src/main/java/com/shuaib/classmate/activities/UserManagementActivity.kt', 'r') as f:
    content = f.read()

# 1. Variables
bad_vars = """    private lateinit var userAdapter: UserAdapter
    private lateinit var binding: ActivityUserManagementBinding
    private val userList = mutableListOf<User>()"""
good_vars = """    private lateinit var pendingAdapter: UserAdapter
    private lateinit var approvedAdapter: UserAdapter
    private lateinit var binding: ActivityUserManagementBinding
    private val pendingList = mutableListOf<User>()
    private val approvedList = mutableListOf<User>()"""
content = content.replace(bad_vars, good_vars)

# 2. setupRecyclerView
bad_setup = """    private fun setupRecyclerView() {
        val rootDecorView = window.decorView.findViewById<ViewGroup>(android.R.id.content)
        userAdapter = UserAdapter(userList, rootDecorView) { user ->
            startActivity(
                Intent(this, UserDetailActivity::class.java)
                    .putExtra(UserDetailActivity.EXTRA_USER_ID, user.uid)
            )
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        userAdapter.onUserLongClick = { targetUser ->
            showUserManagementOptions(targetUser)
        }

        binding.rvUsers.apply {
            layoutManager = LinearLayoutManager(this@UserManagementActivity)
            adapter = userAdapter
        }
    }"""
good_setup = """    private fun setupRecyclerView() {
        val rootDecorView = window.decorView.findViewById<ViewGroup>(android.R.id.content)
        val onUserClick = { user: User ->
            startActivity(
                Intent(this, UserDetailActivity::class.java)
                    .putExtra(UserDetailActivity.EXTRA_USER_ID, user.uid)
            )
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
        val onUserLongClick = { targetUser: User ->
            showUserManagementOptions(targetUser)
        }

        pendingAdapter = UserAdapter(pendingList, rootDecorView, onUserClick)
        pendingAdapter.onUserLongClick = onUserLongClick
        
        approvedAdapter = UserAdapter(approvedList, rootDecorView, onUserClick)
        approvedAdapter.onUserLongClick = onUserLongClick

        binding.rvPendingUsers.apply {
            layoutManager = LinearLayoutManager(this@UserManagementActivity)
            adapter = pendingAdapter
        }
        binding.rvApprovedUsers.apply {
            layoutManager = LinearLayoutManager(this@UserManagementActivity)
            adapter = approvedAdapter
        }
    }"""
content = content.replace(bad_setup, good_setup)

# 3. fetchAllUsers / snapshot listener
bad_fetch = """    private fun fetchAllUsers() {
        firestore.collection("users")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("UserManagement", "Listen failed.", e)
                    return@addSnapshotListener
                }
                userList.clear()
                snapshot?.documents?.forEach { doc ->
                    val user = parseUserSafely(doc)
                    if (user != null) {
                        userList.add(user)
                    }
                }
                userAdapter.notifyDataSetChanged()
            }
    }"""
good_fetch = """    private fun fetchAllUsers() {
        firestore.collection("users")
            .addSnapshotListener { snapshot, e ->
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
                pendingAdapter.notifyDataSetChanged()
                approvedAdapter.notifyDataSetChanged()
                
                binding.tvPendingTitle.visibility = if (pendingList.isNotEmpty()) android.view.View.VISIBLE else android.view.View.GONE
                binding.rvPendingUsers.visibility = if (pendingList.isNotEmpty()) android.view.View.VISIBLE else android.view.View.GONE
                binding.tvApprovedTitle.visibility = if (approvedList.isNotEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            }
    }"""
content = content.replace(bad_fetch, good_fetch)

with open('app/src/main/java/com/shuaib/classmate/activities/UserManagementActivity.kt', 'w') as f:
    f.write(content)
