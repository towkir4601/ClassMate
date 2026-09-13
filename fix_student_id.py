import re

with open('app/src/main/java/com/shuaib/classmate/utils/StudentIdUtils.kt', 'r') as f:
    content = f.read()

bad_valid = "fun isValid(value: String): Boolean = Pattern.matches(normalize(value))"
good_valid = """fun isValid(value: String): Boolean {
        if (value.contains("@")) return false
        return Pattern.matches(normalize(value))
    }"""

content = content.replace(bad_valid, good_valid)

with open('app/src/main/java/com/shuaib/classmate/utils/StudentIdUtils.kt', 'w') as f:
    f.write(content)
