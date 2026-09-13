import re

with open('firestore.rules', 'r') as f:
    content = f.read()

bad = """    function validManagerUserUpdate() {
      return canManageUsers()
        && request.resource.data.diff(resource.data).affectedKeys().hasOnly(["""

good = """    function validManagerUserUpdate() {
      return canManageUsers()
        && (resource.data.role != "superadmin" || isSuperAdmin())
        && (!("role" in request.resource.data.diff(resource.data).affectedKeys()) || request.resource.data.role != "superadmin" || isSuperAdmin())
        && request.auth.uid != resource.data.uid
        && request.resource.data.diff(resource.data).affectedKeys().hasOnly(["""
content = content.replace(bad, good)

with open('firestore.rules', 'w') as f:
    f.write(content)
