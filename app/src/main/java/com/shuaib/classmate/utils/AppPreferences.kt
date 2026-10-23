// com/shuaib/classmate/utils/AppPreferences.kt
package com.shuaib.classmate.utils

import android.content.Context

class AppPreferences(context: Context) {

    private val prefs = context.getSharedPreferences(
        "classmate_prefs",
        Context.MODE_PRIVATE
    )

    fun isOnboardingComplete(): Boolean =
        prefs.getBoolean("onboarding_complete", false)

    fun setOnboardingComplete() {
        prefs.edit()
            .putBoolean("onboarding_complete", true)
            .apply()
    }

    fun getUserBatch(): String =
        prefs.getString("user_batch", "") ?: ""

    fun setUserBatch(batch: String) {
        prefs.edit()
            .putString("user_batch", batch)
            .apply()
    }

    fun getTestRoleOverride(): String? =
        prefs.getString("test_role_override", null)?.takeIf { it.isNotBlank() }

    fun setTestRoleOverride(role: String?) {
        val editor = prefs.edit()
        if (role.isNullOrBlank()) {
            editor.remove("test_role_override")
        } else {
            editor.putString("test_role_override", role.lowercase().trim())
        }
        editor.apply()
    }

    fun getUserRole(): String {
        val override = getTestRoleOverride()
        if (!override.isNullOrBlank()) return override
        return prefs.getString("user_role", "student") ?: "student"
    }

    fun setUserRole(role: String) {
        prefs.edit()
            .putString("user_role", role)
            .apply()
    }

    fun getUserName(): String =
        prefs.getString("user_name", "") ?: ""

    fun setUserName(name: String) {
        prefs.edit()
            .putString("user_name", name)
            .apply()
    }

    fun getUserSemester(): String =
        prefs.getString("user_semester", "") ?: ""

    fun setUserSemester(semester: String) {
        prefs.edit()
            .putString("user_semester", semester)
            .apply()
    }

    fun getTeacherCode(): String =
        prefs.getString("teacher_code", "") ?: ""

    fun setTeacherCode(code: String) {
        prefs.edit()
            .putString("teacher_code", code)
            .apply()
    }

    fun isDarkMode(): Boolean =
        prefs.getBoolean("dark_mode", false)

    fun setDarkMode(enabled: Boolean) {
        prefs.edit()
            .putBoolean("dark_mode", enabled)
            .apply()
    }

    fun isNotificationsEnabled(): Boolean =
        prefs.getBoolean("notifications_enabled", true)

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit()
            .putBoolean("notifications_enabled", enabled)
            .apply()
    }

    fun isAutoMuteEnabled(): Boolean =
        prefs.getBoolean("auto_mute_enabled", false)

    fun setAutoMuteEnabled(enabled: Boolean) {
        prefs.edit()
            .putBoolean("auto_mute_enabled", enabled)
            .apply()
    }

    fun isShakeToTorchEnabled(): Boolean =
        prefs.getBoolean("shake_to_torch_enabled", false)

    fun setShakeToTorchEnabled(enabled: Boolean) {
        prefs.edit()
            .putBoolean("shake_to_torch_enabled", enabled)
            .apply()
    }

    fun getSavedRingerMode(): Int =
        prefs.getInt("saved_ringer_mode", 2) // AudioManager.RINGER_MODE_NORMAL is 2

    fun setSavedRingerMode(mode: Int) {
        prefs.edit()
            .putInt("saved_ringer_mode", mode)
            .apply()
    }

    fun isGeminiPrimary(): Boolean = true

    fun setGeminiPrimary(enabled: Boolean) {
        prefs.edit()
            .putBoolean("is_gemini_primary", enabled)
            .apply()
    }

    fun getGeminiModel(): String {
        return "gemini-3.5-flash-lite"
    }

    fun setGeminiModel(model: String) {
        prefs.edit()
            .putString("gemini_model", model)
            .apply()
    }

    fun getGroqModel(): String =
        prefs.getString("groq_model", "llama-3.3-70b-versatile") ?: "llama-3.3-70b-versatile"

    fun setGroqModel(model: String) {
        prefs.edit()
            .putString("groq_model", model)
            .apply()
    }

    fun getDeletedLostFoundIds(): Set<String> =
        prefs.getStringSet("deleted_lost_found_ids", emptySet()) ?: emptySet()

    fun addDeletedLostFoundId(id: String) {
        val current = getDeletedLostFoundIds().toMutableSet()
        current.add(id)
        prefs.edit()
            .putStringSet("deleted_lost_found_ids", current)
            .apply()
    }

    fun isGuestMode(): Boolean =
        prefs.getBoolean("is_guest_mode", false)

    fun setGuestMode(enabled: Boolean) {
        prefs.edit()
            .putBoolean("is_guest_mode", enabled)
            .apply()
    }

    fun hasViewedPost(postId: String): Boolean {
        val viewed = prefs.getStringSet("viewed_post_ids", emptySet()) ?: emptySet()
        return viewed.contains(postId)
    }

    fun markPostViewed(postId: String) {
        val current = (prefs.getStringSet("viewed_post_ids", emptySet()) ?: emptySet()).toMutableSet()
        current.add(postId)
        prefs.edit()
            .putStringSet("viewed_post_ids", current)
            .apply()
    }
}

