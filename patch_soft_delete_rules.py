import re

with open('firestore.rules', 'r') as f:
    content = f.read()

bad_library = """    function validLibrarySoftDeleteUpdate() {
      return canUploadPdf()
        && request.resource.data.diff(resource.data).affectedKeys().hasOnly(["isDeleted", "updatedAt"])"""

good_library = """    function validLibrarySoftDeleteUpdate() {
      return canUploadPdf()
        && (isAdmin() || request.auth.uid == resource.data.uploadedByUid)
        && request.resource.data.diff(resource.data).affectedKeys().hasOnly(["isDeleted", "updatedAt"])"""

bad_qb = """    function validQbSoftDelete() {
      return canUploadPdf()
        && request.resource.data.diff(resource.data).affectedKeys().hasOnly(["isDeleted", "updatedAt"])"""

good_qb = """    function validQbSoftDelete() {
      return canUploadPdf()
        && (isAdmin() || request.auth.uid == resource.data.uploadedByUid)
        && request.resource.data.diff(resource.data).affectedKeys().hasOnly(["isDeleted", "updatedAt"])"""

content = content.replace(bad_library, good_library)
content = content.replace(bad_qb, good_qb)

with open('firestore.rules', 'w') as f:
    f.write(content)
