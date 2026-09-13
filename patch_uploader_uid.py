import re

with open('firestore.rules', 'r') as f:
    content = f.read()

bad_lib = """    function validLibraryCreate() {
      return canUploadPdf()
        && validLibraryBaseFields()"""

good_lib = """    function validLibraryCreate() {
      return canUploadPdf()
        && request.resource.data.uploadedByUid == request.auth.uid
        && validLibraryBaseFields()"""

bad_qb = """    function validQbCreate() {
      return canUploadPdf()
        && validQbBaseFields()"""

good_qb = """    function validQbCreate() {
      return canUploadPdf()
        && request.resource.data.uploadedByUid == request.auth.uid
        && validQbBaseFields()"""

content = content.replace(bad_lib, good_lib)
content = content.replace(bad_qb, good_qb)

with open('firestore.rules', 'w') as f:
    f.write(content)
