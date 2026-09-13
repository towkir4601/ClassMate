import re

with open('app/src/main/java/com/shuaib/classmate/fragments/TimetableFragment.kt', 'r') as f:
    content = f.read()

bad = """        binding.tvHeroSubject.text = nextPeriod.subject
        binding.tvHeroTeacher.text = nextPeriod.teacher.ifBlank { "Course Teacher" }"""
good = """        binding.tvHeroSubject.text = nextPeriod.subject
        val batchText = if (nextPeriod.batch.isNotBlank() && nextPeriod.batch != "all") " (Batch ${nextPeriod.batch})" else ""
        binding.tvHeroTeacher.text = nextPeriod.teacher.ifBlank { "Course Teacher" } + batchText"""
content = content.replace(bad, good)
with open('app/src/main/java/com/shuaib/classmate/fragments/TimetableFragment.kt', 'w') as f:
    f.write(content)
