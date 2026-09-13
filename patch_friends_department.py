import re

with open('app/src/main/java/com/shuaib/classmate/fragments/FriendsFragment.kt', 'r') as f:
    content = f.read()

# Modify resetFilters
bad_reset = """    private fun resetFilters() {
        selectedBloodGroup = null
        selectedDistrict = null
        selectedBatch = null
        binding.chipBlood.text = "Blood Group"
        binding.chipDistrict.text = "Home District"
        binding.chipBatch?.text = "Batch"
        binding.chipAll.isChecked = true
        filterList(binding.etSearch.text.toString())
    }"""
good_reset = """    private fun resetFilters() {
        selectedBloodGroup = null
        selectedDistrict = null
        selectedBatch = null
        binding.chipBlood.text = "Blood Group"
        binding.chipDistrict.text = if (isTeacherMode) "Department" else "Home District"
        binding.chipBatch?.text = "Batch"
        binding.chipAll.isChecked = true
        filterList(binding.etSearch.text.toString())
    }"""
content = content.replace(bad_reset, good_reset)

# Update chipDistrict text initially if Teacher Mode
bad_init = """        if (isTeacherMode) {
            binding.tvHeaderTitle.text = "Teacher Directory"
            binding.chipBatch?.visibility = android.view.View.GONE
            binding.chipMyMatches.visibility = android.view.View.GONE
            binding.tilSearch.hint = "Search name or department..."
        }"""
good_init = """        if (isTeacherMode) {
            binding.tvHeaderTitle.text = "Teacher Directory"
            binding.chipBatch?.visibility = android.view.View.GONE
            binding.chipMyMatches.visibility = android.view.View.GONE
            binding.tilSearch.hint = "Search name or department..."
            binding.chipDistrict.text = "Department"
        }"""
content = content.replace(bad_init, good_init)

# Update setupFilters chipDistrict click
bad_click = """        binding.chipDistrict.setOnClickListener {
            showingMatches = false
            showDistrictSelector()
        }"""
good_click = """        binding.chipDistrict.setOnClickListener {
            showingMatches = false
            if (isTeacherMode) showDepartmentSelector() else showDistrictSelector()
        }"""
content = content.replace(bad_click, good_click)

# Add showDepartmentSelector after showDistrictSelector
bad_show_district = """            .setNeutralButton("Clear Filter") { _, _ ->
                resetFilters()
            }
            .show()
    }

    private fun showBatchSelector() {"""
good_show_district = """            .setNeutralButton("Clear Filter") { _, _ ->
                resetFilters()
            }
            .show()
    }

    private fun showDepartmentSelector() {
        val depts = allUsersList.map { it.department.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
            
        if (depts.isEmpty()) {
            android.widget.Toast.makeText(context, "No departments found", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Department")
            .setItems(depts.toTypedArray()) { _, which ->
                val selected = depts[which]
                selectedDistrict = selected // Reuse selectedDistrict variable for department
                selectedBloodGroup = null
                selectedBatch = null
                binding.chipDistrict.text = "Dept: $selected"
                binding.chipBlood.text = "Blood Group"
                binding.chipBatch?.text = "Batch"
                binding.chipDistrict.isChecked = true
                filterList(binding.etSearch.text.toString())
            }
            .setNeutralButton("Clear Filter") { _, _ ->
                resetFilters()
            }
            .show()
    }

    private fun showBatchSelector() {"""
content = content.replace(bad_show_district, good_show_district)

# Update filter logic to check department if teacher mode
bad_filter = """            selectedDistrict?.let { district ->
                filtered = filtered.filter { it.homeDistrict.equals(district, ignoreCase = true) }
            }"""
good_filter = """            selectedDistrict?.let { district ->
                if (isTeacherMode) {
                    filtered = filtered.filter { it.department.equals(district, ignoreCase = true) }
                } else {
                    filtered = filtered.filter { it.homeDistrict.equals(district, ignoreCase = true) }
                }
            }"""
content = content.replace(bad_filter, good_filter)

with open('app/src/main/java/com/shuaib/classmate/fragments/FriendsFragment.kt', 'w') as f:
    f.write(content)
