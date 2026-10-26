package com.shuaib.classmate.activities

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.shuaib.classmate.R
import com.shuaib.classmate.databinding.ActivityCoverPageBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

open class CoverPageGeneratorActivity : AppCompatActivity() {

    protected lateinit var binding: ActivityCoverPageBinding

    private var currentType: CoverType = CoverType.LAB_REPORT
    private var isPreviewMode: Boolean = false
    private var isPrintPending: Boolean = false
    private var isPageLoaded: Boolean = false

    private val previewHandler = Handler(Looper.getMainLooper())
    private val previewRunnable = Runnable {
        updatePreview()
    }

    companion object {
        const val EXTRA_DOCUMENT_TYPE = "EXTRA_DOCUMENT_TYPE"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCoverPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Read intent extra if passed, default to LAB_REPORT or ASSIGNMENT
        val initialTypeStr = intent.getStringExtra(EXTRA_DOCUMENT_TYPE)
        if (initialTypeStr != null) {
            currentType = try {
                CoverType.valueOf(initialTypeStr.uppercase(Locale.US))
            } catch (_: Exception) {
                CoverType.LAB_REPORT
            }
        }

        setupUI()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isPreviewMode) {
                    showEditMode()
                } else {
                    finish()
                }
            }
        })
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            if (isPreviewMode) {
                showEditMode()
            } else {
                finish()
            }
        }

        // Side-by-side feature cards selection
        binding.cardAssignment.setOnClickListener { selectType(CoverType.ASSIGNMENT) }
        binding.cardLabReport.setOnClickListener { selectType(CoverType.LAB_REPORT) }

        // Date selection: do NOT prefill, keep empty until picked
        binding.etSubmissionDate.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                val picked = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
                binding.etSubmissionDate.setText(SimpleDateFormat("dd MMMM yyyy", Locale.US).format(picked.time))
                schedulePreviewUpdate()
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        // Group submission switch
        binding.switchGroupSubmission.setOnCheckedChangeListener { _, isChecked ->
            binding.layoutGroupMembers.visibility = if (isChecked) View.VISIBLE else View.GONE
            schedulePreviewUpdate()
        }

        // Text change listeners
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                schedulePreviewUpdate()
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        listOf(
            binding.etCourseCode,
            binding.etCourseTitle,
            binding.etDocumentNumber,
            binding.etAssignmentTopic,
            binding.etStudentName,
            binding.etStudentId,
            binding.etStudentDept,
            binding.etStudentYear,
            binding.etStudentSemester,
            binding.etStudentSession,
            binding.etTeacherName,
            binding.etTeacherDesignation,
            binding.etTeacherDept,
            binding.etSubmissionDate,
            binding.etMember2,
            binding.etMember3,
            binding.etMember4
        ).forEach { it.addTextChangedListener(watcher) }

        // Mode toggle button
        binding.btnToggleMode.setOnClickListener {
            if (isPreviewMode) {
                showEditMode()
            } else {
                showPreviewMode()
            }
        }

        // Form action buttons
        binding.btnLivePreview.setOnClickListener { showPreviewMode() }
        binding.btnDownloadA4Pdf.setOnClickListener { initiatePdfPrint() }

        // Preview action buttons
        binding.btnBottomEdit.setOnClickListener { showEditMode() }
        binding.btnBottomDownloadPdf.setOnClickListener { initiatePdfPrint() }
        binding.btnBottomShare.setOnClickListener { shareDetails() }

        // Department pickers
        com.shuaib.classmate.utils.GstuDepartments.attachPicker(
            binding.etStudentDept,
            supportFragmentManager,
            "Select Student Department"
        ) { schedulePreviewUpdate() }
        binding.containerStudentDept.setOnClickListener { binding.etStudentDept.performClick() }

        com.shuaib.classmate.utils.GstuDepartments.attachPicker(
            binding.etTeacherDept,
            supportFragmentManager,
            "Select Instructor Department"
        ) { schedulePreviewUpdate() }
        binding.containerTeacherDept.setOnClickListener { binding.etTeacherDept.performClick() }

        selectType(currentType)
    }

    private fun selectType(type: CoverType) {
        currentType = type
        if (type == CoverType.ASSIGNMENT) {
            binding.cardAssignment.setBackgroundResource(R.drawable.bg_cover_card_assignment_selected)
            binding.tvBadgeAssignment.setBackgroundResource(R.drawable.bg_badge_accent_pill)
            binding.tvBadgeAssignment.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#1D4ED8"))
            binding.tvBadgeAssignment.text = "Active"
            binding.tvBadgeAssignment.setTextColor(Color.WHITE)
            binding.tvTitleAssignment.setTextColor(Color.parseColor("#1D4ED8"))

            binding.cardLabReport.setBackgroundResource(R.drawable.bg_cover_card_unselected)
            binding.tvBadgeLabReport.setBackgroundResource(R.drawable.bg_badge_accent_pill)
            binding.tvBadgeLabReport.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#E2E8F0"))
            binding.tvBadgeLabReport.text = "Select"
            binding.tvBadgeLabReport.setTextColor(Color.parseColor("#64748B"))
            binding.tvTitleLabReport.setTextColor(Color.parseColor("#020617"))

            binding.tvLabelDocNumber.text = "Assignment No."
            binding.etDocumentNumber.hint = "e.g. 01"
            binding.tvLabelTopic.text = "Assignment Title / Topic"
            binding.etAssignmentTopic.hint = "e.g. Raft Consensus"
        } else {
            binding.cardLabReport.setBackgroundResource(R.drawable.bg_cover_card_lab_selected)
            binding.tvBadgeLabReport.setBackgroundResource(R.drawable.bg_badge_accent_pill)
            binding.tvBadgeLabReport.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#059669"))
            binding.tvBadgeLabReport.text = "Active"
            binding.tvBadgeLabReport.setTextColor(Color.WHITE)
            binding.tvTitleLabReport.setTextColor(Color.parseColor("#059669"))

            binding.cardAssignment.setBackgroundResource(R.drawable.bg_cover_card_unselected)
            binding.tvBadgeAssignment.setBackgroundResource(R.drawable.bg_badge_accent_pill)
            binding.tvBadgeAssignment.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#E2E8F0"))
            binding.tvBadgeAssignment.text = "Select"
            binding.tvBadgeAssignment.setTextColor(Color.parseColor("#64748B"))
            binding.tvTitleAssignment.setTextColor(Color.parseColor("#020617"))

            binding.tvLabelDocNumber.text = "Experiment No."
            binding.etDocumentNumber.hint = "e.g. 01"
            binding.tvLabelTopic.text = "Experiment Name / Lab Topic"
            binding.etAssignmentTopic.hint = "e.g. Study of Logic Gates"
        }

        schedulePreviewUpdate()
    }

    private fun showPreviewMode() {
        isPreviewMode = true
        updatePreview()
        binding.layoutForm.visibility = View.GONE
        binding.layoutPreview.visibility = View.VISIBLE
        binding.btnToggleMode.setImageResource(R.drawable.ic_edit)
    }

    private fun showEditMode() {
        isPreviewMode = false
        binding.layoutForm.visibility = View.VISIBLE
        binding.layoutPreview.visibility = View.GONE
        binding.btnToggleMode.setImageResource(R.drawable.ic_eye_outline)
    }

    private fun schedulePreviewUpdate() {
        isPageLoaded = false
        previewHandler.removeCallbacks(previewRunnable)
        previewHandler.postDelayed(previewRunnable, 250)
    }

    private fun updatePreview() {
        val data = buildCoverPageData()
        CoverPageRenderer.loadPreview(this, binding.webViewPreview, data) {
            isPageLoaded = true
            if (isPrintPending) {
                isPrintPending = false
                val coursePart = if (data.courseCode.isNotBlank()) data.courseCode.replace(" ", "_") else "Document"
                val docName = "GSTU_${data.type.name}_$coursePart"
                CoverPageRenderer.printCoverPage(this@CoverPageGeneratorActivity, binding.webViewPreview, docName)
            }
        }
    }

    private fun buildCoverPageData(): CoverPageData {
        val code = binding.etCourseCode.text.toString().trim()
        val title = binding.etCourseTitle.text.toString().trim()
        val docNum = binding.etDocumentNumber.text.toString().trim()
        val topic = binding.etAssignmentTopic.text.toString().trim()

        val sName = binding.etStudentName.text.toString().trim()
        val sId = binding.etStudentId.text.toString().trim()
        val sDept = binding.etStudentDept.text.toString().trim()
        val sYear = binding.etStudentYear.text.toString().trim()
        val sSem = binding.etStudentSemester.text.toString().trim()
        val sSession = binding.etStudentSession.text.toString().trim()

        val tName = binding.etTeacherName.text.toString().trim()
        val tDesig = binding.etTeacherDesignation.text.toString().trim()
        val tDept = binding.etTeacherDept.text.toString().trim()
        val date = binding.etSubmissionDate.text.toString().trim()

        val isGroup = binding.switchGroupSubmission.isChecked
        val groupMembers = if (isGroup) {
            listOf(
                binding.etMember2.text.toString().trim(),
                binding.etMember3.text.toString().trim(),
                binding.etMember4.text.toString().trim()
            ).filter { it.isNotBlank() }
        } else emptyList()

        return CoverPageData(
            type = currentType,
            university = "GOPALGANJ SCIENCE & TECHNOLOGY UNIVERSITY,",
            department = sDept,
            studentDepartment = sDept,
            teacherDepartment = tDept,
            universityLocation = "GOPALGANJ-8100",
            heading = if (currentType == CoverType.ASSIGNMENT) "Assignment On" else "Lab Report",
            numberLabel = if (currentType == CoverType.ASSIGNMENT) "Assignment No." else "Experiment No.",
            documentNumber = docNum,
            titleLabel = if (currentType == CoverType.ASSIGNMENT) "Assignment Title" else "Experiment Name",
            documentTitle = topic,
            courseCode = code,
            courseTitle = title,
            studentName = sName,
            studentId = sId,
            year = sYear,
            semester = sSem,
            session = sSession,
            teacherName = tName,
            designation = tDesig,
            submissionDate = date,
            isGroupSubmission = isGroup,
            groupMembers = groupMembers,
            logoAssetPath = "University_Logo.webp"
        )
    }

    private fun initiatePdfPrint() {
        val data = buildCoverPageData()

        if (data.studentName.isBlank() && data.courseCode.isBlank()) {
            Toast.makeText(this, "Filling all fields before printing is recommended", Toast.LENGTH_SHORT).show()
        }

        Toast.makeText(this, "Preparing A4 PDF export...", Toast.LENGTH_SHORT).show()

        if (isPageLoaded) {
            val coursePart = if (data.courseCode.isNotBlank()) data.courseCode.replace(" ", "_") else "Cover"
            val docName = "GSTU_${data.type.name}_$coursePart"
            CoverPageRenderer.printCoverPage(this, binding.webViewPreview, docName)
        } else {
            isPrintPending = true
            updatePreview()
        }
    }

    private fun shareDetails() {
        val data = buildCoverPageData()
        val typeName = if (data.type == CoverType.ASSIGNMENT) "Assignment" else "Lab Report"
        val shareText = """
            📄 GSTU Official $typeName Cover Page
            University: ${data.university}
            Location: ${data.universityLocation}
            Course: ${data.courseCode.ifBlank { "—" }} - ${data.courseTitle.ifBlank { "—" }}
            Topic: ${data.documentTitle.ifBlank { "—" }}
            Submitted By: ${data.studentName.ifBlank { "—" }} (ID: ${data.studentId.ifBlank { "—" }})
            Submitted To: ${data.teacherName.ifBlank { "—" }} (${data.teacherDepartment.ifBlank { "—" }})
            Date: ${data.submissionDate.ifBlank { "—" }}
            Generated via GSTU Go App
        """.trimIndent()

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "GSTU $typeName Cover Page")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        startActivity(Intent.createChooser(intent, "Share Cover Page Details"))
    }
}
