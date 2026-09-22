import os
import re

# 1. Update activity_admin_panel.xml
xml_path = "app/src/main/res/layout/activity_admin_panel.xml"
with open(xml_path, 'r') as f:
    xml_content = f.read()

card_xml = """
        <com.google.android.material.card.MaterialCardView
            android:id="@+id/cardUploadRoutineCsv"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginBottom="16dp"
            android:clickable="true"
            android:focusable="true"
            android:visibility="gone"
            app:cardCornerRadius="16dp"
            app:cardElevation="0dp"
            app:strokeColor="#1F2937"
            app:strokeWidth="1dp">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:background="?attr/selectableItemBackground"
                android:gravity="center_vertical"
                android:orientation="horizontal"
                android:padding="20dp">

                <ImageView
                    android:layout_width="28dp"
                    android:layout_height="28dp"
                    android:src="@drawable/ic_calendar"
                    app:tint="@color/primary" />

                <LinearLayout
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_marginStart="16dp"
                    android:layout_weight="1"
                    android:orientation="vertical">

                    <TextView
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:text="Upload Routine (CSV)"
                        android:textColor="#FFFFFF"
                        android:textSize="18sp"
                        android:textStyle="bold" />

                    <TextView
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:layout_marginTop="4dp"
                        android:text="Import CSP routine from CSV file"
                        android:textColor="#9CA3AF"
                        android:textSize="14sp" />
                </LinearLayout>

                <ImageView
                    android:layout_width="24dp"
                    android:layout_height="24dp"
                    android:src="@drawable/ic_chevron_right"
                    app:tint="#6B7280" />
            </LinearLayout>
        </com.google.android.material.card.MaterialCardView>
"""

# Insert before cardManageUsers
xml_content = xml_content.replace('android:id="@+id/cardManageUsers"', card_xml.strip() + '\n\n        <com.google.android.material.card.MaterialCardView\n            android:id="@+id/cardManageUsers"')
with open(xml_path, 'w') as f:
    f.write(xml_content)


# 2. Update AdminPanelActivity.kt
kt_path = "app/src/main/java/com/shuaib/classmate/activities/AdminPanelActivity.kt"
with open(kt_path, 'r') as f:
    kt_content = f.read()

imports = """
import androidx.activity.result.contract.ActivityResultContracts
import java.io.BufferedReader
import java.io.InputStreamReader
import com.shuaib.classmate.models.Period
import com.google.firebase.firestore.WriteBatch
"""
if "import java.io.BufferedReader" not in kt_content:
    kt_content = kt_content.replace("import com.shuaib.classmate.models.User", "import com.shuaib.classmate.models.User\n" + imports)

# Add launcher
launcher_code = """
    private val csvPickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            parseAndUploadCsv(uri)
        }
    }
"""
kt_content = kt_content.replace('class AdminPanelActivity : AppCompatActivity() {', 'class AdminPanelActivity : AppCompatActivity() {\n' + launcher_code)

# Add to setupClickListeners
click_listener_code = """
        // Upload CSV Routine
        if (user.role == "superadmin") {
            binding.cardUploadRoutineCsv.visibility = View.VISIBLE
            binding.cardUploadRoutineCsv.applyClickAnimation {
                csvPickerLauncher.launch("*/*")
            }
        }
"""
kt_content = kt_content.replace('if (user.canManageUsers()) {', click_listener_code + '\n        if (user.canManageUsers()) {')

# Add to animateEntry
kt_content = kt_content.replace('if (binding.cardManageUsers.visibility == View.VISIBLE) viewsToAnimate.add(binding.cardManageUsers)', 'if (binding.cardManageUsers.visibility == View.VISIBLE) viewsToAnimate.add(binding.cardManageUsers)\n        if (binding.cardUploadRoutineCsv.visibility == View.VISIBLE) viewsToAnimate.add(binding.cardUploadRoutineCsv)')

