with open("firestore.rules", "r") as f:
    content = f.read()

old_regex1 = 'request.resource.data.studentId.matches("^[A-Z]{2,4}[0-9]{5}$")'
new_regex1 = 'request.resource.data.studentId.matches("^[a-zA-Z0-9]{3,50}$")'

old_regex2 = 'studentId.matches("^[A-Z]{2,4}[0-9]{5}$")'
new_regex2 = 'studentId.matches("^[a-zA-Z0-9]{3,50}$")'

content = content.replace(old_regex1, new_regex1)
content = content.replace(old_regex2, new_regex2)

with open("firestore.rules", "w") as f:
    f.write(content)
