import re

with open('firestore.rules', 'r') as f:
    content = f.read()

old_manager_update = '''    function validManagerUserUpdate() {
      return canManageUsers()
        && request.resource.data.diff(resource.data).affectedKeys().hasOnly([
          "role",
          "permissions"
        ])
        && ('''

new_manager_update = '''    function validManagerUserUpdate() {
      return canManageUsers()
        && request.resource.data.diff(resource.data).affectedKeys().hasOnly([
          "role",
          "permissions",
          "approved"
        ])
        && (
          !("approved" in request.resource.data.diff(resource.data).affectedKeys()) ||
          request.resource.data.approved is bool
        )
        && ('''

if old_manager_update in content:
    content = content.replace(old_manager_update, new_manager_update)
    with open('firestore.rules', 'w') as f:
        f.write(content)
    print("Patched validManagerUserUpdate")
else:
    print("Could not find validManagerUserUpdate")

