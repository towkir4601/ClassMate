package com.shuaib.classmate.adapters

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.shuaib.classmate.databinding.ItemBloodDonorBinding
import com.shuaib.classmate.models.User

class BloodDonorAdapter(
    private var donors: List<User>
) : RecyclerView.Adapter<BloodDonorAdapter.DonorViewHolder>() {

    inner class DonorViewHolder(val binding: ItemBloodDonorBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DonorViewHolder {
        val binding = ItemBloodDonorBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return DonorViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DonorViewHolder, position: Int) {
        val user = donors[position]
        val b = holder.binding
        val context = holder.itemView.context

        b.tvBloodGroup.text = user.bloodGroup.ifBlank { "O+" }
        b.tvDonorName.text = user.fullName.ifBlank { user.name.ifBlank { "GSTU Donor" } }

        val dept = user.department.ifBlank { "Department" }
        val batch = if (user.batch.isNotBlank()) " · ${user.batch} Batch" else ""
        b.tvDonorDeptBatch.text = "$dept$batch"

        val location = user.homeDistrict.ifBlank { user.presentAddress.ifBlank { "Campus" } }
        b.tvDonorLocation.text = location

        b.btnCallDonor.setOnClickListener {
            val phone = user.phone.ifBlank { user.whatsappNumber }
            if (phone.isNotBlank()) {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                context.startActivity(intent)
            } else {
                Toast.makeText(context, "No phone number available for this donor", Toast.LENGTH_SHORT).show()
            }
        }

        b.btnWhatsAppDonor.setOnClickListener {
            val rawNum = user.whatsappNumber.ifBlank { user.phone }
            if (rawNum.isNotBlank()) {
                openWhatsApp(context, rawNum)
            } else {
                Toast.makeText(context, "No WhatsApp number available for this donor", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openWhatsApp(context: Context, rawNum: String) {
        val cleanNum = rawNum.replace("+", "").replace("-", "").replace(" ", "").trim()
        val numWithCountry = if (cleanNum.startsWith("01")) "88$cleanNum" else cleanNum
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://api.whatsapp.com/send?phone=$numWithCountry&text=Hello%2C%20I%20found%20your%20contact%20via%20GSTU%20Go%20Blood%20Network.")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open WhatsApp: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount(): Int = donors.size

    fun updateList(newList: List<User>) {
        donors = newList
        notifyDataSetChanged()
    }
}
