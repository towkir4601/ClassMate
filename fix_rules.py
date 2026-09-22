import re

with open('firestore.rules', 'r') as f:
    content = f.read()

# 1. & 2. Fix studentId regex everywhere
content = re.sub(r'\^\[A-Z\]\{2,4\}\[0-9\]\{5\}\$', '^[a-zA-Z0-9]{3,50}$', content)

# 3. Add canManageAcademicCalendar to noElevatedCreatePermissions hasOnly
content = content.replace(
    '"canEditTimetable",\n            "canPostNotices"',
    '"canEditTimetable",\n            "canManageAcademicCalendar",\n            "canPostNotices"'
)

# Add it to the false check in noElevatedCreatePermissions
content = content.replace(
    '&& (!("canEditTimetable" in request.resource.data.permissions) ||\n              request.resource.data.permissions.canEditTimetable == false)',
    '&& (!("canEditTimetable" in request.resource.data.permissions) ||\n              request.resource.data.permissions.canEditTimetable == false)\n          && (!("canManageAcademicCalendar" in request.resource.data.permissions) ||\n              request.resource.data.permissions.canManageAcademicCalendar == false)'
)

# 4. Add canManageAcademicCalendar to validOwnPermissionsUpdate
# It should already be caught by the first replace for hasOnly because they are identical blocks, but let's check.
# Wait, they might not be identical spacing.
content = content.replace(
    '&& permissionStaysFalseOrUnchanged("canEditTimetable")',
    '&& permissionStaysFalseOrUnchanged("canEditTimetable")\n          && permissionStaysFalseOrUnchanged("canManageAcademicCalendar")'
)

# 5. Fix validOwnUserCreate role check
content = content.replace(
    '&& (!("role" in request.resource.data) || request.resource.data.role == "student")',
    '&& (!("role" in request.resource.data) || request.resource.data.role in ["student", "teacher"])'
)

# 6. Add missing fields to validOwnUserUpdate
content = content.replace(
    '"photoUrl",\n          "studentId",',
    '"photoUrl",\n          "studentId",\n          "batch",\n          "whatsappNumber",\n          "presentAddress",\n          "permanentAddress",\n          "fatherName",\n          "motherName",\n          "isDeleted",\n          "approved",'
)

# Also validOwnUserUpdate checks role update:
content = content.replace(
    '&& (!("role" in request.resource.data.diff(resource.data).affectedKeys()) ||\n            (!("role" in resource.data) && request.resource.data.role == "student"))',
    '&& (!("role" in request.resource.data.diff(resource.data).affectedKeys()) ||\n            (!("role" in resource.data) && request.resource.data.role in ["student", "teacher"]))'
)


with open('firestore.rules', 'w') as f:
    f.write(content)

