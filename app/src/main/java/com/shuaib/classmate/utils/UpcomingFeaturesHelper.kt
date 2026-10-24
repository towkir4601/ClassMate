package com.shuaib.classmate.utils

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AlertDialog
import com.shuaib.classmate.R
import com.shuaib.classmate.activities.CvMakerActivity

object UpcomingFeaturesHelper {

    fun showCvMakerDialog(context: Context) {
        AlertDialog.Builder(context, R.style.Theme_ClassMate_Dialog)
            .setTitle("📄 Professional CV Maker (Beta)")
            .setMessage("Create ATS-friendly academic and corporate CVs in seconds with live preview, customized templates, and instant PDF download.\n\nStatus: Beta Available.\n\nWould you like to launch the CV Maker Beta right now?")
            .setPositiveButton("Launch Beta CV Maker") { _, _ ->
                context.startActivity(Intent(context, CvMakerActivity::class.java))
            }
            .setNegativeButton("Close", null)
            .show()
    }

    fun showLiveBusTrackingDialog(context: Context) {
        AlertDialog.Builder(context, R.style.Theme_ClassMate_Dialog)
            .setTitle("🚌 Live Bus GPS Tracking")
            .setMessage("We are currently outfitting GSTU campus shuttles with live GPS telemetry. You will be able to see the live position of the bus on the map, speed, and real-time arrival estimates (ETA) for each campus stoppage.\n\nStatus: Under Active Development.")
            .setPositiveButton("Got It", null)
            .show()
    }

    fun showCafeteriaMenuDialog(context: Context) {
        AlertDialog.Builder(context, R.style.Theme_ClassMate_Dialog)
            .setTitle("🍽️ GSTU Cafeteria Live Menu")
            .setMessage("Check daily lunch, breakfast, and snack menus with real-time pricing directly from GSTU central cafeteria. Features will also include online meal token purchase to avoid canteen queues.\n\nStatus: Coming Soon.")
            .setPositiveButton("Got It", null)
            .show()
    }

    fun showHallAlumniDialog(context: Context) {
        AlertDialog.Builder(context, R.style.Theme_ClassMate_Dialog)
            .setTitle("🏛️ Hall Seat & Alumni Network")
            .setMessage("Apply for dormitory hall seat allocation digitally, track allotment seniority, and connect with verified GSTU alumni across tech, banking, and government sectors for mentorship.\n\nStatus: Planned in Roadmap.")
            .setPositiveButton("Got It", null)
            .show()
    }
}
