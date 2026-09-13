import re

with open('app/src/main/java/com/shuaib/classmate/activities/UserManagementActivity.kt', 'r') as f:
    content = f.read()

bad_setup = """    private fun setupBatchFilter() {
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
    }"""
good_setup = """    private fun setupBatchFilter() {
        val mode = intent.getStringExtra("MODE") ?: "BOTH"
        if (mode == "PENDING") {
            binding.tilBatchFilter.visibility = android.view.View.GONE
            return
        }
        binding.dropdownBatchFilter.setText("All Batches", false)
        binding.dropdownBatchFilter.setOnItemClickListener { _, _, _, _ ->
            updateUIVisibility()
        }
    }

    private fun updateBatchFilterDropdown() {
        val allUsers = pendingList + approvedList
        val availableBatches = allUsers.mapNotNull { it.batch.trim().takeIf { b -> b.isNotBlank() } }
            .distinct()
            .sortedByDescending { it.toIntOrNull() ?: 0 }
            
        val batches = mutableListOf("All Batches")
        batches.addAll(availableBatches)
        
        val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, batches)
        binding.dropdownBatchFilter.setAdapter(adapter)
    }"""
content = content.replace(bad_setup, good_setup)

bad_fetch = """                pendingList.sortByDescending { it.createdAt?.seconds ?: 0L }
                approvedList.sortByDescending { it.createdAt?.seconds ?: 0L }
                
                
                updateUIVisibility()"""
good_fetch = """                pendingList.sortByDescending { it.createdAt?.seconds ?: 0L }
                approvedList.sortByDescending { it.createdAt?.seconds ?: 0L }
                
                updateBatchFilterDropdown()
                updateUIVisibility()"""
content = content.replace(bad_fetch, good_fetch)

with open('app/src/main/java/com/shuaib/classmate/activities/UserManagementActivity.kt', 'w') as f:
    f.write(content)
