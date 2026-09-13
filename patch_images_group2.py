import re

with open("app/src/main/java/com/shuaib/classmate/chat/GroupChatFragment.kt", "r") as f:
    content = f.read()

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
                viewModel.sendImage(com.shuaib.classmate.chat.ChatRepository.groupRoomId, url, caption)
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
                    viewModel.sendImage(com.shuaib.classmate.chat.ChatRepository.groupRoomId, url, caption)
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

with open("app/src/main/java/com/shuaib/classmate/chat/GroupChatFragment.kt", "w") as f:
    f.write(content)
