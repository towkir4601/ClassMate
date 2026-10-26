package com.shuaib.classmate.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.shuaib.classmate.R
import com.shuaib.classmate.databinding.BottomSheetDepartmentPickerBinding
import com.shuaib.classmate.databinding.ItemDepartmentPickerBinding
import com.shuaib.classmate.utils.GstuDepartments

/**
 * Modern, searchable BottomSheet dialog for selecting from the 34 official GSTU academic departments.
 */
class DepartmentPickerBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetDepartmentPickerBinding? = null
    private val binding get() = _binding!!

    private var pickerTitle: String = "Select Department"
    private var selectedDepartment: String = ""
    private var onDepartmentSelected: ((String) -> Unit)? = null

    private lateinit var adapter: DepartmentPickerAdapter
    private var currentList: List<GstuDepartments.DepartmentItem> = GstuDepartments.ITEMS

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Theme_ClassMate_BottomSheetDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetDepartmentPickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Make bottom sheet expand properly
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }

        binding.tvPickerTitle.text = pickerTitle
        binding.btnClosePicker.setOnClickListener { dismiss() }

        adapter = DepartmentPickerAdapter(
            items = currentList,
            selectedDepartment = selectedDepartment,
            onItemClick = { item ->
                onDepartmentSelected?.invoke(item.name)
                dismiss()
            }
        )

        binding.rvDepartments.layoutManager = LinearLayoutManager(requireContext())
        binding.rvDepartments.adapter = adapter
        updateResultCounter(currentList.size)

        // Clear search button
        binding.btnClearSearch.setOnClickListener {
            binding.etSearchDepartment.setText("")
        }

        // Live text filter
        binding.etSearchDepartment.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString()?.trim().orEmpty()
                binding.btnClearSearch.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
                filterDepartments(query)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Custom department fallback
        binding.btnUseCustomDepartment.setOnClickListener {
            val custom = binding.etSearchDepartment.text.toString().trim()
            if (custom.isNotEmpty()) {
                onDepartmentSelected?.invoke(custom)
                dismiss()
            }
        }
    }

    private fun filterDepartments(query: String) {
        val filtered = GstuDepartments.search(query)
        currentList = filtered
        adapter.updateData(filtered)
        updateResultCounter(filtered.size)

        if (filtered.isEmpty()) {
            binding.rvDepartments.visibility = View.GONE
            binding.layoutEmptyState.visibility = View.VISIBLE
            if (query.isNotBlank()) {
                binding.btnUseCustomDepartment.visibility = View.VISIBLE
                binding.btnUseCustomDepartment.text = "Use \"$query\""
            } else {
                binding.btnUseCustomDepartment.visibility = View.GONE
            }
        } else {
            binding.rvDepartments.visibility = View.VISIBLE
            binding.layoutEmptyState.visibility = View.GONE
        }
    }

    private fun updateResultCounter(count: Int) {
        binding.tvResultCount.text = when (count) {
            34 -> "All 34 departments available"
            1 -> "1 department found"
            else -> "$count departments found"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class DepartmentPickerAdapter(
        private var items: List<GstuDepartments.DepartmentItem>,
        private val selectedDepartment: String,
        private val onItemClick: (GstuDepartments.DepartmentItem) -> Unit
    ) : RecyclerView.Adapter<DepartmentPickerAdapter.ViewHolder>() {

        inner class ViewHolder(val b: ItemDepartmentPickerBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val b = ItemDepartmentPickerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(b)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.b.tvDeptAcronym.text = item.acronym
            holder.b.tvDeptName.text = item.name
            holder.b.tvDeptFaculty.text = item.faculty

            val isSelected = selectedDepartment.equals(item.name, ignoreCase = true) ||
                    selectedDepartment.equals(item.acronym, ignoreCase = true)
            holder.b.ivSelectedCheck.visibility = if (isSelected) View.VISIBLE else View.GONE

            holder.b.root.setOnClickListener {
                onItemClick(item)
            }
        }

        override fun getItemCount(): Int = items.size

        fun updateData(newItems: List<GstuDepartments.DepartmentItem>) {
            items = newItems
            notifyDataSetChanged()
        }
    }

    companion object {
        const val TAG = "DepartmentPickerBottomSheet"

        fun show(
            fragmentManager: FragmentManager,
            title: String = "Select Department",
            selectedDepartment: String = "",
            onSelected: (String) -> Unit
        ): DepartmentPickerBottomSheet {
            val sheet = DepartmentPickerBottomSheet().apply {
                this.pickerTitle = title
                this.selectedDepartment = selectedDepartment
                this.onDepartmentSelected = onSelected
            }
            sheet.show(fragmentManager, TAG)
            return sheet
        }
    }
}
