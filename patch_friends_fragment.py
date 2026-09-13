import re

with open('app/src/main/java/com/shuaib/classmate/fragments/FriendsFragment.kt', 'r') as f:
    content = f.read()

# 1. Remove the strict batch restriction in snapshot listener
bad_restriction = """                            if (currentUserRole != "superadmin" && currentUserBatch.isNotEmpty() && userBatch.isNotEmpty() && userBatch != currentUserBatch) {
                                return@mapNotNull null // Skip user if they are in a different batch and we are not superadmin
                            }"""
good_restriction = ""
content = content.replace(bad_restriction, good_restriction)

# 2. Add selectedBatch variable
bad_vars = """    private var selectedDistrict: String? = null
    private var showingMatches = false"""
good_vars = """    private var selectedDistrict: String? = null
    private var selectedBatch: String? = null
    private var showingMatches = false"""
content = content.replace(bad_vars, good_vars)

# 3. Add chipBatch click listener in setupFilters
bad_setup = """        binding.chipDistrict.setOnClickListener {
            showingMatches = false
            showDistrictSelector()
        }
    }"""
good_setup = """        binding.chipDistrict.setOnClickListener {
            showingMatches = false
            showDistrictSelector()
        }
        
        binding.chipBatch?.setOnClickListener {
            showingMatches = false
            showBatchSelector()
        }
    }"""
content = content.replace(bad_setup, good_setup)

# 4. Update resetFilters
bad_reset = """    private fun resetFilters() {
        selectedBloodGroup = null
        selectedDistrict = null
        binding.chipBlood.text = "Blood Group"
        binding.chipDistrict.text = "Home District"
        binding.chipAll.isChecked = true
        filterList(binding.etSearch.text.toString())
    }"""
good_reset = """    private fun resetFilters() {
        selectedBloodGroup = null
        selectedDistrict = null
        selectedBatch = null
        binding.chipBlood.text = "Blood Group"
        binding.chipDistrict.text = "Home District"
        binding.chipBatch?.text = "Batch"
        binding.chipAll.isChecked = true
        filterList(binding.etSearch.text.toString())
    }"""
content = content.replace(bad_reset, good_reset)

# 5. Clear batch when others selected (optional but matches logic)
bad_blood = """                selectedDistrict = null
                binding.chipBlood.text = "Blood: $selected"
                binding.chipDistrict.text = "Home District\""""
good_blood = """                selectedDistrict = null
                selectedBatch = null
                binding.chipBlood.text = "Blood: $selected"
                binding.chipDistrict.text = "Home District"
                binding.chipBatch?.text = "Batch\""""
content = content.replace(bad_blood, good_blood)

bad_district = """                selectedBloodGroup = null
                binding.chipDistrict.text = "Dist: $selected"
                binding.chipBlood.text = "Blood Group\""""
good_district = """                selectedBloodGroup = null
                selectedBatch = null
                binding.chipDistrict.text = "Dist: $selected"
                binding.chipBlood.text = "Blood Group"
                binding.chipBatch?.text = "Batch\""""
content = content.replace(bad_district, good_district)

# 6. Add showBatchSelector
bad_batch_dialog = """    private fun filterList(query: String) {"""
good_batch_dialog = """    private fun showBatchSelector() {
        val batches = allUsersList.map { it.batch.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
            
        if (batches.isEmpty()) {
            android.widget.Toast.makeText(context, "No batches found", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Batch")
            .setItems(batches.toTypedArray()) { _, which ->
                val selected = batches[which]
                selectedBatch = selected
                selectedBloodGroup = null
                selectedDistrict = null
                binding.chipBatch?.text = "Batch: $selected"
                binding.chipBlood.text = "Blood Group"
                binding.chipDistrict.text = "Home District"
                binding.chipBatch?.isChecked = true
                filterList(binding.etSearch.text.toString())
            }
            .setNeutralButton("Clear Filter") { _, _ ->
                resetFilters()
            }
            .show()
    }

    private fun filterList(query: String) {"""
content = content.replace(bad_batch_dialog, good_batch_dialog)

# 7. Apply batch filter in filterList
bad_filter = """            selectedDistrict?.let { district ->
                filtered = filtered.filter { it.homeDistrict.equals(district, ignoreCase = true) }
            }
            
            if (query.isNotBlank()) {"""
good_filter = """            selectedDistrict?.let { district ->
                filtered = filtered.filter { it.homeDistrict.equals(district, ignoreCase = true) }
            }
            
            selectedBatch?.let { batch ->
                filtered = filtered.filter { it.batch.equals(batch, ignoreCase = true) }
            }
            
            if (query.isNotBlank()) {"""
content = content.replace(bad_filter, good_filter)

with open('app/src/main/java/com/shuaib/classmate/fragments/FriendsFragment.kt', 'w') as f:
    f.write(content)
