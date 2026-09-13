import re

with open('app/src/main/java/com/shuaib/classmate/fragments/ProfileFragment.kt', 'r') as f:
    content = f.read()

bad1 = """        binding.cardPersonalInfo.applyClickAnimation {
            currentUser?.let { showEditProfileDialog(it) }
        }"""
good1 = ""
content = content.replace(bad1, good1)

# Also need to remove showEditProfileDialog method entirely to clean up code
bad_method_pattern = r"private fun showEditProfileDialog\(user: User\) \{.*?(?=\n    private fun updateUserInFirestore)"
content = re.sub(bad_method_pattern, "", content, flags=re.DOTALL)

with open('app/src/main/java/com/shuaib/classmate/fragments/ProfileFragment.kt', 'w') as f:
    f.write(content)
