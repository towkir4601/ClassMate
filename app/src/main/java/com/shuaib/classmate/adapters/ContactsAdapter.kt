package com.shuaib.classmate.adapters

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.shuaib.classmate.databinding.ItemContactCardBinding
import com.shuaib.classmate.models.User

class ContactsAdapter(
    private var contacts: List<User>
) : RecyclerView.Adapter<ContactsAdapter.ContactViewHolder>() {

    inner class ContactViewHolder(val binding: ItemContactCardBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val binding = ItemContactCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ContactViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val user = contacts[position]
        val b = holder.binding
        val context = holder.itemView.context

        b.tvContactName.text = user.fullName.ifBlank { user.name.ifBlank { "Faculty Member" } }
        val roleStr = when (user.role.lowercase()) {
            "teacher" -> "Teacher"
            "admin" -> "Admin / CR"
            "superadmin" -> "Super Admin"
            else -> "Staff"
        }
        val dept = user.department.ifBlank { "University" }
        b.tvContactRole.text = "$roleStr · $dept"
        b.tvRoleBadge.text = roleStr.uppercase()

        b.tvContactEmail.text = user.email.ifBlank { "No email listed" }

        b.btnCallContact.setOnClickListener {
            val phone = user.phone.ifBlank { user.whatsappNumber }
            if (phone.isNotBlank()) {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                context.startActivity(intent)
            } else {
                Toast.makeText(context, "No phone number available", Toast.LENGTH_SHORT).show()
            }
        }

        b.btnEmailContact.setOnClickListener {
            if (user.email.isNotBlank()) {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:${user.email}")
                }
                context.startActivity(intent)
            } else {
                Toast.makeText(context, "No email address available", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun getItemCount(): Int = contacts.size

    fun updateList(newList: List<User>) {
        contacts = newList
        notifyDataSetChanged()
    }
}
