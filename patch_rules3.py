with open("firestore.rules", "r") as f:
    content = f.read()

# Fix noElevatedCreatePermissions
old_keys1 = """          && request.resource.data.permissions.keys().hasOnly([
            "canCreatePolls",
            "canEditTimetable",
            "canPostNotices",
            "canSendClassCancel",
            "canUploadPDF",
            "canUploadLibrary",
            "canUploadResult",
            "canUploadSeatPlan",
            "canManageUsers",
            "canManageAdmins"
          ])"""

new_keys1 = """          && request.resource.data.permissions.keys().hasOnly([
            "canCreatePolls",
            "canEditTimetable",
            "canManageAcademicCalendar",
            "canPostNotices",
            "canSendClassCancel",
            "canUploadPDF",
            "canUploadLibrary",
            "canUploadResult",
            "canUploadSeatPlan",
            "canManageUsers",
            "canManageAdmins"
          ])"""
content = content.replace(old_keys1, new_keys1)

# Fix validOwnUserUpdate
old_keys2 = """          request.resource.data.permissions.keys().hasOnly([
            "canCreatePolls",
            "canEditTimetable",
            "canPostNotices",
            "canSendClassCancel",
            "canUploadPDF",
            "canUploadLibrary",
            "canUploadResult",
            "canUploadSeatPlan",
            "canManageUsers",
            "canManageAdmins"
          ])"""

new_keys2 = """          request.resource.data.permissions.keys().hasOnly([
            "canCreatePolls",
            "canEditTimetable",
            "canManageAcademicCalendar",
            "canPostNotices",
            "canSendClassCancel",
            "canUploadPDF",
            "canUploadLibrary",
            "canUploadResult",
            "canUploadSeatPlan",
            "canManageUsers",
            "canManageAdmins"
          ])"""
content = content.replace(old_keys2, new_keys2)

with open("firestore.rules", "w") as f:
    f.write(content)
