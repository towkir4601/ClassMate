import re

with open('firestore.rules', 'r') as f:
    content = f.read()

old_qb_read = """    match /question_bank/{paperId} {
      // Any logged-in student can list/read non-deleted papers
      allow read: if isLoggedIn() && resource.data.isDeleted != true;"""

new_qb_read = """    match /question_bank/{paperId} {
      // Any logged-in student can list/read non-deleted papers
      allow read: if isLoggedIn() && resource.data.isDeleted != true
         && get(/databases/$(database)/documents/users/$(request.auth.uid)).data.approved == true;"""

content = content.replace(old_qb_read, new_qb_read)

with open('firestore.rules', 'w') as f:
    f.write(content)
