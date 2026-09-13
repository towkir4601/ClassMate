with open("firestore.rules", "r") as f:
    content = f.read()

subjects_rules = """
    match /subjects/{subjectId} {
      allow read: if isLoggedIn();
      allow create: if canUploadPdf();
      allow update: if canUploadPdf();
      allow delete: if isAdmin() || isSuperAdmin();
    }
"""

if "match /subjects" not in content:
    content = content.replace("match /{document=**} {", subjects_rules + "\n    match /{document=**} {")

with open("firestore.rules", "w") as f:
    f.write(content)
