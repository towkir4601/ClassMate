// com/shuaib/classmate/fragments/FriendDetailFragment.kt
package com.shuaib.classmate.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.shuaib.classmate.R
import com.shuaib.classmate.activities.MainActivity
import com.shuaib.classmate.databinding.FragmentFriendDetailBinding
import com.shuaib.classmate.models.User

class FriendDetailFragment : Fragment() {

    private var _binding: FragmentFriendDetailBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFriendDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = FirebaseFirestore.getInstance()

        val userId = arguments?.getString("userId") ?: return
        fetchFriendDetails(userId)
    }

    private fun fetchFriendDetails(userId: String) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (!document.exists()) return@addOnSuccessListener
                
                try {
                    val user = User(
                        uid = document.id,
                        name = document.getString("name") ?: "",
                        fullName = document.getString("fullName") ?: "",
                        studentId = document.getString("studentId") ?: "",
                        department = document.getString("department") ?: "",
                        email = document.getString("email") ?: "",
                        phone = document.getString("phone") ?: "",
                        bloodGroup = document.getString("bloodGroup") ?: "",
                        homeDistrict = document.getString("homeDistrict") ?: "",
                        address = document.getString("address") ?: "",
                        role = document.getString("role") ?: "student",
                        approved = document.getBoolean("approved") ?: false,
                        photoUrl = document.getString("photoUrl") ?: "",
                        authProvider = document.getString("authProvider") ?: "",
                        createdAt = document.getTimestamp("createdAt"),
                        updatedAt = document.getTimestamp("updatedAt"),
                        oneSignalPlayerId = document.getString("oneSignalPlayerId") ?: "",
                        permissions = (document.get("permissions") as? Map<String, Boolean>) ?: User.DEFAULT_PERMISSIONS
                    )
                    updateUI(user)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
    }

    private fun updateUI(user: User) {
        binding.tvFriendDetailName.text = user.name
        binding.tvFriendDetailRole.text = user.role.uppercase()

        Glide.with(this)
            .load(user.photoUrl.ifEmpty { null })
            .placeholder(R.drawable.ic_default_avatar)
            .into(binding.ivFriendDetailProfile)

        binding.detailFriendId.tvLabel.text = "Student ID"
        binding.detailFriendId.tvValue.text = user.studentId

        binding.detailFriendEmail.tvLabel.text = "Email"
        binding.detailFriendEmail.tvValue.text = user.email

        binding.detailFriendPhone.tvLabel.text = "Phone"
        binding.detailFriendPhone.tvValue.text = user.phone

        binding.detailFriendBlood.tvLabel.text = "Blood Group"
        binding.detailFriendBlood.tvValue.text = user.bloodGroup

        binding.detailFriendDistrict.tvLabel.text = "Home District"
        binding.detailFriendDistrict.tvValue.text = user.homeDistrict

        binding.detailFriendAddress.tvLabel.text = "Address"
        binding.detailFriendAddress.tvValue.text = user.address

        binding.btnDetailChat.setOnClickListener {
            val myUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
            if (myUid.isNotEmpty() && user.uid.isNotEmpty()) {
                val roomId = listOf(myUid, user.uid).sorted().joinToString(separator = "_", prefix = "dm_")
                val bundle = bundleOf(
                    "roomId" to roomId,
                    "otherUserName" to user.name.ifBlank { "Chat" },
                    "otherUserId" to user.uid
                )
                (requireActivity() as? MainActivity)?.openChildDestination(
                    R.id.nav_chat,
                    R.id.fragment_dm_chat,
                    bundle
                ) ?: run {
                    try {
                        findNavController().navigate(R.id.action_detail_to_dm_chat, bundle)
                    } catch (_: Exception) {
                        try {
                            findNavController().navigate(R.id.fragment_dm_chat, bundle)
                        } catch (_: Exception) {}
                    }
                }
            }
        }

        binding.btnDetailCall.setOnClickListener {
            val cleanPhone = user.phone.filter { it.isDigit() || it == '+' }
            if (cleanPhone.isEmpty()) {
                Toast.makeText(context, "Phone number not available", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$cleanPhone")
            }
            startActivity(intent)
        }

        binding.btnDetailWhatsapp.setOnClickListener {
            val cleanPhone = user.phone.filter { it.isDigit() || it == '+' }
            if (cleanPhone.isEmpty()) {
                Toast.makeText(context, "Phone number not available", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val url = "https://api.whatsapp.com/send?phone=$cleanPhone"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
            }
            try {
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
