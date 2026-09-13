import re

with open('firestore.rules', 'r') as f:
    content = f.read()

bad = """    function validManagerUserUpdate() {
      return canManageUsers()
        && request.resource.data.diff(resource.data).affectedKeys().hasOnly([
          "role",
          "permissions"
        ])"""

good = """    function validManagerUserUpdate() {
      return canManageUsers()
        && request.resource.data.diff(resource.data).affectedKeys().hasOnly([
          "role",
          "permissions",
          "approved"
        ])"""
content = content.replace(bad, good)

with open('firestore.rules', 'w') as f:
    f.write(content)
