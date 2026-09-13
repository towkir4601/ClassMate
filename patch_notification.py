with open("app/src/main/java/com/shuaib/classmate/utils/NotificationSender.kt", "r") as f:
    content = f.read()

old_target = """            targetBuilder = {
                put("include_aliases", JSONObject().put("external_id", org.json.JSONArray().put(externalUserId)))
            },"""
new_target = """            targetBuilder = {
                put("include_aliases", JSONObject().put("external_id", org.json.JSONArray().put(externalUserId)))
                put("target_channel", "push")
            },"""

content = content.replace(old_target, new_target)

with open("app/src/main/java/com/shuaib/classmate/utils/NotificationSender.kt", "w") as f:
    f.write(content)
