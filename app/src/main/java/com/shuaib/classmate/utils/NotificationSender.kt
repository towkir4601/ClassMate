/*
 * C:/Users/USER/AndroidStudioProjects/ClassMate/app/src/main/java/com/shuaib/classmate/utils/NotificationSender.kt
 */
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
        val targetBuilder: org.json.JSONObject.() -> Unit = if (targetBatch.isNotBlank() && targetBatch != "all") {
            {
                val filters = org.json.JSONArray().apply {
                    put(org.json.JSONObject().put("field", "tag").put("key", "batch").put("relation", "=").put("value", targetBatch))
                }
                put("filters", filters)
            }
        } else {
            { put("included_segments", org.json.JSONArray().put("All")) }
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

    fun sendToPlayers(
        playerIds: List<String>,
        title: String,
        message: String,
        type: String,
        extraData: Map<String, String> = emptyMap(),
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        if (playerIds.isEmpty()) return
        sendOneSignal(
            title = title,
            message = message,
            type = type,
            extraData = extraData,
            targetBuilder = { put("include_subscription_ids", org.json.JSONArray(playerIds)) },
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
        }

        val uid = targetUserId ?: return
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
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
            }
            .addOnFailureListener { onFailure(it.message ?: "Failed to load target user") }
    }

    private fun sendToExternalUser(
        externalUserId: String,
        title: String,
        message: String,
        type: String,
        extraData: Map<String, String> = emptyMap(),
        onFailure: (String) -> Unit = {}
    ) {
        sendOneSignal(
            title = title,
            message = message,
            type = type,
            extraData = extraData,
            targetBuilder = {
                put("include_aliases", JSONObject().put("external_id", org.json.JSONArray().put(externalUserId)))
                put("target_channel", "push")
            },
            onFailure = onFailure
        )
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

    // New Assignment Alert
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

    // New Poll Alert
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

    // New resource alert
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

    // Normal notice
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

    // Class cancellation
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

    // Substitute
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

    // Academic Calendar Holiday Alert
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

    // Registration alert for Admins
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
