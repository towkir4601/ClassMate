import re

with open('app/src/main/java/com/shuaib/classmate/fragments/ProfileFragment.kt', 'r') as f:
    content = f.read()

bad_method_pattern = r"    private fun updateUserInFirestore\(data: Map<String, Any>\) \{.*?(?=\n    private fun navigateToLogin)"
content = re.sub(bad_method_pattern, "", content, flags=re.DOTALL)

with open('app/src/main/java/com/shuaib/classmate/fragments/ProfileFragment.kt', 'w') as f:
    f.write(content)
