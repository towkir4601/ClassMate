import re

# ====== 1. PdfLibraryFragment.kt ======
with open('app/src/main/java/com/shuaib/classmate/fragments/PdfLibraryFragment.kt', 'r') as f:
    c = f.read()

# Change isAdmin check: only superadmin bypasses batch filter
c = c.replace(
    'isAdmin = (role == "superadmin" || role == "admin")',
    'isAdmin = (role == "superadmin")'
)

with open('app/src/main/java/com/shuaib/classmate/fragments/PdfLibraryFragment.kt', 'w') as f:
    f.write(c)
print("1. Patched PdfLibraryFragment")

# ====== 2. SubjectPdfListFragment.kt ======
with open('app/src/main/java/com/shuaib/classmate/fragments/SubjectPdfListFragment.kt', 'r') as f:
    c = f.read()

c = c.replace(
    'isAdmin = (role == "superadmin" || role == "admin")',
    'isAdmin = (role == "superadmin")'
)

with open('app/src/main/java/com/shuaib/classmate/fragments/SubjectPdfListFragment.kt', 'w') as f:
    f.write(c)
print("2. Patched SubjectPdfListFragment")

# ====== 3. LibrarySearchFragment.kt ======
with open('app/src/main/java/com/shuaib/classmate/fragments/LibrarySearchFragment.kt', 'r') as f:
    c = f.read()

c = c.replace(
    'isAdmin = (role == "superadmin" || role == "admin")',
    'isAdmin = (role == "superadmin")'
)

with open('app/src/main/java/com/shuaib/classmate/fragments/LibrarySearchFragment.kt', 'w') as f:
    f.write(c)
print("3. Patched LibrarySearchFragment")

# ====== 4. LibraryAllFilesFragment.kt ======
with open('app/src/main/java/com/shuaib/classmate/fragments/LibraryAllFilesFragment.kt', 'r') as f:
    c = f.read()

c = c.replace(
    'isAdmin = role == "superadmin" || role == "admin"',
    'isAdmin = role == "superadmin"'
)

with open('app/src/main/java/com/shuaib/classmate/fragments/LibraryAllFilesFragment.kt', 'w') as f:
    f.write(c)
print("4. Patched LibraryAllFilesFragment")

# ====== 5. QuestionBankFragment.kt ======
with open('app/src/main/java/com/shuaib/classmate/fragments/QuestionBankFragment.kt', 'r') as f:
    c = f.read()

c = c.replace(
    'isAdmin = (role == "superadmin" || role == "admin")',
    'isAdmin = (role == "superadmin")'
)

with open('app/src/main/java/com/shuaib/classmate/fragments/QuestionBankFragment.kt', 'w') as f:
    f.write(c)
print("5. Patched QuestionBankFragment")