# Add parsing function
parsing_function = """
    private fun parseAndUploadCsv(uri: android.net.Uri) {
        val progressDialog = MaterialAlertDialogBuilder(this, R.style.Theme_ClassMate_Dialog)
            .setTitle("Uploading Routine")
            .setMessage("Parsing and uploading to Firestore...")
            .setCancelable(false)
            .show()

        Thread {
            try {
                val inputStream = contentResolver.openInputStream(uri)
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
                        val row = line.split(",")
                        if (!headerProcessed) {
                            for ((i, col) in row.withIndex()) {
                                val cleanCol = col.trim().replace("\"", "")
                                if (cleanCol.equals("Day", true)) dayIdx = i
                                if (cleanCol.equals("YearSemester", true) || cleanCol.equals("Batch", true)) batchIdx = i
                                if (cleanCol.equals("CourseCode", true)) subjectIdx = i
                                if (cleanCol.equals("CourseTitle", true)) titleIdx = i
                                if (cleanCol.equals("TeacherFullName", true) || cleanCol.equals("Teacher", true)) teacherIdx = i
                                if (cleanCol.equals("Room", true)) roomIdx = i
                                if (cleanCol.equals("TimeSlot", true)) timeSlotIdx = i
                                if (cleanCol.equals("StartTime", true)) startTimeIdx = i
                                if (cleanCol.equals("EndTime", true)) endTimeIdx = i
                            }
                            // TeacherFullName preferred over Teacher code if both match the "Teacher" fallback above
                            // We will check if "TeacherFullName" is found explicitly
                            for ((i, col) in row.withIndex()) {
                                if (col.trim().replace("\"", "").equals("TeacherFullName", true)) teacherIdx = i
                            }
                            headerProcessed = true
                            continue
                        }

                        if (row.size < 4) continue

                        val day = if (dayIdx != -1 && dayIdx < row.size) row[dayIdx].trim().replace("\"", "") else ""
                        if (day.isBlank()) continue

                        val batch = if (batchIdx != -1 && batchIdx < row.size) row[batchIdx].trim().replace("\"", "") else ""
                        val courseCode = if (subjectIdx != -1 && subjectIdx < row.size) row[subjectIdx].trim().replace("\"", "") else ""
                        val courseTitle = if (titleIdx != -1 && titleIdx < row.size) row[titleIdx].trim().replace("\"", "") else ""
                        val teacher = if (teacherIdx != -1 && teacherIdx < row.size) row[teacherIdx].trim().replace("\"", "") else ""
                        val room = if (roomIdx != -1 && roomIdx < row.size) row[roomIdx].trim().replace("\"", "") else ""
                        val timeSlot = if (timeSlotIdx != -1 && timeSlotIdx < row.size) row[timeSlotIdx].trim().replace("\"", "") else ""
                        
                        var start = ""
                        var end = ""
                        if (timeSlot.contains("-")) {
                            val parts = timeSlot.split("-")
                            start = parts[0].trim()
                            end = parts[1].trim()
                        } else {
                            if (startTimeIdx != -1 && startTimeIdx < row.size) start = row[startTimeIdx].trim().replace("\"", "")
                            if (endTimeIdx != -1 && endTimeIdx < row.size) end = row[endTimeIdx].trim().replace("\"", "")
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

                        if (!periodsByDay.containsKey(day)) {
                            periodsByDay[day] = mutableListOf()
                        }
                        periodsByDay[day]?.add(period)
                    }
                }
                
                // Now let's try to merge consecutive blocks of the same subject & room
                val finalPeriodsByDay = mutableMapOf<String, MutableList<Period>>()
                for ((day, periods) in periodsByDay) {
                    val mergedList = mutableListOf<Period>()
                    var currentGroup: Period? = null
                    
                    for (p in periods) {
                        if (currentGroup == null) {
                            currentGroup = p
                            continue
                        }
                        // If same batch, subject, teacher, room, and currentGroup.endTime == p.startTime (roughly)
                        // Actually, just comparing if they are identical except time is safer.
                        // Let's just insert them all to avoid merging bugs with time formats, 
                        // Timetable fragment handles individual slots well.
                        mergedList.add(p)
                    }
                    if (currentGroup != null) {
                        // wait, the merge logic above was aborted. Let's just use original periods.
                    }
                    finalPeriodsByDay[day] = periods
                }

                val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                
                // Batch writes (max 500 per batch)
                var batchWrite = firestore.batch()
                var operationCount = 0
                
                // Clear existing timetable first? Usually we might want to clear or just overwrite.
                // It's safer to just add. Or we can warn the user.
                
                for ((day, periods) in finalPeriodsByDay) {
                    val dayRef = firestore.collection("timetable").document(day)
                    // Create day doc if needed
                    batchWrite.set(dayRef, mapOf("updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()))
                    operationCount++
                    
                    for (period in periods) {
                        val pRef = dayRef.collection("periods").document(period.id)
                        batchWrite.set(pRef, period)
                        operationCount++
                        
                        if (operationCount >= 450) {
                            batchWrite.commit().await()
                            batchWrite = firestore.batch()
                            operationCount = 0
                        }
                    }
                }
                
                if (operationCount > 0) {
                    batchWrite.commit().await()
                }

                Handler(Looper.getMainLooper()).post {
                    progressDialog.dismiss()
                    Toast.makeText(this, "✅ Routine uploaded successfully!", Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post {
                    progressDialog.dismiss()
                    Toast.makeText(this, "❌ Error parsing CSV: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }
    
    // Polyfill for await if not in this file
    private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T {
        return kotlinx.coroutines.tasks.await(this)
    }
"""

if "private fun parseAndUploadCsv" not in kt_content:
    kt_content = kt_content.replace("override fun onBackPressed() {", parsing_function + "\n\n    override fun onBackPressed() {")

with open(kt_path, 'w') as f:
    f.write(kt_content)
print("Done")
