package com.shuaib.classmate.activities

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.shuaib.classmate.R
import com.shuaib.classmate.databinding.ActivityCoverPageBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CoverPageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCoverPageBinding
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    // Mode: "assignment" vs "lab_report" (Pic 2, 3, 4, 5)
    private var currentMode = "assignment"

    // View state: true = preview mode, false = edit form mode
    private var isPreviewMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCoverPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        loadUserProfile()

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

        // Mode switch tabs (Pic 2, 3, 4, 5)
        binding.tabAssignment.setOnClickListener { selectMode("assignment") }
        binding.tabLabReport.setOnClickListener { selectMode("lab_report") }

        // Default Date formatted like Image 1: "Wednesday, Oct 14, 2026"
        val defaultDateStr = SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.US).format(Calendar.getInstance().time)
        binding.etSubmissionDate.setText(defaultDateStr)
        binding.tvPrevSubmissionDate.text = "Submission Date: $defaultDateStr"

        binding.etSubmissionDate.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                val pickedCal = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
                val formatted = SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.US).format(pickedCal.time)
                binding.etSubmissionDate.setText(formatted)
                updateLivePreview()
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        // Group Submission Switch (Image 1)
        binding.switchGroupSubmission.setOnCheckedChangeListener { _, isChecked ->
            binding.layoutGroupMembers.visibility = if (isChecked) View.VISIBLE else View.GONE
            updateLivePreview()
        }

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateLivePreview()
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        binding.etCourseCode.addTextChangedListener(watcher)
        binding.etCourseTitle.addTextChangedListener(watcher)
        binding.etAssignmentTopic.addTextChangedListener(watcher)
        binding.etStudentName.addTextChangedListener(watcher)
        binding.etStudentId.addTextChangedListener(watcher)
        binding.etStudentDept.addTextChangedListener(watcher)
        binding.etStudentYear.addTextChangedListener(watcher)
        binding.etStudentSemester.addTextChangedListener(watcher)
        binding.etStudentSession.addTextChangedListener(watcher)
        binding.etTeacherName.addTextChangedListener(watcher)
        binding.etTeacherDesignation.addTextChangedListener(watcher)
        binding.etTeacherDept.addTextChangedListener(watcher)
        binding.etSubmissionDate.addTextChangedListener(watcher)
        binding.etMember2.addTextChangedListener(watcher)
        binding.etMember3.addTextChangedListener(watcher)
        binding.etMember4.addTextChangedListener(watcher)

        // Toggle button on top bar (Eye vs Edit icon)
        binding.btnToggleMode.setOnClickListener {
            if (isPreviewMode) {
                showEditMode()
            } else {
                showPreviewMode()
            }
        }

        // Form Bottom Action Buttons (Image 1)
        binding.btnLivePreview.setOnClickListener {
            showPreviewMode()
        }

        binding.btnDownloadA4Pdf.setOnClickListener {
            downloadA4Pdf()
        }

        // Preview Mode Bottom Action Buttons (Pictures 3 & 4)
        binding.btnBottomEdit.setOnClickListener {
            showEditMode()
        }

        binding.btnBottomDownloadPdf.setOnClickListener {
            downloadA4Pdf()
        }

        binding.btnBottomShare.setOnClickListener {
            shareCoverPageDetails()
        }

        updateLivePreview()
    }

    private fun selectMode(mode: String) {
        currentMode = mode
        val colorSelected = Color.parseColor("#6366F1")
        val textMuted = Color.parseColor("#94A3B8")

        if (mode == "assignment") {
            binding.tabAssignment.setBackgroundResource(R.drawable.bg_badge_accent_pill)
            binding.tabAssignment.backgroundTintList = android.content.res.ColorStateList.valueOf(colorSelected)
            binding.tvTabAssignment.setTextColor(Color.WHITE)

            binding.tabLabReport.setBackgroundColor(Color.TRANSPARENT)
            binding.tvTabLabReport.setTextColor(textMuted)

            binding.tvLabelTopic.text = "Assignment Title / Topic"
            binding.tvPreviewTypeHeader.text = "ASSIGNMENT ON"
        } else {
            binding.tabLabReport.setBackgroundResource(R.drawable.bg_badge_accent_pill)
            binding.tabLabReport.backgroundTintList = android.content.res.ColorStateList.valueOf(colorSelected)
            binding.tvTabLabReport.setTextColor(Color.WHITE)

            binding.tabAssignment.setBackgroundColor(Color.TRANSPARENT)
            binding.tvTabAssignment.setTextColor(textMuted)

            binding.tvLabelTopic.text = "Experiment Name / Lab Topic"
            binding.tvPreviewTypeHeader.text = "LAB REPORT ON"
        }
        updateLivePreview()
    }

    private fun showPreviewMode() {
        isPreviewMode = true
        updateLivePreview()
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

    private fun loadUserProfile() {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val name = doc.getString("fullName") ?: doc.getString("name") ?: auth.currentUser?.displayName ?: ""
                val studentId = doc.getString("studentId") ?: ""
                val dept = doc.getString("department") ?: "Computer Science & Engineering"
                val batch = doc.getString("batch") ?: ""
                val semesterStr = doc.getString("semester") ?: ""

                if (name.isNotBlank()) binding.etStudentName.setText(name)
                if (studentId.isNotBlank()) binding.etStudentId.setText(studentId)
                if (dept.isNotBlank()) {
                    binding.etStudentDept.setText(dept)
                    binding.etTeacherDept.setText("Dept. of $dept")
                }

                if (semesterStr.isNotBlank()) {
                    if (semesterStr.contains("-")) {
                        val parts = semesterStr.split("-")
                        binding.etStudentYear.setText("${parts.getOrNull(0) ?: "4"}th")
                        binding.etStudentSemester.setText("${parts.getOrNull(1) ?: "1"}st")
                    } else {
                        binding.etStudentSemester.setText(semesterStr)
                        binding.etStudentYear.setText("4th")
                    }
                } else {
                    binding.etStudentYear.setText("4th")
                    binding.etStudentSemester.setText("1st")
                }

                if (batch.isNotBlank()) {
                    binding.etStudentSession.setText(batch)
                } else {
                    binding.etStudentSession.setText("2022-23")
                }

                updateLivePreview()
            }
            .addOnFailureListener {
                binding.etStudentDept.setText("Computer Science & Engineering")
                binding.etTeacherDept.setText("Dept. of CSE")
                binding.etStudentYear.setText("4th")
                binding.etStudentSemester.setText("1st")
                binding.etStudentSession.setText("2022-23")
                updateLivePreview()
            }
    }

    private fun updateLivePreview() {
        val code = binding.etCourseCode.text.toString().trim()
        val title = binding.etCourseTitle.text.toString().trim()
        val topic = binding.etAssignmentTopic.text.toString().trim()

        val sName = binding.etStudentName.text.toString().trim()
        val sId = binding.etStudentId.text.toString().trim()
        val sYear = binding.etStudentYear.text.toString().trim()
        val sSem = binding.etStudentSemester.text.toString().trim()
        val sSession = binding.etStudentSession.text.toString().trim()
        val sDept = binding.etStudentDept.text.toString().trim()

        val tName = binding.etTeacherName.text.toString().trim()
        val tDesig = binding.etTeacherDesignation.text.toString().trim()
        val tDept = binding.etTeacherDept.text.toString().trim().ifBlank {
            if (sDept.isNotBlank()) "Dept. of $sDept" else "Dept. of CSE"
        }
        val submissionDate = binding.etSubmissionDate.text.toString().trim()

        // Course Line (Single colon: Course: <Code> - <Title> or Course: —)
        val courseFormatted = when {
            code.isNotBlank() && title.isNotBlank() -> "$code - $title"
            code.isNotBlank() -> code
            title.isNotBlank() -> title
            else -> "—"
        }
        binding.tvPrevCourseLine.text = "Course: $courseFormatted"

        // Topic Title
        if (topic.isNotBlank()) {
            binding.tvPrevAssignmentTitle.visibility = View.VISIBLE
            binding.tvPrevAssignmentTitle.text = topic
        } else {
            binding.tvPrevAssignmentTitle.visibility = View.GONE
        }

        // Mode header (Pic 3 vs Pic 4)
        binding.tvPreviewTypeHeader.text = if (currentMode == "assignment") {
            "ASSIGNMENT ON"
        } else {
            "LAB REPORT ON"
        }

        // Submitted By (Pic 3 & 4)
        if (binding.switchGroupSubmission.isChecked) {
            val m2 = binding.etMember2.text.toString().trim()
            val m3 = binding.etMember3.text.toString().trim()
            val m4 = binding.etMember4.text.toString().trim()

            val member1 = "${sName.ifBlank { "Member 1" }} (${sId.ifBlank { "ID" }})"
            binding.tvPrevStudentName.text = "1. $member1"
            binding.tvPrevStudentId.text = if (m2.isNotBlank()) "2. $m2" else ""
            binding.tvPrevStudentId.visibility = if (m2.isNotBlank()) View.VISIBLE else View.GONE

            binding.tvPrevStudentYear.text = if (m3.isNotBlank()) "3. $m3" else ""
            binding.tvPrevStudentYear.visibility = if (m3.isNotBlank()) View.VISIBLE else View.GONE

            binding.tvPrevStudentSemester.text = if (m4.isNotBlank()) "4. $m4" else ""
            binding.tvPrevStudentSemester.visibility = if (m4.isNotBlank()) View.VISIBLE else View.GONE

            binding.tvPrevStudentSession.text = "Session: ${sSession.ifBlank { "—" }}"
            binding.tvPrevStudentDept.text = "Dept.: ${sDept.ifBlank { "—" }}"
        } else {
            binding.tvPrevStudentId.visibility = View.VISIBLE
            binding.tvPrevStudentYear.visibility = View.VISIBLE
            binding.tvPrevStudentSemester.visibility = View.VISIBLE

            binding.tvPrevStudentName.text = "Name: ${sName.ifBlank { "—" }}"
            binding.tvPrevStudentId.text = "ID: ${sId.ifBlank { "—" }}"
            binding.tvPrevStudentYear.text = "Year: ${sYear.ifBlank { "—" }}"
            binding.tvPrevStudentSemester.text = "Semester: ${sSem.ifBlank { "—" }}"
            binding.tvPrevStudentSession.text = "Session: ${sSession.ifBlank { "—" }}"
            binding.tvPrevStudentDept.text = "Dept.: ${sDept.ifBlank { "—" }}"
        }

        // Submitted To (Pic 3 & 4)
        binding.tvPrevTeacherName.text = "Name: ${tName.ifBlank { "—" }}"
        binding.tvPrevTeacherDesignation.text = "Designation: ${tDesig.ifBlank { "—" }}"
        binding.tvPrevTeacherDept.text = "Dept.: $tDept"

        // Date (Pic 3 & 4)
        val defaultDateStr = SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.US).format(Calendar.getInstance().time)
        binding.tvPrevSubmissionDate.text = "Submission Date: ${submissionDate.ifBlank { defaultDateStr }}"
    }

    private fun downloadA4Pdf() {
        val code = binding.etCourseCode.text.toString().trim().ifBlank { "Course" }
        val title = binding.etCourseTitle.text.toString().trim().ifBlank { "Title" }
        val topic = binding.etAssignmentTopic.text.toString().trim()

        val sName = binding.etStudentName.text.toString().trim().ifBlank { "—" }
        val sId = binding.etStudentId.text.toString().trim().ifBlank { "—" }
        val sYear = binding.etStudentYear.text.toString().trim().ifBlank { "—" }
        val sSem = binding.etStudentSemester.text.toString().trim().ifBlank { "—" }
        val sSession = binding.etStudentSession.text.toString().trim().ifBlank { "—" }
        val sDept = binding.etStudentDept.text.toString().trim().ifBlank { "Computer Science & Engineering" }

        val tName = binding.etTeacherName.text.toString().trim().ifBlank { "—" }
        val tDesig = binding.etTeacherDesignation.text.toString().trim().ifBlank { "—" }
        val tDept = binding.etTeacherDept.text.toString().trim().ifBlank { "Dept. of $sDept" }
        val defaultDateStr = SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.US).format(Calendar.getInstance().time)
        val submissionDate = binding.etSubmissionDate.text.toString().trim().ifBlank { defaultDateStr }

        val headerType = if (currentMode == "assignment") "ASSIGNMENT ON" else "LAB REPORT ON"

        val isGroup = binding.switchGroupSubmission.isChecked
        val m2 = binding.etMember2.text.toString().trim()
        val m3 = binding.etMember3.text.toString().trim()
        val m4 = binding.etMember4.text.toString().trim()

        val courseFormatted = when {
            code.isNotBlank() && title.isNotBlank() -> "$code - $title"
            code.isNotBlank() -> code
            title.isNotBlank() -> title
            else -> "—"
        }

        val logoBase64 = getOfficialLogoBase64()
        val logoHtml = if (logoBase64.isNotBlank()) {
            """<img class="logo-img" src="data:image/png;base64,$logoBase64" alt="GSTU Official Seal" />"""
        } else {
            """<div class="logo-box">GSTU<br>SEAL</div>"""
        }

        val submittedByHtml = if (isGroup) {
            """
                <div class="col-title">Submitted By (Group)</div>
                <div class="info-line">1. <strong>$sName</strong> (ID: $sId)</div>
                ${if (m2.isNotBlank()) "<div class=\"info-line\">2. <strong>$m2</strong></div>" else ""}
                ${if (m3.isNotBlank()) "<div class=\"info-line\">3. <strong>$m3</strong></div>" else ""}
                ${if (m4.isNotBlank()) "<div class=\"info-line\">4. <strong>$m4</strong></div>" else ""}
                <div class="info-line" style="margin-top: 8px;"><strong>Session:</strong> $sSession</div>
                <div class="info-line"><strong>Dept.:</strong> $sDept</div>
            """.trimIndent()
        } else {
            """
                <div class="col-title">Submitted By</div>
                <div class="info-line"><strong>Name:</strong> $sName</div>
                <div class="info-line"><strong>ID:</strong> $sId</div>
                <div class="info-line"><strong>Year:</strong> $sYear</div>
                <div class="info-line"><strong>Semester:</strong> $sSem</div>
                <div class="info-line"><strong>Session:</strong> $sSession</div>
                <div class="info-line"><strong>Dept.:</strong> $sDept</div>
            """.trimIndent()
        }

        val htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <style>
                    @page {
                        size: A4 portrait;
                        margin: 18mm;
                    }
                    body {
                        font-family: 'Times New Roman', Times, serif;
                        margin: 0;
                        padding: 0;
                        color: #000;
                        background: #fff;
                    }
                    .frame-border {
                        border: 2px solid #000;
                        padding: 35px 25px;
                        min-height: 920px;
                        box-sizing: border-box;
                        display: flex;
                        flex-direction: column;
                        justify-content: space-between;
                        text-align: center;
                    }
                    .logo-img {
                        width: 84px;
                        height: 84px;
                        object-fit: contain;
                        margin: 0 auto 12px auto;
                        display: block;
                    }
                    .logo-box {
                        width: 72px;
                        height: 72px;
                        background: #0B7B3E;
                        border-radius: 8px;
                        margin: 0 auto 14px auto;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        color: #fff;
                        font-weight: bold;
                        font-size: 11px;
                        letter-spacing: 0.5px;
                        line-height: 1.2;
                        text-align: center;
                    }
                    .univ-header {
                        font-size: 19px;
                        font-weight: bold;
                        text-transform: uppercase;
                        letter-spacing: 1.2px;
                        margin-bottom: 4px;
                    }
                    .univ-location {
                        font-size: 15px;
                        font-weight: 600;
                        letter-spacing: 1.5px;
                        margin-bottom: 24px;
                    }
                    .topic-header {
                        font-size: 20px;
                        font-weight: bold;
                        letter-spacing: 1px;
                        margin-top: 15px;
                    }
                    .divider {
                        width: 220px;
                        height: 1px;
                        background: #000;
                        margin: 10px auto 20px auto;
                    }
                    .topic-text {
                        font-size: 16px;
                        font-weight: bold;
                        margin-bottom: 14px;
                    }
                    .course-line {
                        font-size: 16px;
                        text-align: left;
                        margin: 25px 0 35px 0;
                        font-weight: 500;
                    }
                    .columns-container {
                        display: flex;
                        justify-content: space-between;
                        text-align: left;
                        margin-top: 20px;
                        font-size: 15px;
                    }
                    .col-left {
                        width: 47%;
                    }
                    .col-divider {
                        width: 1px;
                        background: #888;
                    }
                    .col-right {
                        width: 47%;
                        padding-left: 10px;
                    }
                    .col-title {
                        font-size: 16px;
                        font-weight: bold;
                        margin-bottom: 12px;
                    }
                    .info-line {
                        margin-bottom: 6px;
                        line-height: 1.4;
                    }
                    .submission-date {
                        text-align: left;
                        font-size: 14px;
                        margin-top: 40px;
                    }
                </style>
            </head>
            <body>
                <div class="frame-border">
                    <div>
                        $logoHtml
                        <div class="univ-header">GOPALGANJ SCIENCE &amp; TECHNOLOGY UNIVERSITY</div>
                        <div class="univ-location">GOPALGANJ-8100</div>

                        <div class="topic-header">$headerType</div>
                        <div class="divider"></div>

                        ${if (topic.isNotBlank()) "<div class=\"topic-text\">$topic</div>" else ""}

                        <div class="course-line"><strong>Course:</strong> $courseFormatted</div>

                        <div class="columns-container">
                            <div class="col-left">
                                $submittedByHtml
                            </div>
                            <div class="col-divider"></div>
                            <div class="col-right">
                                <div class="col-title">Submitted To</div>
                                <div class="info-line"><strong>Name:</strong> $tName</div>
                                <div class="info-line"><strong>Designation:</strong> $tDesig</div>
                                <div class="info-line"><strong>Dept.:</strong> $tDept</div>
                                <div class="info-line"><strong>University:</strong> GSTU,</div>
                                <div class="info-line">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Gopalganj-8100</div>
                            </div>
                        </div>
                    </div>

                    <div class="submission-date">
                        <strong>Submission Date:</strong> $submissionDate
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()

        val webView = WebView(this)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val printManager = getSystemService(Context.PRINT_SERVICE) as? PrintManager
                val docName = "GSTU_Cover_${code.replace(" ", "_")}"
                val printAdapter = webView.createPrintDocumentAdapter(docName)
                val builder = PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                    .build()
                printManager?.print(docName, printAdapter, builder)
            }
        }
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "utf-8", null)
        Toast.makeText(this, "Preparing A4 PDF export...", Toast.LENGTH_SHORT).show()
    }

    private fun shareCoverPageDetails() {
        val code = binding.etCourseCode.text.toString().trim()
        val title = binding.etCourseTitle.text.toString().trim()
        val sName = binding.etStudentName.text.toString().trim()
        val sId = binding.etStudentId.text.toString().trim()
        val tName = binding.etTeacherName.text.toString().trim()
        val type = if (currentMode == "assignment") "Assignment" else "Lab Report"

        val shareText = """
            📄 GSTU Official $type Cover Page
            University: GOPALGANJ SCIENCE & TECHNOLOGY UNIVERSITY
            Course: $code - $title
            Submitted By: $sName (ID: $sId)
            Submitted To: $tName
            Created via GSTU Go App
        """.trimIndent()

        val intent = Intent(Intent.ACTION_SEND).apply {
            this.type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "GSTU $type Cover Page")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        startActivity(Intent.createChooser(intent, "Share Cover Page Details"))
    }

    private fun getOfficialLogoBase64(): String {
        return try {
            val inputStream = resources.openRawResource(R.drawable.logo_gstu_official)
            val bytes = inputStream.readBytes()
            inputStream.close()
            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            try {
                val drawable = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.logo_gstu_official)
                val bitmap = if (drawable is android.graphics.drawable.BitmapDrawable) {
                    drawable.bitmap
                } else {
                    drawable?.let {
                        val bmp = android.graphics.Bitmap.createBitmap(
                            it.intrinsicWidth.coerceAtLeast(1),
                            it.intrinsicHeight.coerceAtLeast(1),
                            android.graphics.Bitmap.Config.ARGB_8888
                        )
                        val canvas = android.graphics.Canvas(bmp)
                        it.setBounds(0, 0, canvas.width, canvas.height)
                        it.draw(canvas)
                        bmp
                    }
                }
                if (bitmap != null) {
                    val stream = java.io.ByteArrayOutputStream()
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
                    android.util.Base64.encodeToString(stream.toByteArray(), android.util.Base64.NO_WRAP)
                } else ""
            } catch (ex: Exception) {
                ""
            }
        }
    }
}
