with open("app/src/main/java/com/shuaib/classmate/utils/NotificationSender.kt", "r") as f:
    content = f.read()

old_logic = """            .addOnSuccessListener { doc ->
                val playerId = doc.getString("oneSignalPlayerId")
                    ?: doc.getString("onesignalPlayerId")
                    ?: doc.getString("playerId")
                if (!playerId.isNullOrBlank()) {
                    sendToPlayers(
                        playerIds = listOf(playerId),
                        title = senderName,
                        message = bodyText,
                        type = "chat_message",
                        extraData = data,
                        onFailure = onFailure
                    )
                } else {
                    sendToExternalUser(
                        externalUserId = uid,
                        title = senderName,
                        message = bodyText,
                        type = "chat_message",
                        extraData = data,
                        onFailure = onFailure
                    )
                }
            }"""

new_logic = """            .addOnSuccessListener { doc ->
                // Always use external_id alias because OneSignal.login(uid) is used in the app.
                // This is much more reliable than the saved oneSignalPlayerId which might be stale or v3 format.
                sendToExternalUser(
                    externalUserId = uid,
                    title = senderName,
                    message = bodyText,
                    type = "chat_message",
                    extraData = data,
                    onFailure = onFailure
                )
            }"""

content = content.replace(old_logic, new_logic)

with open("app/src/main/java/com/shuaib/classmate/utils/NotificationSender.kt", "w") as f:
    f.write(content)
