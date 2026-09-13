with open("firestore.rules", "r") as f:
    content = f.read()

# Add isOnline to validOwnUserUpdate
if '"isOnline"' not in content:
    content = content.replace('"oneSignalPlayerId",\n          "permissions",', '"oneSignalPlayerId",\n          "permissions",\n          "isOnline",')

with open("firestore.rules", "w") as f:
    f.write(content)
