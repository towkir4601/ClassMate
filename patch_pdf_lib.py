import re

with open('app/src/main/java/com/shuaib/classmate/fragments/PdfLibraryFragment.kt', 'r') as f:
    content = f.read()

old_isadmin = 'isAdmin = (role == "superadmin" || role == "admin" || permissions["canUploadPDF"] == true || permissions["canUploadLibrary"] == true)'
new_isadmin = 'isAdmin = (role == "superadmin" || role == "admin")\n                    val canUpload = isAdmin || permissions["canUploadPDF"] == true || permissions["canUploadLibrary"] == true'

content = content.replace(old_isadmin, new_isadmin)
content = content.replace('binding.btnUploadPdf.isVisible = isAdmin', 'binding.btnUploadPdf.isVisible = canUpload')
content = content.replace('binding.btnAddRegular.isVisible = isAdmin', 'binding.btnAddRegular.isVisible = canUpload')
content = content.replace('binding.btnAddLab.isVisible = isAdmin', 'binding.btnAddLab.isVisible = canUpload')
content = content.replace('binding.btnAddOther.isVisible = isAdmin', 'binding.btnAddOther.isVisible = canUpload')

with open('app/src/main/java/com/shuaib/classmate/fragments/PdfLibraryFragment.kt', 'w') as f:
    f.write(content)
print("Patched PdfLibraryFragment")
