kt_path = "app/src/main/java/com/shuaib/classmate/activities/AdminPanelActivity.kt"
with open(kt_path, 'r') as f:
    kt_content = f.read()

kt_content = kt_content.replace("Thread {", "androidx.lifecycle.lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {")
kt_content = kt_content.replace(".start()", "")
kt_content = kt_content.replace("Handler(Looper.getMainLooper()).post {", "withContext(kotlinx.coroutines.Dispatchers.Main) {")

with open(kt_path, 'w') as f:
    f.write(kt_content)
