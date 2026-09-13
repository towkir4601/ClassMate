package com.shuaib.classmate.notices

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.shuaib.classmate.activities.MainActivity
import com.shuaib.classmate.chat.ChatRepository
import com.shuaib.classmate.databinding.BottomSheetShareForwardNoticeBinding
import com.shuaib.classmate.models.Notice

class ShareForwardNoticeBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetShareForwardNoticeBinding? = null
    private val binding get() = _binding!!
    private var notice: Notice? = null
    var onShareRecorded: (() -> Unit)? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetShareForwardNoticeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnClose.setOnClickListener { dismiss() }
        binding.rowAnotherGroup.alpha = 0.45f
        binding.rowAnotherGroup.isEnabled = false
        loadNotice()
    }

    private fun loadNotice() {
        val noticeId = requireArguments().getString(ARG_NOTICE_ID).orEmpty()
        FirebaseFirestore.getInstance().collection("notices").document(noticeId).get()
            .addOnSuccessListener { doc ->
                if (_binding == null) return@addOnSuccessListener
                notice = NoticeUi.parseNotice(doc)
                bindNotice(notice!!)
                setupActions(notice!!)
            }
            .addOnFailureListener {
                dismiss()
            }
    }

    private fun bindNotice(notice: Notice) {
        binding.tvPreviewType.text = NoticeUi.typeLabel(notice.displayType)
        binding.tvPreviewTitle.text = notice.title
        binding.tvPreviewMeta.text = "${NoticeUi.formatDate(notice.createdAt)} - ${notice.displaySubject}"
    }

    private fun checkUserRole() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (_binding == null) return@addOnSuccessListener
                val role = doc.getString("role") ?: "student"
                if (role == "superadmin") {
                    binding.btnEditThorffinLink.visibility = View.VISIBLE
                    binding.btnEditThorffinLink.setOnClickListener {
                        showEditThorffinLinkDialog()
                    }
                }
            }
    }

    private fun showEditThorffinLinkDialog() {
        val input = android.widget.EditText(requireContext()).apply {
            hint = "https://chat.whatsapp.com/..."
            setPadding(50, 50, 50, 50)
        }
        
        FirebaseFirestore.getInstance().collection("config").document("thorffin_link").get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    input.setText(doc.getString("url").orEmpty())
                }
            }

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Thorffin-14 Link")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newUrl = input.text.toString().trim()
                FirebaseFirestore.getInstance().collection("config").document("thorffin_link")
                    .set(mapOf("url" to newUrl))
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Link updated", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupActions(notice: Notice) {
        checkUserRole()
        
        binding.rowClassGroup.setOnClickListener {
            NoticeShareManager.recordShare(notice.id, "class_group")
            onShareRecorded?.invoke()
            FirebaseFirestore.getInstance().collection("config").document("thorffin_link").get()
                .addOnSuccessListener { doc ->
                    val url = doc.getString("url").orEmpty()
                    if (url.isNotBlank()) {
                        try {
                            startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)))
                        } catch (e: Exception) {
                            Toast.makeText(requireContext(), "Invalid link", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(requireContext(), "Thorffin-14 link is not set yet", Toast.LENGTH_SHORT).show()
                    }
                    dismiss()
                }
        }
        binding.rowSendDm.setOnClickListener { showDmSelector(notice) }
        binding.rowCopyLink.setOnClickListener {
            val link = "classmate://notice/${notice.id}"
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("notice link", link))
            NoticeShareManager.recordShare(notice.id, "copy_link")
            onShareRecorded?.invoke()
            Toast.makeText(requireContext(), "Notice link copied", Toast.LENGTH_SHORT).show()
            dismiss()
        }
        binding.rowMoreOptions.setOnClickListener {
            NoticeShareManager.recordShare(notice.id, "android_share")
            onShareRecorded?.invoke()
            startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND)
                        .setType("text/plain")
                        .putExtra(Intent.EXTRA_TEXT, NoticeForwardManager.shareText(notice)),
                    "Share notice"
                )
            )
            dismiss()
        }
    }

    private fun showDmSelector(notice: Notice) {
        ChatRepository.getUsers()
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        val users = ChatRepository.users.value.filterNot { it.id == currentUserId }
        if (users.isEmpty()) {
            Toast.makeText(requireContext(), "No chat users loaded yet", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Send to DM")
            .setItems(users.map { it.name }.toTypedArray()) { _, which ->
                val user = users[which]
                ChatRepository.createDm(user.id, user.name)
                val roomId = ChatRepository.dmRoomIdFor(user.id)
                NoticeForwardManager.forwardToRoom(roomId, notice)
                NoticeShareManager.recordShare(notice.id, "dm")
                onShareRecorded?.invoke()
                Toast.makeText(requireContext(), "Forwarded to ${user.name}", Toast.LENGTH_SHORT).show()
                openChat(roomId, "dm", user.name, user.id)
                dismiss()
            }
            .show()
    }

    private fun openChat(roomId: String, roomType: String, name: String, userId: String?) {
        startActivity(
            Intent(requireContext(), MainActivity::class.java)
                .putExtra("roomId", roomId)
                .putExtra("roomType", roomType)
                .putExtra("senderName", name)
                .putExtra("senderId", userId)
        )
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "ShareForwardNoticeBottomSheet"
        private const val ARG_NOTICE_ID = "noticeId"
        

        fun newInstance(noticeId: String): ShareForwardNoticeBottomSheet {
            return ShareForwardNoticeBottomSheet().apply {
                arguments = Bundle().apply { putString(ARG_NOTICE_ID, noticeId) }
            }
        }
    }
}
