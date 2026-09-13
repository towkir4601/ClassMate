import re

with open('firestore.rules', 'r') as f:
    content = f.read()

# For library_files
old_lib_read = """    match /library_files/{fileId} {
      allow read: if isLoggedIn() && resource.data.isDeleted != true &&
         (
           get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role in ['admin', 'superadmin', 'teacher']
           || get(/databases/$(database)/documents/users/$(request.auth.uid)).data.batch == resource.data.batch
           || resource.data.batch == ""
         )
         && get(/databases/$(database)/documents/users/$(request.auth.uid)).data.approved == true;"""

new_lib_read = """    match /library_files/{fileId} {
      allow read: if isLoggedIn() && resource.data.isDeleted != true
         && get(/databases/$(database)/documents/users/$(request.auth.uid)).data.approved == true;"""

# For question_bank
old_qb_read = """    match /question_bank/{qbId} {
      allow read: if isLoggedIn() && resource.data.isDeleted != true &&
         (
           get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role in ['admin', 'superadmin', 'teacher']
           || get(/databases/$(database)/documents/users/$(request.auth.uid)).data.batch == resource.data.batch
         )
         && get(/databases/$(database)/documents/users/$(request.auth.uid)).data.approved == true;"""

new_qb_read = """    match /question_bank/{qbId} {
      allow read: if isLoggedIn() && resource.data.isDeleted != true
         && get(/databases/$(database)/documents/users/$(request.auth.uid)).data.approved == true;"""

content = content.replace(old_lib_read, new_lib_read)
content = content.replace(old_qb_read, new_qb_read)

with open('firestore.rules', 'w') as f:
    f.write(content)
