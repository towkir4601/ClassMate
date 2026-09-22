with open('app/src/main/res/layout/fragment_timetable.xml', 'r') as f:
    content = f.read()

content = content.replace('@drawable/bg_spinner', '@drawable/bg_search_field')

with open('app/src/main/res/layout/fragment_timetable.xml', 'w') as f:
    f.write(content)
print("Patched bg_search_field")
