package com.shuaib.classmate.utils

import android.content.Context
import com.google.firebase.Timestamp

object NoticeReadTracker {
    private const val PREFS = "notice_read_state"
    private const val KEY_LAST_READ_MS = "last_notice_read_ms"

    fun lastReadMillis(context: Context): Long {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val stored = sp.getLong(KEY_LAST_READ_MS, 0L)
        if (stored == 0L) {
            val now = System.currentTimeMillis()
            sp.edit().putLong(KEY_LAST_READ_MS, now).apply()
            return now
        }
        return stored
    }

    fun markReadNow(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_READ_MS, System.currentTimeMillis())
            .apply()
    }

    fun isUnread(context: Context, timestamp: Timestamp?): Boolean {
        val noticeMillis = timestamp?.toDate()?.time ?: return false
        return noticeMillis > lastReadMillis(context)
    }
}
