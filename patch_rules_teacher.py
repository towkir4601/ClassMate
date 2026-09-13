with open("firestore.rules", "r") as f:
    content = f.read()

old_roles = """          !("role" in request.resource.data.diff(resource.data).affectedKeys()) ||
          request.resource.data.role in ["student", "admin", "superadmin"]"""

new_roles = """          !("role" in request.resource.data.diff(resource.data).affectedKeys()) ||
          request.resource.data.role in ["student", "teacher", "admin", "superadmin"]"""

content = content.replace(old_roles, new_roles)

with open("firestore.rules", "w") as f:
    f.write(content)
