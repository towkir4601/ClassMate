package com.shuaib.classmate.utils

import android.content.Context
import com.shuaib.classmate.models.User

/**
 * GSTU Go - Utility for resolving effective user roles and permissions.
 * Supports active test/demo overrides set from the Profile screen so developers
 * and testers can preview any role without being blocked by server-side rule delays.
 */
object UserRoleHelper {

    /**
     * Returns the currently active role. If a test role override has been selected
     * in the Profile screen, it takes precedence. Otherwise falls back to [docRole] or "student".
     */
    fun getEffectiveRole(context: Context, docRole: String? = null): String {
        val testRole = AppPreferences(context).getTestRoleOverride()
        if (!testRole.isNullOrBlank()) {
            return testRole.trim().lowercase()
        }
        val cleanDocRole = docRole?.trim()?.lowercase()
        if (!cleanDocRole.isNullOrBlank()) {
            return cleanDocRole
        }
        return AppPreferences(context).getUserRole()
    }

    /**
     * Returns the effective permissions map for the current user.
     * When a test role is active, returns the pre-configured permission defaults for that role.
     */
    @Suppress("UNCHECKED_CAST")
    fun getEffectivePermissions(
        context: Context,
        effectiveRole: String,
        docPermissions: Any? = null
    ): Map<String, Boolean> {
        val testRole = AppPreferences(context).getTestRoleOverride()
        if (!testRole.isNullOrBlank()) {
            return User.getRoleDefaultPermissions(testRole)
        }

        if (docPermissions is Map<*, *>) {
            val result = mutableMapOf<String, Boolean>()
            docPermissions.forEach { (k, v) ->
                if (k is String) {
                    result[k] = (v == true || v == "true")
                }
            }
            if (result.isNotEmpty()) return result
        }

        return User.getRoleDefaultPermissions(effectiveRole)
    }

    /**
     * True if a test role override is currently active on this device.
     */
    fun isTestRoleActive(context: Context): Boolean {
        return !AppPreferences(context).getTestRoleOverride().isNullOrBlank()
    }

    /**
     * Activates a test role on this device.
     */
    fun setTestRole(context: Context, role: String) {
        val clean = role.trim().lowercase()
        AppPreferences(context).setTestRoleOverride(clean)
        AppPreferences(context).setUserRole(clean)
    }

    /**
     * Clears the test role override, reverting to the authentic Firestore database role.
     */
    fun clearTestRole(context: Context) {
        AppPreferences(context).setTestRoleOverride(null)
    }
}
