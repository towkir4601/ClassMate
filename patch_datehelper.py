with open("app/src/main/java/com/shuaib/classmate/utils/DateHelper.kt", "r") as f:
    content = f.read()

old_format = """    private val dateFormat = SimpleDateFormat(
        "yyyy-MM-dd", Locale.getDefault()
    )"""

new_format = """    private val dateFormat = SimpleDateFormat(
        "yyyy-MM-dd", Locale.US
    )"""

content = content.replace(old_format, new_format)

with open("app/src/main/java/com/shuaib/classmate/utils/DateHelper.kt", "w") as f:
    f.write(content)
