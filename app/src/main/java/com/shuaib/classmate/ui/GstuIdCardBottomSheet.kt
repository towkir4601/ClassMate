package com.shuaib.classmate.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.shuaib.classmate.R
import com.shuaib.classmate.databinding.DialogGstuIdCardBinding
import com.shuaib.classmate.models.User
import java.io.File
import java.io.FileOutputStream

class GstuIdCardBottomSheet : BottomSheetDialogFragment() {

    private var _binding: DialogGstuIdCardBinding? = null
    private val binding get() = _binding!!

    private var user: User? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogGstuIdCardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindUserData()

        binding.btnShareImage.setOnClickListener {
            shareCardAsImage()
        }

        binding.btnShareText.setOnClickListener {
            shareCardAsText()
        }
    }

    private fun bindUserData() {
        val u = user ?: return
        val isTeacher = u.role.equals("teacher", true)

        val displayName = u.name.ifBlank { u.fullName.ifBlank { u.email.substringBefore("@") } }
        binding.tvCardName.text = displayName.ifBlank { "GSTU Member" }

        if (isTeacher) {
            binding.tvCardTypeBadge.text = "FACULTY IDENTITY CARD"
            binding.tvCardTypeBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#2563EB"))
            binding.tvCardIdLabel.text = "Teacher Code: "
            binding.tvCardIdValue.text = u.teacherCode.ifBlank { "FACULTY" }
            binding.tvCardBatch.text = "Role: Faculty Member"
            binding.tvCardSemester.isVisible = false
        } else {
            binding.tvCardTypeBadge.text = "STUDENT IDENTITY CARD"
            binding.tvCardTypeBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#059669"))
            binding.tvCardIdLabel.text = "Student ID: "
            binding.tvCardIdValue.text = u.studentId.ifBlank { "Pending ID" }
            binding.tvCardBatch.text = if (u.batch.isNotBlank()) "Batch: ${u.batch}th" else "Student"
            binding.tvCardSemester.text = if (u.semester.isNotBlank()) "• Sem: ${u.semester}" else ""
            binding.tvCardSemester.isVisible = u.semester.isNotBlank()
        }

        binding.tvCardDept.text = u.department.ifBlank { "Gopalganj Science and Technology University" }
        binding.tvCardEmail.text = u.email.ifBlank { "member@gstu.ac.bd" }

        val contactNumber = u.phone.ifBlank { u.whatsappNumber }
        if (contactNumber.isNotBlank()) {
            binding.tvCardPhone.text = "Tel: $contactNumber"
            binding.tvCardPhone.isVisible = true
        } else {
            binding.tvCardPhone.isVisible = false
        }

        if (u.bloodGroup.isNotBlank()) {
            binding.tvCardBlood.text = "🩸 ${u.bloodGroup}"
            binding.tvCardBlood.isVisible = true
        } else {
            binding.tvCardBlood.isVisible = false
        }

        val barcodeSeed = if (isTeacher) u.teacherCode.ifBlank { u.uid.take(8) } else u.studentId.ifBlank { u.uid.take(8) }
        binding.tvCardBarcodeText.text = "*GSTU-${barcodeSeed.uppercase()}*"

        if (u.photoUrl.isNotBlank()) {
            Glide.with(this)
                .load(u.photoUrl)
                .placeholder(R.drawable.ic_default_avatar)
                .error(R.drawable.ic_default_avatar)
                .circleCrop()
                .into(binding.ivCardPhoto)
        } else {
            binding.ivCardPhoto.setImageResource(R.drawable.ic_default_avatar)
        }
    }

    private fun shareCardAsImage() {
        val targetCard = binding.cardIdContainer
        try {
            val bitmap = Bitmap.createBitmap(targetCard.width, targetCard.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            targetCard.draw(canvas)

            val cachePath = File(requireContext().cacheDir, "images")
            cachePath.mkdirs()
            val imageFile = File(cachePath, "GSTU_ID_Card_${System.currentTimeMillis()}.png")
            val stream = FileOutputStream(imageFile)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.flush()
            stream.close()

            val contentUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.provider",
                imageFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "GSTU Go Official Identity Card")
                putExtra(Intent.EXTRA_TEXT, "🎓 Official Digital Member Identity Card • Gopalganj Science and Technology University (GSTU)")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Share GSTU Identity Card"))
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to export card image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareCardAsText() {
        val u = user ?: return
        val displayName = u.name.ifBlank { u.fullName.ifBlank { u.email.substringBefore("@") } }
        val cardText = buildString {
            appendLine("🎓 GOPALGANJ SCIENCE AND TECHNOLOGY UNIVERSITY")
            appendLine("══════════════════════════════════════════")
            appendLine("Name: ${displayName.ifBlank { "GSTU Member" }}")
            if (u.role.equals("teacher", true)) {
                appendLine("Role: Faculty Member")
                if (u.teacherCode.isNotBlank()) appendLine("Teacher Code: ${u.teacherCode}")
            } else {
                appendLine("Student ID: ${u.studentId.ifBlank { "N/A" }}")
                if (u.batch.isNotBlank()) appendLine("Batch: ${u.batch}th Batch")
                if (u.semester.isNotBlank()) appendLine("Semester: ${u.semester}")
            }
            if (u.department.isNotBlank()) appendLine("Department: ${u.department}")
            if (u.bloodGroup.isNotBlank()) appendLine("Blood Group: ${u.bloodGroup}")
            if (u.email.isNotBlank()) appendLine("Institutional Email: ${u.email}")
            if (u.phone.isNotBlank()) appendLine("Phone: ${u.phone}")
            appendLine("══════════════════════════════════════════")
            append("— Verified via GSTU Go Campus Ecosystem")
        }

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, cardText)
            type = "text/plain"
        }
        startActivity(Intent.createChooser(sendIntent, "Share Identity Card Info"))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "GstuIdCardBottomSheet"

        fun show(fragmentManager: FragmentManager, user: User) {
            val sheet = GstuIdCardBottomSheet().apply {
                this.user = user
            }
            sheet.show(fragmentManager, TAG)
        }
    }
}
