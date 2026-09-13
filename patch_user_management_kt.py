import re

with open('app/src/main/java/com/shuaib/classmate/activities/UserManagementActivity.kt', 'r') as f:
    content = f.read()

bad = """    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        firestore = FirebaseFirestore.getInstance()
        setupRecyclerView()
        fetchCurrentUser()
    }"""
good = """    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        val mode = intent.getStringExtra("MODE") ?: "BOTH"
        if (mode == "PENDING") {
            binding.toolbar.title = "Pending Approvals"
        } else if (mode == "APPROVED") {
            binding.toolbar.title = "Manage Users"
        }

        binding.toolbar.setNavigationOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        firestore = FirebaseFirestore.getInstance()
        setupRecyclerView()
        fetchCurrentUser()
    }"""
content = content.replace(bad, good)

# In updateLists
bad_update = """    private fun updateLists() {
        if (_binding == null) return
        
        pendingAdapter.updateList(pendingList)
        approvedAdapter.updateList(approvedList)

        if (pendingList.isNotEmpty()) {
            binding.tvPendingTitle.visibility = View.VISIBLE
            binding.rvPendingUsers.visibility = View.VISIBLE
        } else {
            binding.tvPendingTitle.visibility = View.GONE
            binding.rvPendingUsers.visibility = View.GONE
        }

        if (approvedList.isNotEmpty()) {
            binding.tvApprovedTitle.visibility = View.VISIBLE
            binding.rvApprovedUsers.visibility = View.VISIBLE
        } else {
            binding.tvApprovedTitle.visibility = View.GONE
            binding.rvApprovedUsers.visibility = View.GONE
        }
    }"""
good_update = """    private fun updateLists() {
        if (_binding == null) return
        
        pendingAdapter.updateList(pendingList)
        approvedAdapter.updateList(approvedList)
        
        val mode = intent.getStringExtra("MODE") ?: "BOTH"

        if (mode == "PENDING" || mode == "BOTH") {
            if (pendingList.isNotEmpty()) {
                binding.tvPendingTitle.visibility = View.VISIBLE
                binding.rvPendingUsers.visibility = View.VISIBLE
            } else {
                binding.tvPendingTitle.visibility = View.GONE
                binding.rvPendingUsers.visibility = View.GONE
            }
        } else {
            binding.tvPendingTitle.visibility = View.GONE
            binding.rvPendingUsers.visibility = View.GONE
        }

        if (mode == "APPROVED" || mode == "BOTH") {
            if (approvedList.isNotEmpty()) {
                binding.tvApprovedTitle.visibility = View.VISIBLE
                binding.rvApprovedUsers.visibility = View.VISIBLE
            } else {
                binding.tvApprovedTitle.visibility = View.GONE
                binding.rvApprovedUsers.visibility = View.GONE
            }
        } else {
            binding.tvApprovedTitle.visibility = View.GONE
            binding.rvApprovedUsers.visibility = View.GONE
        }
    }"""
content = content.replace(bad_update, good_update)

with open('app/src/main/java/com/shuaib/classmate/activities/UserManagementActivity.kt', 'w') as f:
    f.write(content)
