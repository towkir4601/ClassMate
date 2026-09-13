with open("firestore.rules", "r") as f:
    content = f.read()

old_rule = """    match /users/{userId} {
      allow read: if request.auth != null &&
        (request.auth.uid == userId || canManageUsers() || isFriendsPublic());"""

new_rule = """    match /users/{userId} {
      allow read: if request.auth != null;"""

content = content.replace(old_rule, new_rule)

with open("firestore.rules", "w") as f:
    f.write(content)
