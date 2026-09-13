import re

with open('firestore.rules', 'r') as f:
    content = f.read()

bad_keys = """        && request.resource.data.diff(resource.data).affectedKeys().hasOnly([
          "uid",
          "createdAt",
          "name",
          "fullName",
          "email",
          "photoUrl",
          "studentId",
          "department",
          "authProvider",
          "updatedAt",
          "phone",
          "bloodGroup",
          "homeDistrict",
          "address",
          "oneSignalPlayerId",
          "permissions",
          "role",
          "favoriteSubjects",
          "favoritePdfIds"
        ])"""

good_keys = """        && request.resource.data.diff(resource.data).affectedKeys().hasOnly([
          "uid",
          "createdAt",
          "name",
          "fullName",
          "email",
          "photoUrl",
          "studentId",
          "department",
          "authProvider",
          "updatedAt",
          "phone",
          "whatsappNumber",
          "batch",
          "fatherName",
          "motherName",
          "presentAddress",
          "permanentAddress",
          "bloodGroup",
          "homeDistrict",
          "address",
          "oneSignalPlayerId",
          "permissions",
          "role",
          "favoriteSubjects",
          "favoritePdfIds",
          "approved"
        ])"""
content = content.replace(bad_keys, good_keys)

# Ensure they can only set approved to false if they update it
bad_role_check = """        && (!("role" in request.resource.data.diff(resource.data).affectedKeys()) ||
            (!("role" in resource.data) && request.resource.data.role == "student"))"""
good_role_check = """        && (!("role" in request.resource.data.diff(resource.data).affectedKeys()) ||
            (!("role" in resource.data) && request.resource.data.role == "student"))
        && (!("approved" in request.resource.data.diff(resource.data).affectedKeys()) ||
            request.resource.data.approved == false)"""
content = content.replace(bad_role_check, good_role_check)

with open('firestore.rules', 'w') as f:
    f.write(content)
