import re

with open('firestore.rules', 'r') as f:
    content = f.read()

old_str = """           || get(/databases/$(database)/documents/users/$(request.auth.uid)).data.batch == resource.data.batch
         )
         && get(/databases/$(database)/documents/users/$(request.auth.uid)).data.approved == true;"""

new_str = """           || get(/databases/$(database)/documents/users/$(request.auth.uid)).data.batch == resource.data.batch
           || resource.data.batch == ""
         )
         && get(/databases/$(database)/documents/users/$(request.auth.uid)).data.approved == true;"""

content = content.replace(old_str, new_str)

with open('firestore.rules', 'w') as f:
    f.write(content)
