import re

with open("app/src/main/java/com/shuaib/classmate/chat/GroupChatFragment.kt", "r") as f:
    content = f.read()

# Update imagePicker
old_picker = """    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { showImagePreview(it) }
    }"""
new_picker = """    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            showImagePreview(uris)
        }
    }"""
content = content.replace(old_picker, new_picker)

# Update showImagePreview
old_show = """    private fun showImagePreview(uri: Uri) {
        val dialog = BottomSheetDialog(requireContext())
        val sheet = layoutInflater.inflate(R.layout.dialog_image_send_preview, null)
        val preview = sheet.findViewById<android.widget.ImageView>(R.id.ivPreviewImage)
        val caption = sheet.findViewById<EditText>(R.id.etCaption)
        val progress = sheet.findViewById<View>(R.id.progressUpload)
        val send = sheet.findViewById<View>(R.id.btnSendImage)
        val cancel = sheet.findViewById<View>(R.id.btnCancel)
        Glide.with(this).load(uri).centerCrop().into(preview)
        cancel.setOnClickListener { dialog.dismiss() }
        send.setOnClickListener {
            send.isEnabled = false
            cancel.isEnabled = false
            progress.isVisible = true
            uploadImage(uri, caption.text?.toString().orEmpty(), dialog, progress)
        }
        dialog.setContentView(sheet)
        dialog.show()
    }"""
new_show = """    private fun showImagePreview(uris: List<Uri>) {
        val dialog = BottomSheetDialog(requireContext())
        val sheet = layoutInflater.inflate(R.layout.dialog_image_send_preview, null)
        val preview = sheet.findViewById<android.widget.ImageView>(R.id.ivPreviewImage)
        val caption = sheet.findViewById<EditText>(R.id.etCaption)
        val progress = sheet.findViewById<View>(R.id.progressUpload)
        val send = sheet.findViewById<View>(R.id.btnSendImage)
        val cancel = sheet.findViewById<View>(R.id.btnCancel)
        Glide.with(this).load(uris.first()).centerCrop().into(preview)
        if (uris.size > 1) {
            caption.hint = "Caption for first image (Sending ${uris.size} total)"
        }
        cancel.setOnClickListener { dialog.dismiss() }
        send.setOnClickListener {
            send.isEnabled = false
            cancel.isEnabled = false
            progress.isVisible = true
            uploadImages(uris, caption.text?.toString().orEmpty(), dialog, progress)
        }
        dialog.setContentView(sheet)
        dialog.show()
    }"""
content = content.replace(old_show, new_show)

# Add uploadImages and remove uploadImage
old_upload = """    private fun uploadImage(uri: Uri, caption: String, dialog: BottomSheetDialog, progress: View) {
        binding.imageUploadOverlay.visibility = View.VISIBLE
        CloudinaryUploader.uploadImage(
            requireContext(),
            uri,
            "classmate/chat",
            { success, url ->
                progress.isVisible = false
                dialog.dismiss()
                binding.imageUploadOverlay.visibility = View.GONE
                if (success && url != null) {
                    viewModel.sendMessage(
                        roomId = viewModel.groupRoomId,
                        text = "",
                        caption = caption,
                        replyToId = replyMessageId,
                        replyToText = replyMessageText,
                        replyToSender = replyMessageSender,
                        imageUrl = url
                    )
                    clearReplyContext()
                } else {
                    Toast.makeText(requireContext(), "Image upload failed", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }"""
new_upload = """    private fun uploadImages(uris: List<Uri>, firstCaption: String, dialog: BottomSheetDialog, progress: View) {
        binding.imageUploadOverlay.visibility = View.VISIBLE
        dialog.dismiss()
        var completed = 0
        val total = uris.size
        uris.forEachIndexed { index, uri ->
            val caption = if (index == 0) firstCaption else ""
            CloudinaryUploader.uploadImage(
                requireContext(),
                uri,
                "classmate/chat",
                { success, url ->
                    if (success && url != null) {
                        viewModel.sendMessage(
                            roomId = viewModel.groupRoomId,
                            text = "",
                            caption = caption,
                            replyToId = replyMessageId,
                            replyToText = replyMessageText,
                            replyToSender = replyMessageSender,
                            imageUrl = url
                        )
                    } else {
                        Toast.makeText(requireContext(), "Image upload failed for one item", Toast.LENGTH_SHORT).show()
                    }
                    completed++
                    if (completed == total) {
                        binding.imageUploadOverlay.visibility = View.GONE
                        clearReplyContext()
                    }
                }
            )
        }
    }"""
content = content.replace(old_upload, new_upload)

with open("app/src/main/java/com/shuaib/classmate/chat/GroupChatFragment.kt", "w") as f:
    f.write(content)
