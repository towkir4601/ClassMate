import re

with open("app/src/main/java/com/shuaib/classmate/chat/DmChatFragment.kt", "r") as f:
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
        val caption = sheet.findViewById<android.widget.EditText>(R.id.etCaption)
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
        val caption = sheet.findViewById<android.widget.EditText>(R.id.etCaption)
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
            onSuccess = { url, _ ->
                if (_binding != null) binding.imageUploadOverlay.visibility = View.GONE
                progress.isVisible = false
                dialog.dismiss()
                forceScrollToBottom = true
                viewModel.sendImage(roomId, url, caption)
            },
            onFailure = {
                if (_binding != null) binding.imageUploadOverlay.visibility = View.GONE
                progress.isVisible = false
                dialog.dismiss()
                view?.let { root -> Snackbar.make(root, "Failed to send image, try again", Snackbar.LENGTH_LONG).show() }
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
                onSuccess = { url, _ ->
                    forceScrollToBottom = true
                    viewModel.sendImage(roomId, url, caption)
                    completed++
                    if (completed == total && _binding != null) {
                        binding.imageUploadOverlay.visibility = View.GONE
                    }
                },
                onFailure = {
                    view?.let { root -> Snackbar.make(root, "Failed to send an image", Snackbar.LENGTH_LONG).show() }
                    completed++
                    if (completed == total && _binding != null) {
                        binding.imageUploadOverlay.visibility = View.GONE
                    }
                }
            )
        }
    }"""
content = content.replace(old_upload, new_upload)

with open("app/src/main/java/com/shuaib/classmate/chat/DmChatFragment.kt", "w") as f:
    f.write(content)
