package com.shuaib.classmate.activities

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.google.firebase.firestore.FirebaseFirestore
import com.shuaib.classmate.BuildConfig
import com.shuaib.classmate.R
import com.shuaib.classmate.adapters.RoutineAssignmentAdapter
import com.shuaib.classmate.databinding.ActivityRoutineBatchBinding
import com.shuaib.classmate.models.BatchItem
import com.shuaib.classmate.models.Period
import com.shuaib.classmate.models.ScheduleAssignment
import com.shuaib.classmate.network.RoutineRetrofitClient
import com.shuaib.classmate.utils.animateSpringScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Locale

class RoutineBatchActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRoutineBatchBinding
    private val adapter = RoutineAssignmentAdapter()
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    private var currentBatchId: Int = -1
    private var batchList: List<BatchItem> = emptyList()
    private var allAssignments: List<ScheduleAssignment> = emptyList()

    private var selectedBatchFilter: String = "All"
    private var selectedDayFilter: String = "All"

    companion object {
        const val EXTRA_BATCH_ID = "extra_batch_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRoutineBatchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentBatchId = intent.getIntExtra(EXTRA_BATCH_ID, -1)

        setupToolbar()
        setupRecyclerView()
        setupListeners()

        loadBatchesAndCurrent()
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener {
            it.animateSpringScale(0.9f)
            finish()
        }
        binding.btnRefresh.setOnClickListener {
            it.animateSpringScale(0.9f)
            if (currentBatchId > 0) {
                loadBatchDetail(currentBatchId)
            } else {
                loadBatchesAndCurrent()
            }
        }
    }

    private fun setupRecyclerView() {
        binding.rvAssignments.layoutManager = LinearLayoutManager(this)
        binding.rvAssignments.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnGenerateCsp.setOnClickListener {
            it.animateSpringScale(0.95f)
            if (currentBatchId <= 0) {
                Toast.makeText(this, "No batch selected", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            confirmAndGenerateRoutine()
        }

        binding.btnDirectAddToTimetable.setOnClickListener {
            it.animateSpringScale(0.95f)
            confirmDirectAddToTimetable()
        }

        binding.btnExportPdf.setOnClickListener {
            it.animateSpringScale(0.95f)
            exportRoutineFile("pdf", "application/pdf")
        }

        binding.btnExportDocx.setOnClickListener {
            it.animateSpringScale(0.95f)
            exportRoutineFile("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
        }

        binding.btnExportCsv.setOnClickListener {
            it.animateSpringScale(0.95f)
            exportRoutineFile("csv", "text/csv")
        }
    }

    private fun loadBatchesAndCurrent() {
        binding.pbMainLoading.isVisible = true

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RoutineRetrofitClient.apiService.getBatches()
                }

                if (response.isSuccessful) {
                    val batches = response.body()?.batches ?: emptyList()
                    batchList = batches

                    if (batches.isNotEmpty()) {
                        setupBatchSpinner(batches)

                        // If no specific batch ID requested or requested ID not found, select first
                        if (currentBatchId <= 0 || batches.none { it.id == currentBatchId }) {
                            currentBatchId = batches.first().id
                        }

                        // Select the spinner item matching currentBatchId
                        val selectedIndex = batches.indexOfFirst { it.id == currentBatchId }.coerceAtLeast(0)
                        binding.spinnerBatches.setSelection(selectedIndex)

                        loadBatchDetail(currentBatchId)
                    } else {
                        binding.pbMainLoading.isVisible = false
                        Toast.makeText(this@RoutineBatchActivity, "No batches found on server", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    binding.pbMainLoading.isVisible = false
                    val err = parseErrorBody(response.errorBody()?.string())
                    Toast.makeText(this@RoutineBatchActivity, "Failed to load batches: $err", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                binding.pbMainLoading.isVisible = false
                Toast.makeText(this@RoutineBatchActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupBatchSpinner(batches: List<BatchItem>) {
        val labels = batches.map { b ->
            val fac = b.faculty?.take(12) ?: "Faculty"
            val sem = b.semester ?: "Sem"
            val yr = b.year ?: ""
            "Batch #${b.id} ($fac - $yr $sem)"
        }

        val spinnerAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            labels
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        binding.spinnerBatches.adapter = spinnerAdapter
        binding.spinnerBatches.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selected = batches.getOrNull(position) ?: return
                if (selected.id != currentBatchId) {
                    currentBatchId = selected.id
                    loadBatchDetail(currentBatchId)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadBatchDetail(batchId: Int) {
        binding.pbMainLoading.isVisible = true

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RoutineRetrofitClient.apiService.getBatchDetail(batchId)
                }

                binding.pbMainLoading.isVisible = false

                if (response.isSuccessful) {
                    val batch = response.body()?.batch
                    if (batch != null) {
                        displayBatchOverview(batch)
                    }

                    // Also fetch routine schedule if available
                    loadBatchSchedule(batchId)
                } else {
                    val err = parseErrorBody(response.errorBody()?.string())
                    Toast.makeText(this@RoutineBatchActivity, "Failed to load batch: $err", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                binding.pbMainLoading.isVisible = false
                Toast.makeText(this@RoutineBatchActivity, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun displayBatchOverview(batch: BatchItem) {
        val counts = batch.counts
        binding.tvCountTeachers.text = counts?.teachers?.toString() ?: "0"
        binding.tvCountCourses.text = counts?.courses?.toString() ?: "0"
        binding.tvCountRooms.text = counts?.rooms?.toString() ?: "0"
        binding.tvCountAssignments.text = counts?.assignments?.toString() ?: "0"

        binding.tvMetadataDept.text = batch.department?.ifBlank { "Gopalganj Science and Technology University" }
            ?: "Gopalganj Science and Technology University"

        val facultyStr = batch.faculty?.ifBlank { "Faculty" } ?: "Faculty"
        val deptStr = batch.department?.ifBlank { "Department" } ?: "Department"
        binding.tvMetadataFaculty.text = "$facultyStr · $deptStr"

        val yearStr = batch.year ?: ""
        val semStr = batch.semester ?: ""
        binding.tvMetadataTerm.text = "Academic Year $yearStr · $semStr Semester"

        binding.tvMetadataFilename.text = "File: ${batch.filename ?: "routine.xlsx"}"
        val status = batch.status?.uppercase(Locale.US) ?: "READY"
        binding.tvMetadataStatus.text = "Status: $status"
    }

    private fun loadBatchSchedule(batchId: Int) {
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RoutineRetrofitClient.apiService.getSchedule(batchId)
                }

                if (response.isSuccessful) {
                    val body = response.body()
                    val assignments = body?.assignments ?: emptyList()
                    allAssignments = assignments

                    if (assignments.isNotEmpty()) {
                        binding.tvCountAssignments.text = assignments.size.toString()
                        binding.layoutRoutinePostGen.isVisible = true

                        val score = body?.score
                        if (score != null && score > 0) {
                            binding.layoutQualityScore.isVisible = true
                            binding.tvRoutineScore.text = String.format(Locale.US, "Routine Quality Score: %.1f / 10", score)
                        } else {
                            binding.layoutQualityScore.isVisible = false
                        }

                        buildFilterChips(assignments)
                        applyFilters()
                    } else {
                        showEmptySchedule("Routine not generated yet for this batch.\nTap 'Generate Routine (Run CSP)' above to compute schedule.")
                    }
                } else {
                    showEmptySchedule("Routine not generated yet for this batch.\nTap 'Generate Routine (Run CSP)' above to compute schedule.")
                }
            } catch (_: Exception) {
                showEmptySchedule("No schedule computed yet.")
            }
        }
    }

    private fun confirmAndGenerateRoutine() {
        AlertDialog.Builder(this)
            .setTitle("Generate Routine with CSP")
            .setMessage("This will run the constraint satisfaction solver algorithm on the backend to place all classes without teacher, room, or batch conflicts.\n\nContinue?")
            .setPositiveButton("Generate") { _, _ ->
                runCspGeneration(currentBatchId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun runCspGeneration(batchId: Int) {
        binding.pbGeneratingCsp.isVisible = true
        binding.tvCspStatus.isVisible = true
        binding.tvCspStatus.text = "⏳ Running CSP constraint satisfaction algorithm on backend…"
        binding.btnGenerateCsp.isEnabled = false

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RoutineRetrofitClient.apiService.generateSchedule(batchId)
                }

                binding.pbGeneratingCsp.isVisible = false
                binding.btnGenerateCsp.isEnabled = true

                if (response.isSuccessful) {
                    val body = response.body()
                    val assignments = body?.assignments ?: emptyList()
                    allAssignments = assignments

                    binding.tvCspStatus.text = "✅ Routine generated successfully! (${assignments.size} classes placed)"
                    binding.tvCountAssignments.text = assignments.size.toString()
                    binding.layoutRoutinePostGen.isVisible = true

                    val score = body?.score
                    if (score != null && score > 0) {
                        binding.layoutQualityScore.isVisible = true
                        binding.tvRoutineScore.text = String.format(Locale.US, "Routine Quality Score: %.1f / 10", score)
                    }

                    buildFilterChips(assignments)
                    applyFilters()

                    Toast.makeText(this@RoutineBatchActivity, "Routine generated successfully!", Toast.LENGTH_LONG).show()
                } else {
                    val errStr = response.errorBody()?.string()
                    val cleanErr = parseErrorBody(errStr)
                    binding.tvCspStatus.text = "❌ Generation failed: $cleanErr"
                    Toast.makeText(this@RoutineBatchActivity, cleanErr, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                binding.pbGeneratingCsp.isVisible = false
                binding.btnGenerateCsp.isEnabled = true
                val err = e.message ?: "Unknown error"
                binding.tvCspStatus.text = "❌ Error: $err"
                Toast.makeText(this@RoutineBatchActivity, "Error: $err", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun buildFilterChips(assignments: List<ScheduleAssignment>) {
        val batches = listOf("All") + assignments.map { it.year_sem }.filter { it.isNotBlank() }.distinct().sorted()
        binding.chipGroupBatches.removeAllViews()

        for (batch in batches) {
            val chip = Chip(this).apply {
                text = batch
                isCheckable = true
                isChecked = (batch == selectedBatchFilter)
                setOnClickListener {
                    selectedBatchFilter = batch
                    applyFilters()
                }
            }
            binding.chipGroupBatches.addView(chip)
        }

        val days = listOf("All") + assignments.map { it.day.uppercase(Locale.US) }.filter { it.isNotBlank() }.distinct()
        binding.chipGroupDays.removeAllViews()

        for (day in days) {
            val chip = Chip(this).apply {
                text = day
                isCheckable = true
                isChecked = (day == selectedDayFilter)
                setOnClickListener {
                    selectedDayFilter = day
                    applyFilters()
                }
            }
            binding.chipGroupDays.addView(chip)
        }
    }

    private fun applyFilters() {
        var filtered = allAssignments

        if (selectedBatchFilter != "All") {
            filtered = filtered.filter { it.year_sem.equals(selectedBatchFilter, ignoreCase = true) }
        }

        if (selectedDayFilter != "All") {
            filtered = filtered.filter { it.day.equals(selectedDayFilter, ignoreCase = true) }
        }

        binding.tvFilteredCount.text = "${filtered.size} classes"

        if (filtered.isNotEmpty()) {
            binding.rvAssignments.isVisible = true
            binding.layoutEmptyAssignments.isVisible = false
            adapter.submitList(filtered)
        } else {
            binding.rvAssignments.isVisible = false
            binding.layoutEmptyAssignments.isVisible = true
            binding.tvEmptyMessage.text = "No classes found matching the selected filters."
        }
    }

    private fun showEmptySchedule(message: String) {
        binding.layoutRoutinePostGen.isVisible = false
        binding.rvAssignments.isVisible = false
        binding.layoutEmptyAssignments.isVisible = true
        binding.tvEmptyMessage.text = message
        binding.tvFilteredCount.text = "0 classes"
        adapter.submitList(emptyList())
    }

    // ─────────────────────────────────────────────────────────────
    // Direct Addition into Class Routine (Firestore Sync)
    // ─────────────────────────────────────────────────────────────

    private fun confirmDirectAddToTimetable() {
        if (allAssignments.isEmpty()) {
            Toast.makeText(this, "Please generate a routine first before adding to timetable", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Add to Class Routine")
            .setMessage(
                "This will add all ${allAssignments.size} generated classes from this routine directly into ClassMate's timetable in Firestore.\n\n" +
                "All students will immediately see these scheduled classes for their batches. Do you want to proceed?"
            )
            .setPositiveButton("Publish to Routine") { _, _ ->
                publishRoutineToFirestore()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun publishRoutineToFirestore() {
        binding.pbMainLoading.isVisible = true

        lifecycleScope.launch {
            try {
                var addedCount = 0
                val dayMap = mapOf(
                    "SAT" to "saturday",
                    "SUN" to "sunday",
                    "MON" to "monday",
                    "TUE" to "tuesday",
                    "WED" to "wednesday",
                    "THU" to "thursday",
                    "FRI" to "friday"
                )

                // Chunk assignments to adhere to Firestore's 500 operations batch limit
                val chunks = allAssignments.chunked(400)

                for (chunk in chunks) {
                    val writeBatch = firestore.batch()
                    for (item in chunk) {
                        val dayUpper = item.day.uppercase(Locale.US).take(3)
                        val firestoreDay = dayMap[dayUpper] ?: item.day.lowercase(Locale.US)

                        val docRef = firestore.collection("timetable")
                            .document(firestoreDay)
                            .collection("periods")
                            .document()

                        val startStr = formatSlotTime(item.slot_start)
                        val endStr = formatSlotTime(item.slot_end)

                        val subjectStr = if (!item.course_name.isNullOrBlank() && item.course_name != item.course_code) {
                            "${item.course_code} - ${item.course_name}"
                        } else {
                            item.course_code
                        }

                        val period = Period(
                            id = docRef.id,
                            subject = subjectStr,
                            teacher = item.teacher_abbr,
                            startTime = startStr,
                            endTime = endStr,
                            room = item.room_id,
                            batch = item.year_sem
                        )

                        writeBatch.set(docRef, period)
                        addedCount++
                    }
                    writeBatch.commit().await()
                }

                binding.pbMainLoading.isVisible = false
                Toast.makeText(
                    this@RoutineBatchActivity,
                    "🎉 Successfully added $addedCount classes to Class Routine!",
                    Toast.LENGTH_LONG
                ).show()

            } catch (e: Exception) {
                binding.pbMainLoading.isVisible = false
                Toast.makeText(
                    this@RoutineBatchActivity,
                    "Failed to add to timetable: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // File Export Download via DownloadManager
    // ─────────────────────────────────────────────────────────────

    private fun exportRoutineFile(extension: String, mimeType: String) {
        if (currentBatchId <= 0) {
            Toast.makeText(this, "No batch selected", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val baseUrl = BuildConfig.ROUTINE_BACKEND_URL.trimEnd('/')
            val exportUrl = "$baseUrl/api/batches/$currentBatchId/export.$extension"
            val fileName = "Class_Routine_Batch_${currentBatchId}.$extension"

            val request = DownloadManager.Request(Uri.parse(exportUrl)).apply {
                setTitle("Downloading $fileName")
                setDescription("Routine file export ($extension)")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                setMimeType(mimeType)
            }

            val manager = getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            if (manager != null) {
                manager.enqueue(request)
                Toast.makeText(this, "Downloading $fileName to Downloads…", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Download manager unavailable", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Download error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun formatSlotTime(raw: Any?): String {
        if (raw == null) return "00:00"
        if (raw is Number) {
            val mins = raw.toInt()
            val h = mins / 60
            val m = mins % 60
            return String.format(Locale.US, "%02d:%02d", h, m)
        }
        val s = raw.toString().trim()
        if (s.matches(Regex("^\\d+$"))) {
            val mins = s.toIntOrNull() ?: 0
            val h = mins / 60
            val m = mins % 60
            return String.format(Locale.US, "%02d:%02d", h, m)
        }
        if (s.contains(":")) {
            val parts = s.split(":")
            val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
            return String.format(Locale.US, "%02d:%02d", h, m)
        }
        return s
    }

    private fun parseErrorBody(errStr: String?): String {
        if (errStr.isNullOrBlank()) return "Unknown server error"
        return try {
            val json = JSONObject(errStr)
            json.optString("message").ifBlank {
                json.optString("error", errStr)
            }
        } catch (_: Exception) {
            errStr
        }
    }
}
