package com.shuaib.classmate.activities

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import com.shuaib.classmate.R
import com.shuaib.classmate.databinding.ActivityCgpaCalculatorBinding
import java.util.Locale

class CgpaCalculatorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCgpaCalculatorBinding
    private val grades = listOf("A+", "A", "A-", "B+", "B", "B-", "C+", "C", "D", "F")
    private val credits = listOf("4.0", "3.0", "2.0", "1.5", "1.0", "0.75")

    private val gradePoints = mapOf(
        "A+" to 4.00,
        "A" to 3.75,
        "A-" to 3.50,
        "B+" to 3.25,
        "B" to 3.00,
        "B-" to 2.75,
        "C+" to 2.50,
        "C" to 2.25,
        "D" to 2.00,
        "F" to 0.00
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCgpaCalculatorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        setupTabs()
        setupSgpaSection()
        setupCgpaSection()

        // Add 5 default courses to get started
        for (i in 1..5) {
            addCourseRow("Course $i", if (i <= 3) "3.0" else "1.5", "A+")
        }
        recalculateSgpa()
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        binding.sectionSgpa.visibility = View.VISIBLE
                        binding.sectionCgpa.visibility = View.GONE
                        binding.tvResultLabel.text = "SEMESTER GPA (SGPA)"
                        recalculateSgpa()
                    }
                    1 -> {
                        binding.sectionSgpa.visibility = View.GONE
                        binding.sectionCgpa.visibility = View.VISIBLE
                        binding.tvResultLabel.text = "CUMULATIVE CGPA"
                        // Pre-populate current semester values if computed in SGPA tab
                        val sgpaStr = binding.tvGpaScore.text.toString()
                        val credStr = binding.tvTotalCredits.text.toString().replace("Total Credits: ", "").trim()
                        if (binding.etCurrSgpa.text.isNullOrBlank()) binding.etCurrSgpa.setText(sgpaStr)
                        if (binding.etCurrCredits.text.isNullOrBlank()) binding.etCurrCredits.setText(credStr)
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupSgpaSection() {
        binding.btnAddCourse.setOnClickListener {
            if (binding.containerCourses.childCount >= 20) {
                Toast.makeText(this, "Maximum 20 courses allowed per semester", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val count = binding.containerCourses.childCount + 1
            addCourseRow("Course $count", "3.0", "A+")
            recalculateSgpa()
        }
    }

    private fun addCourseRow(name: String, defaultCredit: String, defaultGrade: String) {
        val row = LayoutInflater.from(this).inflate(R.layout.item_cgpa_course, binding.containerCourses, false)
        val etCourseName = row.findViewById<TextInputEditText>(R.id.etCourseName)
        val dropdownCredit = row.findViewById<AutoCompleteTextView>(R.id.dropdownCredit)
        val dropdownGrade = row.findViewById<AutoCompleteTextView>(R.id.dropdownGrade)
        val btnRemove = row.findViewById<ImageButton>(R.id.btnRemoveCourse)

        etCourseName.setText(name)
        
        val creditAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, credits)
        dropdownCredit.setAdapter(creditAdapter)
        dropdownCredit.setText(defaultCredit, false)

        val gradeAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, grades)
        dropdownGrade.setAdapter(gradeAdapter)
        dropdownGrade.setText(defaultGrade, false)

        dropdownCredit.setOnItemClickListener { _, _, _, _ -> recalculateSgpa() }
        dropdownGrade.setOnItemClickListener { _, _, _, _ -> recalculateSgpa() }

        btnRemove.setOnClickListener {
            if (binding.containerCourses.childCount > 1) {
                binding.containerCourses.removeView(row)
                recalculateSgpa()
            } else {
                Toast.makeText(this, "At least one course is required", Toast.LENGTH_SHORT).show()
            }
        }

        binding.containerCourses.addView(row)
    }

    private fun recalculateSgpa() {
        var totalCredits = 0.0
        var totalPoints = 0.0

        for (i in 0 until binding.containerCourses.childCount) {
            val row = binding.containerCourses.getChildAt(i)
            val dropdownCredit = row.findViewById<AutoCompleteTextView>(R.id.dropdownCredit)
            val dropdownGrade = row.findViewById<AutoCompleteTextView>(R.id.dropdownGrade)

            val credit = dropdownCredit.text.toString().toDoubleOrNull() ?: 0.0
            val grade = dropdownGrade.text.toString().trim()
            val point = gradePoints[grade] ?: 0.0

            totalCredits += credit
            totalPoints += (credit * point)
        }

        val sgpa = if (totalCredits > 0) totalPoints / totalCredits else 0.0
        binding.tvGpaScore.text = String.format(Locale.US, "%.2f", sgpa)
        binding.tvTotalCredits.text = String.format(Locale.US, "Total Credits: %.1f", totalCredits)
        binding.tvTotalPoints.text = String.format(Locale.US, "Grade Points: %.2f", totalPoints)
    }

    private fun setupCgpaSection() {
        binding.btnCalculateCgpa.setOnClickListener {
            val prevCredits = binding.etPrevCredits.text.toString().toDoubleOrNull() ?: 0.0
            val prevCgpa = binding.etPrevCgpa.text.toString().toDoubleOrNull() ?: 0.0
            val currCredits = binding.etCurrCredits.text.toString().toDoubleOrNull() ?: 0.0
            val currSgpa = binding.etCurrSgpa.text.toString().toDoubleOrNull() ?: 0.0

            if (prevCredits < 0.0 || prevCredits > 300.0) {
                binding.etPrevCredits.error = "Previous credits must be between 0 and 300"
                return@setOnClickListener
            }
            if (prevCgpa < 0.0 || prevCgpa > 4.0) {
                binding.etPrevCgpa.error = "Previous CGPA must be between 0.00 and 4.00"
                return@setOnClickListener
            }
            if (currCredits <= 0.0 || currCredits > 50.0) {
                binding.etCurrCredits.error = "Current credits must be between 0.5 and 50"
                return@setOnClickListener
            }
            if (currSgpa < 0.0 || currSgpa > 4.0) {
                binding.etCurrSgpa.error = "Current SGPA must be between 0.00 and 4.00"
                return@setOnClickListener
            }

            val totalCredits = prevCredits + currCredits
            if (totalCredits <= 0) {
                Toast.makeText(this, "Please enter valid credits", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val totalPoints = (prevCredits * prevCgpa) + (currCredits * currSgpa)
            val newCgpa = totalPoints / totalCredits

            binding.tvGpaScore.text = String.format(Locale.US, "%.2f", newCgpa)
            binding.tvTotalCredits.text = String.format(Locale.US, "Total Credits: %.1f", totalCredits)
            binding.tvTotalPoints.text = String.format(Locale.US, "Grade Points: %.2f", totalPoints)

            Toast.makeText(this, "Cumulative CGPA calculated!", Toast.LENGTH_SHORT).show()
        }
    }
}
