import re

with open('firestore.rules', 'r') as f:
    content = f.read()

bad = """      allow delete: if isLoggedIn() && (isSuperAdmin() || canManageUsers());"""
good = """      allow delete: if isLoggedIn() && (isSuperAdmin() || (canManageUsers() && resource.data.role != "superadmin"));"""
content = content.replace(bad, good)

with open('firestore.rules', 'w') as f:
    f.write(content)
