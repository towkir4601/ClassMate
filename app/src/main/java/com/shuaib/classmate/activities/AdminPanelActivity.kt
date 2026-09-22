// com/shuaib/classmate/activities/AdminPanelActivity.kt
package com.shuaib.classmate.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.shuaib.classmate.R
import com.shuaib.classmate.databinding.ActivityAdminPanelBinding
import com.shuaib.classmate.models.User

import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.IOException
import com.shuaib.classmate.models.Period
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await

import com.shuaib.classmate.utils.AppConstants
import com.shuaib.classmate.utils.applyClickAnimation

class AdminPanelActivity : AppCompatActivity() {

    private val csvPickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            parseAndUploadCsv(uri)
        }
    }


    private lateinit var binding: ActivityAdminPanelBinding
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminPanelBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        binding.btnTestTelegram.setOnClickListener {
            testTelegramConnection()
        }

        checkPermissionsAndSetupUI()
    }

    private fun checkPermissionsAndSetupUI() {
        val uid = auth.currentUser?.uid ?: return

        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                try {
                    val role = document.getString("role") ?: "student"
                    val permissions = document.get("permissions") as? Map<String, Boolean> ?: User.DEFAULT_PERMISSIONS
                    val user = User(
                        uid = uid,
                        role = role,
                        permissions = permissions
                    )
                    val isSuperAdmin = role == "superadmin"
                    binding.btnTestTelegram.visibility = if (isSuperAdmin) View.VISIBLE else View.GONE
                    setupClickListeners(user)
                    animateEntry()
                } catch (e: Exception) {
                    Toast.makeText(this@AdminPanelActivity, "Error parsing user: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this@AdminPanelActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupClickListeners(user: User) {
        // Post Notice
        if (user.canPostNotices()) {
            binding.cardPostNotice.visibility = View.VISIBLE
            binding.cardPostNotice.applyClickAnimation {
                startActivity(Intent(this, PostNoticeActivity::class.java))
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
        }

        // Timetable
        if (user.canEditTimetable()) {
            binding.cardEditTimetable.visibility = View.VISIBLE
            binding.cardEditTimetable.applyClickAnimation {
                startActivity(Intent(this, TimetableManagementActivity::class.java))
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }

        }
        
        // Academic Calendar
        if (user.canManageAcademicCalendar()) {
            binding.cardAcademicCalendar.visibility = View.VISIBLE
            binding.cardAcademicCalendar.applyClickAnimation {
                startActivity(Intent(this, AcademicCalendarActivity::class.java))
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
        }
        
        if (user.canEditTimetable()) {
            binding.cardManageBusSchedule.visibility = View.VISIBLE
            binding.cardManageBusSchedule.applyClickAnimation {
                startActivity(Intent(this, BusScheduleManagementActivity::class.java))
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
        }

        // PDF
        if (user.canUploadPDF() || user.canUploadLibrary()) {
            binding.cardUploadPDF.visibility = View.VISIBLE
            binding.cardUploadPDF.applyClickAnimation {
                startActivity(Intent(this, PdfUploadActivity::class.java))
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
        }

        // Seat Plan
        if (user.canUploadSeatPlan()) {
            binding.cardUploadSeatPlan.visibility = View.VISIBLE
            binding.cardUploadSeatPlan.applyClickAnimation {
                startActivity(Intent(this, SeatPlanUploadActivity::class.java))
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
        }

        // Result
        if (user.canUploadResult()) {
            binding.cardUploadResult.visibility = View.VISIBLE
            binding.cardUploadResult.applyClickAnimation {
                startActivity(Intent(this, ResultUploadActivity::class.java))
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
        }

        
        // Upload CSV Routine
        if (user.role == "superadmin") {
            binding.cardUploadRoutineCsv.visibility = View.VISIBLE
            binding.cardUploadRoutineCsv.applyClickAnimation {
                csvPickerLauncher.launch("text/*")
            }
        }

        if (user.canManageUsers()) {
            binding.cardManageUsers.visibility = View.VISIBLE
            binding.cardManageUsers.applyClickAnimation {
                startActivity(Intent(this, UserManagementActivity::class.java))
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
        }
    }

    private fun testTelegramConnection() {
        Thread {
            try {
                val testUrl = "https://api.telegram.org/bot" +
                        AppConstants.TELEGRAM_BOT_TOKEN +
                        "/sendMessage"

                val connection = java.net.URL(testUrl)
                    .openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty(
                    "Content-Type", "application/json"
                )
                connection.doOutput = true
                connection.connectTimeout = 10000

                val body = """
            {
                "chat_id": "${AppConstants.TELEGRAM_CHANNEL_ID}",
                "text": "✅ ClassMate bot is connected and working!"
            }
            """.trimIndent()

                connection.outputStream.write(
                    body.toByteArray(Charsets.UTF_8)
                )
                connection.outputStream.flush()

                val responseCode = connection.responseCode
                val response = if (responseCode == 200) {
                    connection.inputStream
                        .bufferedReader().readText()
                } else {
                    connection.errorStream
                        ?.bufferedReader()?.readText()
                        ?: "Unknown error"
                }

                android.util.Log.d("TELEGRAM_TEST",
                    "Code: $responseCode")
                android.util.Log.d("TELEGRAM_TEST",
                    "Response: $response")

                Handler(Looper.getMainLooper()).post {
                    if (responseCode == 200) {
                        Toast.makeText(this@AdminPanelActivity,
                            "✅ Telegram connected!",
                            Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@AdminPanelActivity,
                            "❌ Failed: $response",
                            Toast.LENGTH_LONG).show()
                    }
                }

            } catch (e: Exception) {
                android.util.Log.e("TELEGRAM_TEST",
                    "Error: ${e.message}")
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(this@AdminPanelActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun animateEntry() {
        val viewsToAnimate = mutableListOf<View>()
        viewsToAnimate.add(binding.tvTitle)
        
        if (binding.cardPostNotice.visibility == View.VISIBLE) viewsToAnimate.add(binding.cardPostNotice)
        if (binding.cardEditTimetable.visibility == View.VISIBLE) viewsToAnimate.add(binding.cardEditTimetable)
        if (binding.cardAcademicCalendar.visibility == View.VISIBLE) viewsToAnimate.add(binding.cardAcademicCalendar)
        if (binding.cardManageBusSchedule.visibility == View.VISIBLE) viewsToAnimate.add(binding.cardManageBusSchedule)
        if (binding.cardUploadPDF.visibility == View.VISIBLE) viewsToAnimate.add(binding.cardUploadPDF)
        if (binding.cardUploadSeatPlan.visibility == View.VISIBLE) viewsToAnimate.add(binding.cardUploadSeatPlan)
        if (binding.cardUploadResult.visibility == View.VISIBLE) viewsToAnimate.add(binding.cardUploadResult)
        if (binding.cardUploadRoutineCsv.visibility == View.VISIBLE) viewsToAnimate.add(binding.cardUploadRoutineCsv)
        if (binding.cardManageUsers.visibility == View.VISIBLE) viewsToAnimate.add(binding.cardManageUsers)
        if (binding.btnTestTelegram.visibility == View.VISIBLE) viewsToAnimate.add(binding.btnTestTelegram)

        viewsToAnimate.forEachIndexed { index, view ->
            view.translationY = 100f
            view.alpha = 0f
            view.animate()
                .translationY(0f)
                .alpha(1f)
                .setStartDelay(index * 100L)
                .setDuration(600)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    
    private fun parseAndUploadCsv(uri: android.net.Uri) {
        // First, ask user for confirmation
        MaterialAlertDialogBuilder(this, R.style.Theme_ClassMate_Dialog)
            .setTitle("⚠️ Replace Timetable?")
            .setMessage("This will replace the existing timetable with the new CSV data. Are you sure?")
            .setPositiveButton("Yes, Upload") { _, _ ->
                performCsvUpload(uri)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performCsvUpload(uri: android.net.Uri) {
        val progressDialog = MaterialAlertDialogBuilder(this, R.style.Theme_ClassMate_Dialog)
            .setTitle("Uploading Routine")
            .setMessage("Parsing and uploading to Firestore...")
            .setCancelable(false)
            .show()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                    ?: throw IOException("Cannot open file")
                val reader = BufferedReader(InputStreamReader(inputStream))

                var headerProcessed = false
                var dayIdx = -1
                var batchIdx = -1
                var subjectIdx = -1
                var titleIdx = -1
                var teacherIdx = -1
                var roomIdx = -1
                var timeSlotIdx = -1
                var startTimeIdx = -1
                var endTimeIdx = -1

                val periodsByDay = mutableMapOf<String, MutableList<Period>>()

                reader.useLines { lines ->
                    for (line in lines) {
                        val row = parseCsvLine(line)
                        if (!headerProcessed) {
                            for ((i, col) in row.withIndex()) {
                                val cleanCol = col.trim()
                                when {
                                    cleanCol.equals("Day", true) -> dayIdx = i
                                    cleanCol.equals("YearSemester", true) || cleanCol.equals("Batch", true) -> batchIdx = i
                                    cleanCol.equals("CourseCode", true) -> subjectIdx = i
                                    cleanCol.equals("CourseTitle", true) -> titleIdx = i
                                    cleanCol.equals("TeacherFullName", true) -> teacherIdx = i
                                    cleanCol.equals("Teacher", true) && teacherIdx == -1 -> teacherIdx = i
                                    cleanCol.equals("Room", true) -> roomIdx = i
                                    cleanCol.equals("TimeSlot", true) -> timeSlotIdx = i
                                    cleanCol.equals("StartTime", true) -> startTimeIdx = i
                                    cleanCol.equals("EndTime", true) -> endTimeIdx = i
                                }
                            }
                            headerProcessed = true
                            continue
                        }

                        if (row.size < 4) continue

                        val day = safeGet(row, dayIdx)
                        if (day.isBlank()) continue

                        val batch = safeGet(row, batchIdx)
                        val courseCode = safeGet(row, subjectIdx)
                        val courseTitle = safeGet(row, titleIdx)
                        val teacher = safeGet(row, teacherIdx)
                        val room = safeGet(row, roomIdx)
                        val timeSlot = safeGet(row, timeSlotIdx)

                        var start = ""
                        var end = ""
                        if (timeSlot.contains("-")) {
                            val parts = timeSlot.split("-", limit = 2)
                            start = parts[0].trim()
                            end = parts.getOrElse(1) { "" }.trim()
                        } else {
                            start = safeGet(row, startTimeIdx)
                            end = safeGet(row, endTimeIdx)
                        }

                        val subjectFull = if (courseTitle.isNotBlank()) "$courseCode - $courseTitle" else courseCode

                        val period = Period(
                            id = java.util.UUID.randomUUID().toString(),
                            subject = subjectFull,
                            teacher = teacher,
                            startTime = start,
                            endTime = end,
                            room = room,
                            batch = batch
                        )

                        periodsByDay.getOrPut(day) { mutableListOf() }.add(period)
                    }
                }

                if (periodsByDay.isEmpty()) {
                    throw IOException("No valid entries found in CSV. Please check the file format.")
                }

                val db = FirebaseFirestore.getInstance()

                // Step 1: Clear old timetable data for the days being uploaded (Chunked for safety)
                for (day in periodsByDay.keys) {
                    val oldPeriods = db.collection("timetable").document(day)
                        .collection("periods").get().await()
                    
                    var deleteBatch = db.batch()
                    var deleteCount = 0
                    
                    for (doc in oldPeriods.documents) {
                        deleteBatch.delete(doc.reference)
                        deleteCount++
                        
                        // Firestore batch limit is 500, we use 450 to be safe
                        if (deleteCount >= 450) {
                            deleteBatch.commit().await()
                            deleteBatch = db.batch()
                            deleteCount = 0
                        }
                    }
                    if (deleteCount > 0) {
                        deleteBatch.commit().await()
                    }
                }

                // Step 2: Upload new data (Chunked for safety)
                var batchWrite = db.batch()
                var operationCount = 0
                var totalPeriods = 0

                for ((day, periods) in periodsByDay) {
                    val dayRef = db.collection("timetable").document(day)
                    batchWrite.set(dayRef, mapOf("updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()))
                    operationCount++

                    for (period in periods) {
                        val pRef = dayRef.collection("periods").document(period.id)
                        batchWrite.set(pRef, period)
                        operationCount++
                        totalPeriods++

                        if (operationCount >= 450) {
                            batchWrite.commit().await()
                            batchWrite = db.batch()
                            operationCount = 0
                        }
                    }
                }

                if (operationCount > 0) {
                    batchWrite.commit().await()
                }

                withContext(Dispatchers.Main) {
                    if (!isFinishing && !isDestroyed && progressDialog.isShowing) {
                        progressDialog.dismiss()
                    }
                    Toast.makeText(
                        this@AdminPanelActivity,
                        "✅ Routine uploaded! $totalPeriods classes across ${periodsByDay.size} days.",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (!isFinishing && !isDestroyed && progressDialog.isShowing) {
                        progressDialog.dismiss()
                    }
                    Toast.makeText(
                        this@AdminPanelActivity,
                        "❌ Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    /** Properly parses a single CSV line, handling quoted fields with commas inside. */
    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var insideQuotes = false

        for (char in line) {
            when {
                char == '"' -> insideQuotes = !insideQuotes
                char == ',' && !insideQuotes -> {
                    result.add(current.toString().trim())
                    current.clear()
                }
                else -> current.append(char)
            }
        }
        result.add(current.toString().trim())
        return result
    }

    /** Safely gets a value from CSV row by index, returning empty string if invalid. */
    private fun safeGet(row: List<String>, idx: Int): String {
        return if (idx in row.indices) row[idx].trim() else ""
    }


    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}
