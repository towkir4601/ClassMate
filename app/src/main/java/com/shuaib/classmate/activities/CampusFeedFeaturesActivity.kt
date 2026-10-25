package com.shuaib.classmate.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.shuaib.classmate.R
import com.shuaib.classmate.databinding.ActivityCampusFeedFeaturesBinding
import com.shuaib.classmate.utils.UserRoleHelper


class CampusFeedFeaturesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCampusFeedFeaturesBinding
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCampusFeedFeaturesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        setupAdminAccess()
    }

    private fun setupAdminAccess() {
        val role = UserRoleHelper.getEffectiveRole(this)
        val perms = UserRoleHelper.getEffectivePermissions(this, role)
        val r = role.trim().lowercase()
        val isAdmin = (r == "superadmin" || r == "admin" || r == "teacher" || r == "club_admin" || r == "cr" || perms.values.any { it == true })

        if (isAdmin) {
            binding.layoutAdminBetaBar.visibility = View.VISIBLE
            binding.btnLaunchLiveFeed.setOnClickListener {
                startActivity(Intent(this, CampusFeedActivity::class.java))
            }
        } else {
            binding.layoutAdminBetaBar.visibility = View.GONE
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnShareFeatures.setOnClickListener {
            shareFeatureRoadmap()
        }

        binding.btnNotifyMe.setOnClickListener {
            registerForNotification()
        }

        binding.btnSuggestFeature.setOnClickListener {
            showSuggestFeatureDialog()
        }
    }

    private fun registerForNotification() {
        val uid = auth.currentUser?.uid ?: ""
        val email = auth.currentUser?.email ?: "anonymous"
        val name = auth.currentUser?.displayName ?: "Student"

        // Store interest in SharedPreferences
        getSharedPreferences("classmate_prefs", MODE_PRIVATE)
            .edit()
            .putBoolean("campus_feed_notified", true)
            .apply()

        // Record in Firestore collection 'campus_feed_waitlist' if possible
        if (uid.isNotEmpty()) {
            val waitlistData = hashMapOf(
                "uid" to uid,
                "email" to email,
                "name" to name,
                "timestamp" to FieldValue.serverTimestamp()
            )
            firestore.collection("campus_feed_waitlist")
                .document(uid)
                .set(waitlistData)
                .addOnSuccessListener {
                    // Success
                }
                .addOnFailureListener {
                    // Non-critical failure fallback
                }
        }

        Toast.makeText(
            this,
            "🎉 You're on the priority notification list! We'll alert you the moment CampusFeed launches.",
            Toast.LENGTH_LONG
        ).show()

        binding.btnNotifyMe.text = "✓ You're on the Priority List!"
        binding.btnNotifyMe.isEnabled = false
    }

    private fun showSuggestFeatureDialog() {
        val input = EditText(this).apply {
            hint = "E.g., Batch anonymous confession box, CGPA tracker integration, etc."
            setPadding(40, 32, 40, 32)
            textSize = 14f
            setTextColor(getColor(R.color.cm_text_primary))
            setHintTextColor(getColor(R.color.cm_text_muted))
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("💡 Suggest a Feature for CampusFeed")
            .setMessage("Have an idea that would make CampusFeed more useful for GSTU students? Let the developers know!")
            .setView(input)
            .setPositiveButton("Submit") { dialog, _ ->
                val suggestion = input.text.toString().trim()
                if (suggestion.isNotBlank()) {
                    val uid = auth.currentUser?.uid ?: "anonymous"
                    val data = hashMapOf(
                        "uid" to uid,
                        "email" to (auth.currentUser?.email ?: ""),
                        "suggestion" to suggestion,
                        "timestamp" to FieldValue.serverTimestamp()
                    )
                    firestore.collection("campus_feed_suggestions").add(data)
                    Toast.makeText(this, "Thank you! Your suggestion has been shared with the developers.", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun shareFeatureRoadmap() {
        val shareText = """
            🚀 Exciting news for GSTU students!
            
            GSTU Go is introducing *CampusFeed* — a dedicated student community platform:
            💬 Post & Share Opinions
            📊 Real-time Campus Polls
            🎯 Interest Circles & Teams
            📜 Digital Student Petitions
            
            Check it out now on GSTU Go App!
        """.trimIndent()

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        startActivity(Intent.createChooser(sendIntent, "Share CampusFeed Roadmap"))
    }
}
