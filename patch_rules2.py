with open("firestore.rules", "r") as f:
    content = f.read()

old_stay = """          && permissionStaysFalseOrUnchanged("canEditTimetable")
          && permissionStaysFalseOrUnchanged("canPostNotices")"""

new_stay = """          && permissionStaysFalseOrUnchanged("canEditTimetable")
          && permissionStaysFalseOrUnchanged("canManageAcademicCalendar")
          && permissionStaysFalseOrUnchanged("canPostNotices")"""

content = content.replace(old_stay, new_stay)

old_pres = """          && existingTruePermissionPreserved("canEditTimetable")
          && existingTruePermissionPreserved("canPostNotices")"""

new_pres = """          && existingTruePermissionPreserved("canEditTimetable")
          && existingTruePermissionPreserved("canManageAcademicCalendar")
          && existingTruePermissionPreserved("canPostNotices")"""

content = content.replace(old_pres, new_pres)

old_check = """          && (!("canEditTimetable" in request.resource.data.permissions) ||
              request.resource.data.permissions.canEditTimetable == false)
          && (!("canPostNotices" in request.resource.data.permissions) ||"""

new_check = """          && (!("canEditTimetable" in request.resource.data.permissions) ||
              request.resource.data.permissions.canEditTimetable == false)
          && (!("canManageAcademicCalendar" in request.resource.data.permissions) ||
              request.resource.data.permissions.canManageAcademicCalendar == false)
          && (!("canPostNotices" in request.resource.data.permissions) ||"""

content = content.replace(old_check, new_check)

with open("firestore.rules", "w") as f:
    f.write(content)
