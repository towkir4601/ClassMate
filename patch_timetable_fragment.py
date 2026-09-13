import re

with open('app/src/main/java/com/shuaib/classmate/fragments/TimetableFragment.kt', 'r') as f:
    content = f.read()

bad = """        val periods = rawPeriods.map { period ->
            if (period.cancelDate == targetDateString) {
                period.copy(isCancelled = true)
            } else {
                period.copy(isCancelled = false)
            }
        }"""
good = """        val periods = rawPeriods.filter { period ->
            !period.isTemporary || period.temporaryDate.isBlank() || period.temporaryDate == targetDateString
        }.map { period ->
            if (period.cancelDate == targetDateString) {
                period.copy(isCancelled = true)
            } else {
                period.copy(isCancelled = false)
            }
        }"""

content = content.replace(bad, good)

with open('app/src/main/java/com/shuaib/classmate/fragments/TimetableFragment.kt', 'w') as f:
    f.write(content)
