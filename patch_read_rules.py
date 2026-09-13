import re

with open('firestore.rules', 'r') as f:
    content = f.read()

bad_lib_read = """    match /library_files/{fileId} {
      allow read: if isLoggedIn() && resource.data.isDeleted != true;"""

good_lib_read = """    match /library_files/{fileId} {
      allow read: if isLoggedIn() && resource.data.isDeleted != true &&
         (
           get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role in ['admin', 'superadmin', 'teacher']
           || get(/databases/$(database)/documents/users/$(request.auth.uid)).data.batch == resource.data.batch
         );"""

bad_qb_read = """    match /question_bank/{qbId} {
      allow read: if isLoggedIn() && resource.data.isDeleted != true;"""

good_qb_read = """    match /question_bank/{qbId} {
      allow read: if isLoggedIn() && resource.data.isDeleted != true &&
         (
           get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role in ['admin', 'superadmin', 'teacher']
           || get(/databases/$(database)/documents/users/$(request.auth.uid)).data.batch == resource.data.batch
         );"""

content = content.replace(bad_lib_read, good_lib_read)
content = content.replace(bad_qb_read, good_qb_read)

with open('firestore.rules', 'w') as f:
    f.write(content)
