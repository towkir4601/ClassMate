package com.shuaib.classmate.activities

import android.content.Context
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.view.LayoutInflater
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.shuaib.classmate.R
import com.shuaib.classmate.databinding.ActivityCvMakerBinding

class CvMakerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCvMakerBinding
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private var currentCgpa = "3.84"
    private var currentGraduation = "2026"
    private var currentDegree = "B.Sc. in Computer Science & Engineering"
    private var currentUniversity = "Gopalganj Science & Tech University"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCvMakerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        loadUserProfile()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { finish() }

        // Set default sample values matching Image 2 & 3
        binding.etCvName.setText("Alif Khan")
        binding.etCvEmail.setText("alif.cse@gstu.edu.bd")
        binding.etCvSkills.setText("Flutter, Dart, Node.js, Go, Docker, PostgreSQL, Git")
        binding.etCvProjects.setText("GSTU Go: Campus SuperApp; Microservices E-Commerce")

        // Education Card click to edit
        binding.cardEducation.setOnClickListener {
            showEditEducationDialog()
        }

        binding.btnGenerateResumePdf.setOnClickListener {
            generateAcademicResumePdf()
        }
    }

    private fun loadUserProfile() {
        val user = auth.currentUser ?: return
        val uid = user.uid

        if (!user.displayName.isNullOrBlank()) {
            binding.etCvName.setText(user.displayName)
        }
        if (!user.email.isNullOrBlank()) {
            binding.etCvEmail.setText(user.email)
        }

        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val fullName = doc.getString("fullName") ?: doc.getString("name") ?: ""
                val email = doc.getString("email") ?: user.email ?: ""
                val dept = doc.getString("department") ?: "Computer Science & Engineering"

                if (fullName.isNotBlank()) binding.etCvName.setText(fullName)
                if (email.isNotBlank()) binding.etCvEmail.setText(email)

                currentDegree = "B.Sc. in $dept"
                binding.tvCvDegree.text = currentDegree
            }
    }

    private fun showEditEducationDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_academic_calendar_exception, null)
        val builder = AlertDialog.Builder(this)
            .setTitle("Edit Education Details")

        val inputLayout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 20, 50, 10)
        }

        val etDegree = EditText(this).apply {
            hint = "Degree (e.g. B.Sc. in CSE)"
            setText(currentDegree)
        }
        val etUniv = EditText(this).apply {
            hint = "University (e.g. Gopalganj Science & Tech University)"
            setText(currentUniversity)
        }
        val etCgpa = EditText(this).apply {
            hint = "CGPA (e.g. 3.84)"
            setText(currentCgpa)
        }
        val etGrad = EditText(this).apply {
            hint = "Graduation Year (e.g. 2026)"
            setText(currentGraduation)
        }

        inputLayout.addView(etDegree)
        inputLayout.addView(etUniv)
        inputLayout.addView(etCgpa)
        inputLayout.addView(etGrad)

        builder.setView(inputLayout)
            .setPositiveButton("Save") { _, _ ->
                currentDegree = etDegree.text.toString().trim().ifBlank { currentDegree }
                currentUniversity = etUniv.text.toString().trim().ifBlank { currentUniversity }
                currentCgpa = etCgpa.text.toString().trim().ifBlank { currentCgpa }
                currentGraduation = etGrad.text.toString().trim().ifBlank { currentGraduation }

                binding.tvCvDegree.text = currentDegree
                binding.tvCvUniversity.text = currentUniversity
                binding.tvCvCgpaBadge.text = "CGPA: $currentCgpa"
                binding.tvCvGraduation.text = "Graduation: $currentGraduation"
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun generateAcademicResumePdf() {
        val rawName = binding.etCvName.text.toString().trim().ifBlank { "Alif Khan" }
        val rawEmail = binding.etCvEmail.text.toString().trim().ifBlank { "alif.cse@gstu.edu.bd" }
        val rawSkills = binding.etCvSkills.text.toString().trim().ifBlank {
            "Flutter, Dart, Node.js, Go, Docker, PostgreSQL, Git"
        }
        val rawProjects = binding.etCvProjects.text.toString().trim().ifBlank {
            "GSTU Go: Campus SuperApp; Microservices E-Commerce"
        }

        fun esc(text: String): String = android.text.TextUtils.htmlEncode(text)

        val name = esc(rawName)
        val email = esc(rawEmail)
        val skills = esc(rawSkills)
        val projects = esc(rawProjects)
        val safeDegree = esc(currentDegree)
        val safeUniversity = esc(currentUniversity)
        val safeCgpa = esc(currentCgpa)
        val safeGraduation = esc(currentGraduation)

        val htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <style>
                    @page {
                        size: A4 portrait;
                        margin: 20mm 18mm;
                    }
                    body {
                        font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;
                        color: #111827;
                        line-height: 1.45;
                        margin: 0;
                        padding: 0;
                        background: #fff;
                    }
                    .header {
                        border-bottom: 2px solid #4F46E5;
                        padding-bottom: 12px;
                        margin-bottom: 18px;
                    }
                    .name {
                        font-size: 26px;
                        font-weight: 800;
                        color: #0F172A;
                        margin: 0 0 4px 0;
                        letter-spacing: -0.5px;
                    }
                    .contact {
                        font-size: 13px;
                        color: #475569;
                    }
                    .section {
                        margin-top: 18px;
                    }
                    .section-title {
                        font-size: 13.5px;
                        font-weight: 700;
                        text-transform: uppercase;
                        letter-spacing: 0.8px;
                        color: #4338CA;
                        border-bottom: 1px solid #E2E8F0;
                        padding-bottom: 4px;
                        margin-bottom: 10px;
                    }
                    .item-title {
                        font-size: 14px;
                        font-weight: 700;
                        color: #1E293B;
                    }
                    .item-subtitle {
                        font-size: 13px;
                        color: #64748B;
                    }
                    .item-badges {
                        font-size: 12.5px;
                        color: #059669;
                        font-weight: 600;
                        margin-top: 2px;
                    }
                    .skills-box {
                        font-size: 13px;
                        color: #334155;
                        line-height: 1.6;
                    }
                    .project-item {
                        margin-bottom: 10px;
                    }
                    .footer-note {
                        font-size: 10.5px;
                        color: #94A3B8;
                        text-align: center;
                        margin-top: 40px;
                    }
                </style>
            </head>
            <body>
                <div class="header">
                    <h1 class="name">$name</h1>
                    <div class="contact">
                        <strong>Email:</strong> $email &nbsp;|&nbsp; <strong>Institution:</strong> $safeUniversity
                    </div>
                </div>

                <div class="section">
                    <div class="section-title">Education</div>
                    <div>
                        <div class="item-title">$safeDegree</div>
                        <div class="item-subtitle">$safeUniversity</div>
                        <div class="item-badges">CGPA: $safeCgpa &nbsp;|&nbsp; Expected Graduation: $safeGraduation</div>
                    </div>
                </div>

                <div class="section">
                    <div class="section-title">Core Technical Competencies</div>
                    <div class="skills-box">
                        $skills
                    </div>
                </div>

                <div class="section">
                    <div class="section-title">Featured Academic &amp; Engineering Projects</div>
                    <div class="project-item">
                        <div class="item-title">$projects</div>
                        <div class="item-subtitle" style="margin-top: 4px;">
                            Designed and engineered full-stack campus and engineering solutions with real-time synchronization, modular clean architecture, and optimized performance benchmarks.
                        </div>
                    </div>
                </div>

                <div class="section">
                    <div class="section-title">Academic Achievements &amp; Extracurriculars</div>
                    <div class="skills-box">
                        &bull; Active Member of GSTU Computer Club (GSTUCC) &amp; Competitive Programming Wing<br>
                        &bull; Participated in Intra-University Hackathons and National Coding Contests<br>
                        &bull; Dean's Honor List Candidate based on academic excellence
                    </div>
                </div>

                <div class="footer-note">
                    Official Academic Curriculum Vitae &bull; Generated via GSTU Go Platform
                </div>
            </body>
            </html>
        """.trimIndent()

        val webView = WebView(this).apply {
            settings.apply {
                javaScriptEnabled = false
                allowFileAccess = false
                allowContentAccess = false
            }
        }
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val printManager = getSystemService(Context.PRINT_SERVICE) as? PrintManager
                val docName = "Resume_${name.replace(" ", "_")}"
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
        Toast.makeText(this, "Preparing Academic Resume PDF...", Toast.LENGTH_SHORT).show()
    }
}
