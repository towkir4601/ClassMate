package com.shuaib.classmate.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.shuaib.classmate.databinding.ActivityPendingApprovalBinding

class PendingApprovalActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPendingApprovalBinding
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPendingApprovalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvEmail.text = auth.currentUser?.email ?: ""

        updateUI()

        binding.btnResendEmail.setOnClickListener {
            auth.currentUser?.sendEmailVerification()?.addOnSuccessListener {
                Toast.makeText(this, "Verification email sent!", Toast.LENGTH_SHORT).show()
            }?.addOnFailureListener {
                Toast.makeText(this, "Failed to send email: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnSignOut.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finishAffinity()
        }
    }

    override fun onResume() {
        super.onResume()
        auth.currentUser?.reload()?.addOnCompleteListener {
            updateUI()
        }
    }

    private fun updateUI() {
        val user = auth.currentUser ?: return
        
        if (!user.isEmailVerified) {
            binding.tvTitle.text = "Email Verification Required"
            binding.tvMessage.text = "A verification link has been sent to your email. Please check your inbox and click the link to verify your account.\n\nAfter verifying, return to this screen to check your approval status."
            binding.btnResendEmail.visibility = View.VISIBLE
        } else {
            binding.tvTitle.text = "Account Pending Approval"
            binding.tvMessage.text = "Your email has been verified!\n\nPlease wait for an admin to approve your account before you can access the app. You can check back later."
            binding.btnResendEmail.visibility = View.GONE
            
            // Check if they are actually approved now
            firestore.collection("users").document(user.uid).get().addOnSuccessListener { doc ->
                if (doc.getBoolean("approved") == true) {
                    startActivity(Intent(this, MainActivity::class.java))
                    finishAffinity()
                }
            }
        }
    }
}
