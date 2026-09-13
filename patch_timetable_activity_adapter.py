import re

with open('app/src/main/java/com/shuaib/classmate/activities/TimetableManagementActivity.kt', 'r') as f:
    content = f.read()

bad = """        val batches = mutableListOf("all")
        for (i in 64 downTo 10) batches.add(i.toString())
        val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, batches)
        dialogBinding.etBatch.setAdapter(adapter)"""
good = """        val batches = mutableListOf("all")
        for (i in 64 downTo 10) batches.add(i.toString())
        val batchAdapter = android.widget.ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, batches)
        dialogBinding.etBatch.setAdapter(batchAdapter)"""
content = content.replace(bad, good)
with open('app/src/main/java/com/shuaib/classmate/activities/TimetableManagementActivity.kt', 'w') as f:
    f.write(content)
