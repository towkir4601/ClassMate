with open("firestore.rules", "r") as f:
    content = f.read()

old_rule = 'allow delete: if isLoggedIn() && (isSuperAdmin() || (canManageUsers() && resource.data.role != "superadmin"));'
new_rule = 'allow delete: if isLoggedIn() && (isSuperAdmin() || (canManageUsers() && (!("role" in resource.data) || resource.data.role != "superadmin")));'

content = content.replace(old_rule, new_rule)

with open("firestore.rules", "w") as f:
    f.write(content)
