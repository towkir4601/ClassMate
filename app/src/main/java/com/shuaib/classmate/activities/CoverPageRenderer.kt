package com.shuaib.classmate.activities

import android.app.Activity
import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.util.Base64
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.IOException

object CoverPageRenderer {

    fun renderHtml(
        context: Context,
        data: CoverPageData
    ): String {
        var html = try {
            context.assets.open("cover_page_template.html").bufferedReader().use { it.readText() }
        } catch (e: IOException) {
            throw IllegalStateException("Unable to load cover_page_template.html from assets", e)
        }

        // Resolve logo base64 from assets for guaranteed offline rendering & printing
        val logoSrc = getLogoBase64(context, data.logoAssetPath)

        val headingText = if (data.type == CoverType.ASSIGNMENT) "Assignment On" else "Lab Report"

        val optionalTopicHtml = if (data.documentTitle.isNotBlank()) {
            """<div class="doc-topic">${escapeHtml(data.documentTitle)}</div>"""
        } else if (data.documentNumber.isNotBlank()) {
            """<div class="doc-topic">${escapeHtml(data.numberLabel)}: ${escapeHtml(data.documentNumber)}</div>"""
        } else {
            ""
        }

        val courseTitleVal = if (data.courseTitle.isNotBlank()) " " + escapeHtml(data.courseTitle) else ""
        val courseCodeVal = if (data.courseCode.isNotBlank()) " " + escapeHtml(data.courseCode) else ""

        // Submitted By Column Content
        val submittedByContent = if (data.isGroupSubmission) {
            buildString {
                append("""<div class="item-row"><span class="item-label">1.</span> <span class="item-val">${escapeHtml(data.studentName)}${if (data.studentId.isNotBlank()) " (${escapeHtml(data.studentId)})" else ""}</span></div>""")
                data.groupMembers.forEachIndexed { index, member ->
                    if (member.isNotBlank()) {
                        append("""<div class="item-row"><span class="item-label">${index + 2}.</span> <span class="item-val">${escapeHtml(member)}</span></div>""")
                    }
                }
                append("""<div class="item-row"><span class="item-label">Session:</span> <span class="item-val">${if (data.session.isNotBlank()) " " + escapeHtml(data.session) else ""}</span></div>""")
                append("""<div class="item-row"><span class="item-label">Department:</span> <span class="item-val">${if (data.studentDepartment.isNotBlank()) " " + escapeHtml(data.studentDepartment) else ""}</span></div>""")
                append("""<div class="item-row inst-footer">GSTU, Gopalganj-8100</div>""")
            }
        } else {
            buildString {
                append("""<div class="item-row"><span class="item-label">Name:</span> <span class="item-val">${if (data.studentName.isNotBlank()) " " + escapeHtml(data.studentName) else ""}</span></div>""")
                append("""<div class="item-row"><span class="item-label">Student ID:</span> <span class="item-val">${if (data.studentId.isNotBlank()) " " + escapeHtml(data.studentId) else ""}</span></div>""")
                append("""<div class="item-row"><span class="item-label">Year:</span> <span class="item-val">${if (data.year.isNotBlank()) " " + escapeHtml(data.year) else ""}</span></div>""")
                append("""<div class="item-row"><span class="item-label">Semester:</span> <span class="item-val">${if (data.semester.isNotBlank()) " " + escapeHtml(data.semester) else ""}</span></div>""")
                append("""<div class="item-row"><span class="item-label">Session:</span> <span class="item-val">${if (data.session.isNotBlank()) " " + escapeHtml(data.session) else ""}</span></div>""")
                append("""<div class="item-row"><span class="item-label">Department:</span> <span class="item-val">${if (data.studentDepartment.isNotBlank()) " " + escapeHtml(data.studentDepartment) else ""}</span></div>""")
                append("""<div class="item-row inst-footer">GSTU, Gopalganj-8100</div>""")
            }
        }

        // Submitted To Column Content
        val submittedToContent = buildString {
            append("""<div class="item-row"><span class="item-label">Name:</span> <span class="item-val">${if (data.teacherName.isNotBlank()) " " + escapeHtml(data.teacherName) else ""}</span></div>""")
            append("""<div class="item-row"><span class="item-label">Designation:</span> <span class="item-val">${if (data.designation.isNotBlank()) " " + escapeHtml(data.designation) else ""}</span></div>""")
            append("""<div class="item-row"><span class="item-label">Department:</span> <span class="item-val">${if (data.teacherDepartment.isNotBlank()) " " + escapeHtml(data.teacherDepartment) else ""}</span></div>""")
            append("""<div class="item-row inst-footer">GSTU, Gopalganj-8100</div>""")
        }

        val replacements = mapOf(
            "university" to escapeHtml(data.university),
            "university_location" to escapeHtml(data.universityLocation),
            "logo_src" to logoSrc,
            "document_heading" to escapeHtml(headingText),
            "optional_topic_html" to optionalTopicHtml,
            "course_title" to courseTitleVal,
            "course_code" to courseCodeVal,
            "submitted_by_content" to submittedByContent,
            "submitted_to_content" to submittedToContent,
            "submission_date" to escapeHtml(data.submissionDate)
        )

        replacements.forEach { (key, value) ->
            html = html.replace("{{$key}}", value)
        }

        return html
    }

    fun escapeHtml(value: String): String {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }

    private fun getLogoBase64(context: Context, assetPath: String): String {
        val candidates = listOf(assetPath, "University_Logo.webp", "university_logo.webp", "gstu_go_logo.webp")
        for (candidate in candidates) {
            try {
                context.assets.open(candidate).use { input ->
                    val bytes = input.readBytes()
                    if (bytes.isNotEmpty()) {
                        val mime = if (candidate.endsWith(".webp", ignoreCase = true)) "image/webp" else "image/png"
                        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                        return "data:$mime;base64,$base64"
                    }
                }
            } catch (_: Exception) {}
        }
        return "file:///android_asset/$assetPath"
    }

    fun loadPreview(
        context: Context,
        webView: WebView,
        data: CoverPageData,
        onPageLoaded: (() -> Unit)? = null
    ) {
        val html = renderHtml(context, data)
        webView.settings.apply {
            javaScriptEnabled = false
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = false
            displayZoomControls = false
            allowFileAccess = true
            allowContentAccess = true
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                onPageLoaded?.invoke()
            }
        }

        webView.loadDataWithBaseURL(
            "file:///android_asset/",
            html,
            "text/html",
            "UTF-8",
            null
        )
    }

    fun printCoverPage(
        activity: Activity,
        webView: WebView,
        docName: String = "GSTU_Cover_Page"
    ) {
        val printManager = activity.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val printAdapter = webView.createPrintDocumentAdapter(docName)
        val attributes = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()

        printManager.print(docName, printAdapter, attributes)
    }
}
