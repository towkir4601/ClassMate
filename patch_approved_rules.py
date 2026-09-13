import re

with open('firestore.rules', 'r') as f:
    content = f.read()

def replace_read(match):
    return match.group(0).replace(");", ")\n           && get(/databases/$(database)/documents/users/$(request.auth.uid)).data.approved == true;")

# We need to find the exact replacement string we just inserted
# It's better to just do a string replace on the good_lib_read and good_qb_read.

old_str = """           || get(/databases/$(database)/documents/users/$(request.auth.uid)).data.batch == resource.data.batch
         );"""

new_str = """           || get(/databases/$(database)/documents/users/$(request.auth.uid)).data.batch == resource.data.batch
         )
         && get(/databases/$(database)/documents/users/$(request.auth.uid)).data.approved == true;"""

content = content.replace(old_str, new_str)

with open('firestore.rules', 'w') as f:
    f.write(content)
