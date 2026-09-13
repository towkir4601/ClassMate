import re

with open('app/src/main/java/com/shuaib/classmate/utils/NotificationSender.kt', 'r') as f:
    content = f.read()

bad = """        if (roomId.startsWith("group_")) {
            sendToAll(
                title = "Class Group",
                message = "$senderName: $bodyText",
                type = "chat_message",
                extraData = data,
                onFailure = onFailure
            )
            return
        }"""
good = """        if (roomId.startsWith("group_")) {
            val batch = roomId.removePrefix("group_batch_")
            val targetBuilder: org.json.JSONObject.() -> Unit = if (batch.isNotBlank() && batch != roomId.removePrefix("group_")) {
                {
                    val filters = org.json.JSONArray().apply {
                        put(org.json.JSONObject().put("field", "tag").put("key", "batch").put("relation", "=").put("value", batch))
                    }
                    put("filters", filters)
                }
            } else {
                { put("included_segments", org.json.JSONArray(listOf("All"))) }
            }
            sendOneSignal(
                title = "Class Group",
                message = "$senderName: $bodyText",
                type = "chat_message",
                extraData = data,
                targetBuilder = targetBuilder,
                onFailure = onFailure
            )
            return
        }"""
content = content.replace(bad, good)
with open('app/src/main/java/com/shuaib/classmate/utils/NotificationSender.kt', 'w') as f:
    f.write(content)
