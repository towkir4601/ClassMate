package com.shuaib.classmate.chat

import android.content.Context
import android.content.SharedPreferences

object ChatUnreadManager {
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.getSharedPreferences("chat_unread_prefs", Context.MODE_PRIVATE)
    }

    fun markRoomSeen(roomId: String, timestamp: Long = System.currentTimeMillis()) {
        prefs?.edit()?.putLong("seen_$roomId", timestamp)?.apply()
    }

    fun getUnreadCount(roomId: String, lastRoomTimestamp: Long): Int {
        val seen = prefs?.getLong("seen_$roomId", 0L) ?: 0L
        return if (lastRoomTimestamp > seen) 1 else 0
    }
    
    fun getTotalUnread(rooms: List<com.shuaib.classmate.chat.model.ChatRoom>, currentUserId: String): Int {
        var count = 0
        for (room in rooms) {
            if (room.lastSenderId == currentUserId) continue
            if (getUnreadCount(room.id, room.lastMessageTime) > 0) {
                count++
            }
        }
        return count
    }
}
