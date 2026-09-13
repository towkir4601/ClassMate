import re

with open('firestore.rules', 'r') as f:
    content = f.read()

# We need to add a check that permissions is not affected, OR if it is, the user cannot change it.
# Wait, why is "permissions" even in the allowed list for validOwnUserUpdate?
# Maybe we can just add `&& !("permissions" in request.resource.data.diff(resource.data).affectedKeys())`

old_str = """        && (!("approved" in request.resource.data.diff(resource.data).affectedKeys()) ||
            request.resource.data.approved == false)"""

new_str = """        && (!("approved" in request.resource.data.diff(resource.data).affectedKeys()) ||
            request.resource.data.approved == false)
        && !("permissions" in request.resource.data.diff(resource.data).affectedKeys())"""

content = content.replace(old_str, new_str)

with open('firestore.rules', 'w') as f:
    f.write(content)
