import re

with open('firestore.rules', 'r') as f:
    content = f.read()

bad = """        && (!("role" in request.resource.data) || request.resource.data.role == "student")"""
good = """        && (!("role" in request.resource.data) || request.resource.data.role in ["student", "teacher"])"""
content = content.replace(bad, good)

with open('firestore.rules', 'w') as f:
    f.write(content)
