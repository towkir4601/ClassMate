with open("firestore.rules", "r") as f:
    content = f.read()

old_keys = """          "favoriteSubjects",
          "favoritePdfIds",
          "approved"
        ])"""

new_keys = """          "favoriteSubjects",
          "favoritePdfIds",
          "approved",
          "isOnline"
        ])"""

content = content.replace(old_keys, new_keys)

with open("firestore.rules", "w") as f:
    f.write(content)
