package com.shuaib.classmate.utils

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.shuaib.classmate.R

object GuestUserHelper {

    /**
     * Checks if the active session is anonymous / guest:
     * - Either AppPreferences has isGuestMode() == true
     * - Or FirebaseAuth.getInstance().currentUser == null
     * - Or currentUser.isAnonymous == true
     */
    fun isGuest(context: Context): Boolean {
        if (AppPreferences(context).isGuestMode()) return true
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        return firebaseUser == null || firebaseUser.isAnonymous
    }

    /**
     * Checks if the department routine has been added for the user.
     * Currently in GSTU Go, only Computer Science & Engineering (CSE) routine has been added.
     * Other departments or anonymous/guest users do not have an initialized routine yet.
     */
    fun isDepartmentRoutineAdded(context: Context): Boolean {
        if (isGuest(context)) return false
        val prefs = AppPreferences(context)
        val dept = prefs.getUserDepartment().trim().lowercase()
        val deptId = prefs.getUserDepartmentId().trim().lowercase()
        if (dept.isBlank() && deptId.isBlank()) return false

        return deptId == "cse" ||
                deptId.contains("computer") ||
                dept.contains("cse") ||
                dept.contains("computer science")
    }

    /**
     * Checks if a specific period belongs to the user's department:
     * - If user is guest: returns false (guest has no department routine)
     * - If user is CSE: matches CSE periods (explicitly tagged or legacy blank) or 'all'
     * - If user is other department: matches only explicit department tag or 'all'
     */
    fun doesPeriodMatchUserDepartment(context: Context, periodDepartmentId: String, periodDepartment: String): Boolean {
        if (isGuest(context)) return false
        val prefs = AppPreferences(context)
        val userDept = prefs.getUserDepartment().trim().lowercase()
        val userDeptId = prefs.getUserDepartmentId().trim().lowercase()
        if (userDept.isBlank() && userDeptId.isBlank()) return false

        val pDeptId = periodDepartmentId.trim().lowercase()
        val pDept = periodDepartment.trim().lowercase()

        if (pDeptId == "all" || pDept == "all") return true

        val userIsCse = userDeptId == "cse" || userDeptId.contains("computer") || userDept.contains("cse") || userDept.contains("computer science")
        val periodIsCse = pDeptId == "cse" || pDeptId.contains("computer") || pDept.contains("cse") || pDept.contains("computer science") || (pDeptId.isBlank() && pDept.isBlank())

        return if (userIsCse) {
            periodIsCse
        } else {
            (pDeptId.isNotBlank() && pDeptId == userDeptId) || (pDept.isNotBlank() && pDept == userDept)
        }
    }

    /**
     * Displays a friendly dialog explaining that this action requires a signed-in university account,
     * with an option to navigate to the Login screen.
     */
    fun promptLogin(context: Context, actionDescription: String = "perform this action") {
        MaterialAlertDialogBuilder(context, R.style.Theme_ClassMate_Dialog)
            .setTitle("Account Required")
            .setMessage("You are currently browsing as a guest. Please sign in with your GSTU account to $actionDescription.")
            .setPositiveButton("Sign In") { _, _ ->
                AppPreferences(context).setGuestMode(false)
                FirebaseAuth.getInstance().signOut()
                val intent = android.content.Intent(context, com.shuaib.classmate.activities.LoginActivity::class.java).apply {
                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                context.startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
