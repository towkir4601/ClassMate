package com.shuaib.classmate.utils

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.shuaib.classmate.models.PdfFile

object LibraryShareHelper {

    fun sharePdfLink(context: Context, pdf: PdfFile) {
        val link = pdf.downloadUrl.ifBlank { pdf.driveUrl.ifBlank { pdf.telegramUrl } }

        if (link.isBlank()) {
            Toast.makeText(context, "Link not available to share", Toast.LENGTH_SHORT).show()
            return
        }

        val shareText = buildString {
            append("📄 ").append(pdf.title).append("\n")

            if (pdf.subject.isNotBlank()) {
                append("📚 Subject: ").append(pdf.subject)
                if (pdf.courseCode.isNotBlank()) {
                    append(" (").append(pdf.courseCode).append(")")
                }
                append("\n")
            }

            if (pdf.year.isNotBlank() || pdf.examType.isNotBlank()) {
                val qbMeta = listOf(pdf.examType, pdf.year).filter { it.isNotBlank() }.joinToString(" ")
                append("🎓 Exam: ").append(qbMeta).append("\n")
            }

            if (pdf.description.isNotBlank()) {
                append("📝 ").append(pdf.description).append("\n")
            }

            append("🔗 Link: ").append(link).append("\n\n")
            append("Shared via ClassMate App")
        }

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, pdf.title)
            putExtra(Intent.EXTRA_TEXT, shareText.trim())
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val chooser = Intent.createChooser(sendIntent, "Share document with friends").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to share link", Toast.LENGTH_SHORT).show()
        }
    }
}
