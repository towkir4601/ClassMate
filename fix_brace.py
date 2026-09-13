import re

with open('app/src/main/java/com/shuaib/classmate/activities/RegisterActivity.kt', 'r') as f:
    content = f.read()

bad_str = """                showError("Could not save profile: ${e.message}")
                shakeForm()
            }

    private fun updatePasswordStrength(password: String) {"""

good_str = """                showError("Could not save profile: ${e.message}")
                shakeForm()
            }
    }

    private fun updatePasswordStrength(password: String) {"""

content = content.replace(bad_str, good_str)

with open('app/src/main/java/com/shuaib/classmate/activities/RegisterActivity.kt', 'w') as f:
    f.write(content)

