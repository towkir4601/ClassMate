package com.shuaib.classmate.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.shuaib.classmate.chat.ChatRepository
import org.json.JSONObject
import com.shuaib.classmate.notices.NoticeTextFormatter

object NotificationSender {

    private val scope = CoroutineScope(Dispatchers.IO)

    private fun getChannelIdForType(type: String): String {
        return when (type) {
            "chat_message" -> "chat_messages"
            "notice" -> "classmate_notices"
            "cancellation", "substitute" -> "classmate_cancellations"
            else -> "classmate_notifications"
        }
    }

    fun sendToAll(
        title: String,
        message: String,
        type: String,
        extraData: Map<String, String> = emptyMap(),
        targetBatch: String = "all",
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        val targetBuilder: org.json.JSONObject.() -> Unit = if (targetBatch.isNotBlank() && !targetBatch.equals("all", ignoreCase = true)) {
            {
                val filters = org.json.JSONArray().apply {
                    put(org.json.JSONObject().put("field", "tag").put("key", "batch").put("relation", "=").put("value", targetBatch))
                    put(org.json.JSONObject().put("operator", "AND"))
                    put(org.json.JSONObject().put("field", "tag").put("key", "role").put("relation", "!=").put("value", "teacher"))
                    put(org.json.JSONObject().put("operator", "OR"))
                    put(org.json.JSONObject().put("field", "tag").put("key", "semester").put("relation", "=").put("value", targetBatch))
                    put(org.json.JSONObject().put("operator", "AND"))
                    put(org.json.JSONObject().put("field", "tag").put("key", "role").put("relation", "!=").put("value", "teacher"))
                }
                put("filters", filters)
            }
        } else {
            { 
                val filters = org.json.JSONArray().apply {
                    put(org.json.JSONObject().put("field", "tag").put("key", "role").put("relation", "!=").put("value", "teacher"))
                }
                put("filters", filters)
            }
        }

        sendOneSignal(
            title = title,
            message = message,
            type = type,
            extraData = extraData,
            targetBuilder = targetBuilder,
            onSuccess = onSuccess,
            onFailure = onFailure
        )
    }

