import re

with open('app/src/main/java/com/shuaib/classmate/fragments/TimetableFragment.kt', 'r') as f:
    content = f.read()

# Add imports for ArrayAdapter and AdapterView
imports = '''import android.widget.ArrayAdapter
import android.widget.AdapterView'''
if 'import android.widget.ArrayAdapter' not in content:
    content = content.replace('import android.widget.Toast\n', 'import android.widget.Toast\n' + imports + '\n')

# Add variables
vars = '''    private var currentPeriods = emptyList<Period>()
    private var currentBatchFilter = "All"
    private var isSpinnerInitialized = false'''
content = content.replace('private var isAdmin = false\n', 'private var isAdmin = false\n' + vars + '\n')

# Modify setupReactiveTimetableCollection
old_collect = '''                    .collect { periods ->
                        if (isViewingRoutine) {
                            renderTimetable(selectedDayFlow.value, periods)
                        }
                    }'''

new_collect = '''                    .collect { periods ->
                        if (isViewingRoutine) {
                            currentPeriods = periods
                            updateBatchSpinner(periods)
                            applyBatchFilterAndRender()
                        }
                    }'''
content = content.replace(old_collect, new_collect)

# Add applyBatchFilterAndRender and updateBatchSpinner
new_funcs = '''
    private fun updateBatchSpinner(periods: List<Period>) {
        if (_binding == null) return
        val distinctBatches = periods.map { it.batch }.filter { it.isNotBlank() }.distinct().sorted()
        if (distinctBatches.isEmpty()) {
            binding.spinnerBatchFilter.isVisible = false
            return
        }
        
        binding.spinnerBatchFilter.isVisible = true
        
        val options = mutableListOf("All")
        options.addAll(distinctBatches)
        
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, options)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        
        // Prevent infinite loops by temporarily detaching listener
        binding.spinnerBatchFilter.onItemSelectedListener = null
        binding.spinnerBatchFilter.adapter = adapter
        
        // Restore selection if it exists
        val selectedIndex = options.indexOf(currentBatchFilter)
        if (selectedIndex >= 0) {
            binding.spinnerBatchFilter.setSelection(selectedIndex, false)
        } else {
            currentBatchFilter = "All"
            binding.spinnerBatchFilter.setSelection(0, false)
        }
        
        binding.spinnerBatchFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val newFilter = options[position]
                if (newFilter != currentBatchFilter) {
                    currentBatchFilter = newFilter
                    applyBatchFilterAndRender()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun applyBatchFilterAndRender() {
        if (_binding == null) return
        val filtered = if (currentBatchFilter == "All") {
            currentPeriods
        } else {
            currentPeriods.filter { it.batch.isEmpty() || it.batch == currentBatchFilter }
        }
        renderTimetable(selectedDayFlow.value, filtered)
    }
'''

content = content.replace('    private fun loadTimetable(day: String) {', new_funcs + '\n    private fun loadTimetable(day: String) {')

with open('app/src/main/java/com/shuaib/classmate/fragments/TimetableFragment.kt', 'w') as f:
    f.write(content)
print("Patched TimetableFragment")
