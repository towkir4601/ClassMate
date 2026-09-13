with open("firestore.rules", "r") as f:
    content = f.read()

# 1. Add chat_rooms
chat_rules = """
    match /chat_rooms/{roomId} {
      allow read: if isLoggedIn();
      allow create: if isLoggedIn();
      allow update: if isLoggedIn() &&
        (!("id" in request.resource.data) || request.resource.data.id == roomId);
      allow delete: if isAdmin();
      
      match /messages/{messageId} {
        allow read: if isLoggedIn();
        allow create: if isLoggedIn()
          && request.resource.data.senderId == request.auth.uid
          && (!("text" in request.resource.data) || request.resource.data.text.size() <= 5000)
          && (!("imageUrl" in request.resource.data) || request.resource.data.imageUrl.size() <= 1000);
          
        allow update: if isLoggedIn()
          && request.resource.data.senderId == resource.data.senderId
          && (!("text" in request.resource.data.diff(resource.data).affectedKeys()) || resource.data.senderId == request.auth.uid)
          && (!("text" in request.resource.data) || request.resource.data.text.size() <= 5000)
          && (!("reactions" in request.resource.data.diff(resource.data).affectedKeys()) || true)
          && (!("seenBy" in request.resource.data.diff(resource.data).affectedKeys()) || true)
          && (!("isPinned" in request.resource.data.diff(resource.data).affectedKeys()) || true)
          && (!("isDeleted" in request.resource.data.diff(resource.data).affectedKeys()) || resource.data.senderId == request.auth.uid);
          
        allow delete: if isAdmin() || resource.data.senderId == request.auth.uid;
      }
    }
"""

if "match /chat_rooms" not in content:
    content = content.replace("match /{document=**} {", chat_rules + "\n    match /{document=**} {")

# 2. Add batch to notices
content = content.replace('"isDeleted"\n      ])', '"isDeleted",\n        "batch"\n      ])')
content = content.replace('"isDeleted",\n        "updatedAt"\n      ])', '"isDeleted",\n        "updatedAt",\n        "batch"\n      ])')

# 3. Add uploadedByUid to library and qb
content = content.replace('function validLibraryCreate() {\n      return canUploadPdf()\n        && validLibraryBaseFields()', 'function validLibraryCreate() {\n      return canUploadPdf()\n        && request.resource.data.uploadedByUid == request.auth.uid\n        && validLibraryBaseFields()')
content = content.replace('function validQbCreate() {\n      return canUploadPdf()\n        && validQbBaseFields()', 'function validQbCreate() {\n      return canUploadPdf()\n        && request.resource.data.uploadedByUid == request.auth.uid\n        && validQbBaseFields()')

# 4. allow read for approved students
content = content.replace('allow read: if isLoggedIn() && resource.data.isDeleted != true;', 'allow read: if isLoggedIn() && resource.data.isDeleted != true\n         && (get(/databases/$(database)/documents/users/$(request.auth.uid)).data.approved == true || isAdmin() || isTeacher());')

with open("firestore.rules", "w") as f:
    f.write(content)
