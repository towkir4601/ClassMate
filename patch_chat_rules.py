import re

with open('firestore.rules', 'r') as f:
    content = f.read()

bad_chat = """    match /chat_rooms/{roomId} {
      allow read: if isLoggedIn();
      allow create: if isLoggedIn();
      allow update: if isLoggedIn();
      allow delete: if isAdmin();
      match /messages/{messageId} {
        allow read: if isLoggedIn();
        allow create: if isLoggedIn();
        allow update: if isLoggedIn();
        allow delete: if isAdmin();
      }
    }"""

good_chat = """    match /chat_rooms/{roomId} {
      allow read: if isLoggedIn();
      allow create: if isLoggedIn(); // Let users create DM rooms
      allow update: if isLoggedIn() &&
        // Prevent malicious changing of room IDs or overriding members completely if they don't belong
        (!("id" in request.resource.data) || request.resource.data.id == roomId);
      allow delete: if isAdmin();
      
      match /messages/{messageId} {
        allow read: if isLoggedIn();
        allow create: if isLoggedIn()
          && request.resource.data.senderId == request.auth.uid
          && (!("text" in request.resource.data) || request.resource.data.text.size() <= 5000)
          && (!("imageUrl" in request.resource.data) || request.resource.data.imageUrl.size() <= 1000);
          
        allow update: if isLoggedIn()
          && request.resource.data.senderId == resource.data.senderId // Cannot change sender
          && (!("text" in request.resource.data.diff(resource.data).affectedKeys()) || resource.data.senderId == request.auth.uid) // Only sender can edit text
          && (!("text" in request.resource.data) || request.resource.data.text.size() <= 5000); // Size limit
          
        allow delete: if isAdmin() || resource.data.senderId == request.auth.uid;
      }
    }"""

content = content.replace(bad_chat, good_chat)

with open('firestore.rules', 'w') as f:
    f.write(content)
