import re

with open('app/src/main/java/com/shuaib/classmate/activities/UserManagementActivity.kt', 'r') as f:
    content = f.read()

bad = """                pendingAdapter.notifyDataSetChanged()
                approvedAdapter.notifyDataSetChanged()
                
                binding.tvPendingTitle.visibility = if (pendingList.isNotEmpty()) android.view.View.VISIBLE else android.view.View.GONE
                binding.rvPendingUsers.visibility = if (pendingList.isNotEmpty()) android.view.View.VISIBLE else android.view.View.GONE
                binding.tvApprovedTitle.visibility = if (approvedList.isNotEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            }
    }"""
good = """                
                updateUIVisibility()
            }
    }

    private fun updateUIVisibility() {
        val mode = intent.getStringExtra("MODE") ?: "BOTH"
        val filterBatch = binding.dropdownBatchFilter.text.toString()
        
        val filteredPending = if (filterBatch.isNotBlank() && filterBatch != "All Batches") {
            pendingList.filter { it.batch == filterBatch }
        } else pendingList
        
        val filteredApproved = if (filterBatch.isNotBlank() && filterBatch != "All Batches") {
            approvedList.filter { it.batch == filterBatch }
        } else approvedList
        
        pendingAdapter.updateList(filteredPending)
        approvedAdapter.updateList(filteredApproved)

        if (mode == "PENDING" || mode == "BOTH") {
            binding.tvPendingTitle.visibility = if (filteredPending.isNotEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            binding.rvPendingUsers.visibility = if (filteredPending.isNotEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        } else {
            binding.tvPendingTitle.visibility = android.view.View.GONE
            binding.rvPendingUsers.visibility = android.view.View.GONE
        }

        if (mode == "APPROVED" || mode == "BOTH") {
            binding.tvApprovedTitle.visibility = if (filteredApproved.isNotEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            binding.rvApprovedUsers.visibility = if (filteredApproved.isNotEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        } else {
            binding.tvApprovedTitle.visibility = android.view.View.GONE
            binding.rvApprovedUsers.visibility = android.view.View.GONE
        }
    }"""
content = content.replace(bad, good)

# also setup the dropdown
bad_setup = """        firestore = FirebaseFirestore.getInstance()
        setupRecyclerView()
        fetchCurrentUser()"""
good_setup = """        firestore = FirebaseFirestore.getInstance()
        setupRecyclerView()
        setupBatchFilter()
        fetchCurrentUser()"""
content = content.replace(bad_setup, good_setup)

good_setup_func = """
    private fun setupBatchFilter() {
        val mode = intent.getStringExtra("MODE") ?: "BOTH"
        if (mode == "PENDING") {
            binding.tilBatchFilter.visibility = android.view.View.GONE
            return
        }
        val batches = mutableListOf("All Batches")
        for (i in 64 downTo 10) batches.add(i.toString())
        val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, batches)
        binding.dropdownBatchFilter.setAdapter(adapter)
        binding.dropdownBatchFilter.setText("All Batches", false)
        binding.dropdownBatchFilter.setOnItemClickListener { _, _, _, _ ->
            updateUIVisibility()
        }
    }
"""
content = content.replace("    private fun setupRecyclerView() {", good_setup_func + "\n    private fun setupRecyclerView() {")

with open('app/src/main/java/com/shuaib/classmate/activities/UserManagementActivity.kt', 'w') as f:
    f.write(content)