    fun sendChatMessageAlert(
        roomId: String,
        senderId: String,
        senderName: String,
        messageText: String,
        targetUserId: String? = null,
        onFailure: (String) -> Unit = {}
    ) {
        val bodyText = messageText.ifBlank { "Photo" }.take(100)
        val data = mapOf("roomId" to roomId, "senderId" to senderId)
        
        if (roomId.startsWith("group_")) {
            // Group chat push alert - MUST EXCLUDE THE SENDER
            scope.launch {
                try {
                    val batch = when {
                        roomId.startsWith("group_batch_") -> roomId.removePrefix("group_batch_")
                        roomId.contains("_batch_") -> roomId.substringAfter("_batch_")
                        else -> ""
                    }

                    val usersColl = com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("users")
                    val query = if (batch.isNotBlank()) usersColl.whereEqualTo("batch", batch) else usersColl

                    query.get().addOnSuccessListener { snapshot ->
                        val targetUids = snapshot.documents
                            .map { it.id }
                            .filter { it.isNotBlank() && it != senderId } // Strictly exclude sender!

                        if (targetUids.isNotEmpty()) {
                            sendOneSignal(
                                title = "Class Group",
                                message = "$senderName: $bodyText",
                                type = "chat_message",
                                extraData = data,
                                targetBuilder = {
                                    val externalIdArray = org.json.JSONArray()
                                    targetUids.take(2000).forEach { externalIdArray.put(it) }
                                    put("include_aliases", org.json.JSONObject().put("external_id", externalIdArray))
                                    put("target_channel", "push")
                                },
                                onFailure = onFailure
                            )
                        } else {
                            // Fallback using OneSignal tag filter explicitly excluding the sender
                            val filters = org.json.JSONArray().apply {
                                if (batch.isNotBlank()) {
                                    put(org.json.JSONObject().put("field", "tag").put("key", "batch").put("relation", "=").put("value", batch))
                                    put(org.json.JSONObject().put("operator", "AND"))
                                }
                                put(org.json.JSONObject().put("field", "tag").put("key", "uid").put("relation", "!=").put("value", senderId))
                            }
                            sendOneSignal(
                                title = "Class Group",
                                message = "$senderName: $bodyText",
                                type = "chat_message",
                                extraData = data,
                                targetBuilder = { put("filters", filters) },
                                onFailure = onFailure
                            )
                        }
                    }.addOnFailureListener {
                        // Fallback on error: filter out senderId via OneSignal tag
                        val filters = org.json.JSONArray().apply {
                            if (batch.isNotBlank()) {
                                put(org.json.JSONObject().put("field", "tag").put("key", "batch").put("relation", "=").put("value", batch))
                                put(org.json.JSONObject().put("operator", "AND"))
                            }
                            put(org.json.JSONObject().put("field", "tag").put("key", "uid").put("relation", "!=").put("value", senderId))
                        }
                        sendOneSignal(
                            title = "Class Group",
                            message = "$senderName: $bodyText",
                            type = "chat_message",
                            extraData = data,
                            targetBuilder = { put("filters", filters) },
                            onFailure = onFailure
                        )
                    }
                } catch (e: Exception) {
                    onFailure(e.message ?: "Failed to dispatch group push alert")
                }
            }
            return
        }

        // Direct Message:
        val uid = targetUserId ?: return
        if (uid == senderId) return // Never send DM push to self!

        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                val playerId = doc.getString("oneSignalPlayerId")
                sendOneSignal(
                    title = senderName,
                    message = bodyText,
                    type = "chat_message",
                    extraData = data,
                    targetBuilder = {
                        val externalIdArray = org.json.JSONArray().put(uid)
                        put("include_aliases", org.json.JSONObject().put("external_id", externalIdArray))
                        if (!playerId.isNullOrBlank()) {
                            put("include_subscription_ids", org.json.JSONArray().put(playerId))
                        }
                        put("target_channel", "push")
                    },
                    onFailure = onFailure
                )
            }
            .addOnFailureListener { onFailure(it.message ?: "Failed to load target user") }
    }

    private fun sendOneSignal(
        title: String,
        message: String,
        type: String,
        extraData: Map<String, String>,
        targetBuilder: JSONObject.() -> Unit,
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        scope.launch {
            try {
                val connection = java.net.URL("https://api.onesignal.com/notifications").openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.setRequestProperty("Authorization", "Key ${AppConstants.ONESIGNAL_REST_API_KEY}")
                connection.doOutput = true
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val dataObj = JSONObject().apply {
                    put("type", type)
                    extraData.forEach { (k, v) -> put(k, v) }
                }
                val body = JSONObject().apply {
                    put("app_id", AppConstants.ONESIGNAL_APP_ID)
                    put("target_channel", "push")
                    targetBuilder()
                    put("headings", JSONObject().put("en", NoticeTextFormatter.stripMarkdown(title)))
                    put("contents", JSONObject().put("en", NoticeTextFormatter.stripMarkdown(message)))
                    put("data", dataObj)
                    put("android_accent_color", "FF4D9FFF")
                    put("priority", 10)
                    put("existing_android_channel_id", getChannelIdForType(type))
                    put("android_visibility", 1)
                }.toString()
                connection.outputStream.write(body.toByteArray(Charsets.UTF_8))
                connection.outputStream.flush()
                val responseCode = connection.responseCode
                val responseMessage = if (responseCode in 200..299) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                }
                android.util.Log.e("ONESIGNAL", "Response $responseCode: $responseMessage")
                withContext(Dispatchers.Main) {
                    if (responseCode in 200..299) onSuccess()
                    else onFailure("HTTP $responseCode: $responseMessage")
                }
            } catch (e: Exception) {
                android.util.Log.e("ONESIGNAL", "Exception", e)
                withContext(Dispatchers.Main) { onFailure(e.message ?: "Unknown error") }
            }
        }
    }

    fun sendAssignmentAlert(
        subject: String,
        topic: String,
        dueDate: String,
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) = sendToAll(
        title = "📝 New Assignment Posted",
        message = "Subject: $subject\nTopic: $topic\nDeadline: $dueDate\n\nClick to add a live countdown to your home screen!",
        type = "assignment",
        extraData = mapOf("subject" to subject, "topic" to topic),
        onSuccess = onSuccess,
        onFailure = onFailure
    )

    fun sendPollAlert(
        question: String,
        targetBatch: String = "all",
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) = sendToAll(
        title = "📊 New Poll",
        message = question,
        type = "poll",
        targetBatch = targetBatch,
        onSuccess = onSuccess,
        onFailure = onFailure
    )

    fun sendResourceAlert(
        title: String,
        subject: String,
        targetBatch: String = "all",
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) = sendToAll(
        title = "📚 New Resource: $title",
        message = "Subject: $subject\n\nA new resource has been added. Click to view!",
        type = "resource",
        extraData = mapOf("subject" to subject, "title" to title),
        targetBatch = targetBatch,
        onSuccess = onSuccess,
        onFailure = onFailure
    )

    fun sendNoticeAlert(
        title: String,
        body: String,
        noticeId: String? = null,
        targetBatch: String = "all",
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) = sendToAll(
        title = "📢 $title",
        message = body,
        type = "notice",
        extraData = if (noticeId != null) mapOf("noticeId" to noticeId) else emptyMap(),
        targetBatch = targetBatch,
        onSuccess = onSuccess,
        onFailure = onFailure
    )

    fun sendCancellationAlert(
        subject: String,
        whenText: String,
        noticeId: String? = null,
        day: String = "",
        targetBatch: String = "all",
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) = sendToAll(
        title = "Class Cancelled",
        message = "$subject class cancelled for $whenText",
        type = "cancellation",
        extraData = mutableMapOf("subject" to subject, "day" to day).apply {
            if (noticeId != null) put("noticeId", noticeId)
        },
        targetBatch = targetBatch,
        onSuccess = onSuccess,
        onFailure = onFailure
    )

    fun sendSubstituteAlert(
        subject: String,
        substituteTeacher: String,
        whenText: String,
        noticeId: String? = null,
        day: String = "",
        targetBatch: String = "all",
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) = sendToAll(
        title = "🔄 Substitute Class",
        message = "$subject will be taken by $substituteTeacher $whenText",
        type = "substitute",
        extraData = mutableMapOf(
            "subject" to subject,
            "teacher" to substituteTeacher,
            "day" to day
        ).apply {
            if (noticeId != null) put("noticeId", noticeId)
        },
        targetBatch = targetBatch,
        onSuccess = onSuccess,
        onFailure = onFailure
    )

    fun sendScheduleUpdateAlert(
        subject: String,
        details: String,
        day: String = "",
        targetBatch: String = "all",
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) = sendToAll(
        title = "⏰ Class Time/Room Updated",
        message = "$subject: $details",
        type = "schedule_update",
        extraData = mapOf("subject" to subject, "day" to day),
        targetBatch = targetBatch.ifBlank { "all" },
        onSuccess = onSuccess,
        onFailure = onFailure
    )

    fun sendHolidayAlert(
        title: String,
        reason: String,
        dateRange: String,
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) = sendToAll(
        title = "📅 $title",
        message = if (reason.isNotBlank()) "$reason\nDate: $dateRange" else "Date: $dateRange",
        type = "notice",
        onSuccess = onSuccess,
        onFailure = onFailure
    )

    fun sendRegistrationAlert(
        userName: String,
        studentId: String,
        onFailure: (String) -> Unit = {}
    ) {
        sendOneSignal(
            title = "New Student Registration",
            message = "$userName ($studentId) registered and is waiting for approval.",
            type = "admin_alert",
            extraData = emptyMap(),
            targetBuilder = {
                val filters = org.json.JSONArray().apply {
                    put(JSONObject().put("field", "tag").put("key", "role").put("relation", "=").put("value", "admin"))
                    put(JSONObject().put("operator", "OR"))
                    put(JSONObject().put("field", "tag").put("key", "role").put("relation", "=").put("value", "superadmin"))
                }
                put("filters", filters)
            },
            onFailure = onFailure
        )
    }
}
