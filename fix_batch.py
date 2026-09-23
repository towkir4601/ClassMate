import re

with open('app/src/main/java/com/shuaib/classmate/activities/PostNoticeActivity.kt', 'r') as f:
    content = f.read()

# Replace any occurrence of the old check with targetBatchId in maps
content = re.sub(
    r'"targetBatch" to if \(binding\.toggleTarget\.checkedButtonId == R\.id\.btnTargetAll\) "all" else currentUserBatch',
    r'"targetBatch" to targetBatchId',
    content
)

# And in NotificationSender calls
content = re.sub(
    r'targetBatch = if \(binding\.toggleTarget\.checkedButtonId == R\.id\.btnTargetAll\) "all" else currentUserBatch',
    r'targetBatch = targetBatchId',
    content
)

# Fix missing targetBatchId lines in functions
funcs = ["publishAiCancellation", "saveNoticeToFirestore", "postCancellationNotice", "publishSubstituteNotice", "publishVacationNotice"]

lines = content.split('\n')
for i, line in enumerate(lines):
    if "binding.progressBar.isVisible = true" in line and "val targetBatchId = getTargetBatchOrNull()" not in lines[i-1]:
        # Check backwards if we are in one of those funcs
        in_func = False
        for j in range(i, max(-1, i-40), -1):
            if any(f"fun {func}" in lines[j] for func in funcs):
                in_func = True
                break
        
        if in_func:
            indent = line[:len(line) - len(line.lstrip())]
            lines[i] = indent + "val targetBatchId = getTargetBatchOrNull() ?: run { binding.progressBar.isVisible = false; binding.btnPublish.isEnabled = true; return }\n" + line

content = '\n'.join(lines)

with open('app/src/main/java/com/shuaib/classmate/activities/PostNoticeActivity.kt', 'w') as f:
    f.write(content)
