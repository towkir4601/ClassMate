package com.shuaib.classmate.adapters

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.shuaib.classmate.databinding.ItemContactCardBinding
import com.shuaib.classmate.models.ImportantContact

class ContactsAdapter(
    private var contacts: List<ImportantContact>,
    private var isSuperAdmin: Boolean = false,
    private val onDeleteContact: ((ImportantContact) -> Unit)? = null
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
        val contact = contacts[position]
        val b = holder.binding
        val context = holder.itemView.context

        b.tvContactName.text = contact.name.ifBlank { "University Contact" }
        b.ivVerifiedBadge.visibility = View.VISIBLE

        val subtext = if (contact.designation.isNotBlank() && contact.departmentOrOffice.isNotBlank()) {
            "${contact.designation} · ${contact.departmentOrOffice}"
        } else {
            contact.designation.ifBlank { contact.departmentOrOffice.ifBlank { "GSTU Campus" } }
        }
        b.tvContactRole.text = subtext

        // Style Category Badge
        val (badgeBgColor, badgeText) = when (contact.category) {
            ImportantContact.CATEGORY_EMERGENCY -> Pair("#EF4444", "🚨 EMERGENCY")
            ImportantContact.CATEGORY_ADMINISTRATION -> Pair("#2563EB", "🏛️ ADMIN")
            ImportantContact.CATEGORY_FACULTY -> Pair("#10B981", "👨‍🏫 FACULTY")
            ImportantContact.CATEGORY_MEDICAL -> Pair("#06B6D4", "🏥 MEDICAL")
            ImportantContact.CATEGORY_SECURITY -> Pair("#D97706", "🛡️ SECURITY")
            else -> Pair("#64748B", contact.category.uppercase())
        }
        b.tvRoleBadge.text = badgeText
        b.tvRoleBadge.setTextColor(Color.WHITE)
        b.tvRoleBadge.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 999f
            setColor(Color.parseColor(badgeBgColor))
        }

        // Phone display
        if (contact.phone.isNotBlank()) {
            b.tvContactPhone.visibility = View.VISIBLE
            b.tvContactPhone.text = contact.phone
            b.tvContactPhone.setOnClickListener {
                dialNumber(context, contact.phone)
            }
        } else {
            b.tvContactPhone.visibility = View.GONE
        }

        // Email display
        if (contact.email.isNotBlank()) {
            b.tvContactEmail.visibility = View.VISIBLE
            b.tvContactEmail.text = contact.email
            b.tvContactEmail.setOnClickListener {
                sendEmail(context, contact.email)
            }
        } else {
            b.tvContactEmail.visibility = View.GONE
        }

        // Super Admin Delete button
        if (isSuperAdmin && contact.id.isNotBlank() && !contact.id.startsWith("user_") && !contact.id.startsWith("default_")) {
            b.btnDeleteContact.visibility = View.VISIBLE
            b.btnDeleteContact.setOnClickListener {
                onDeleteContact?.invoke(contact)
            }
        } else {
            b.btnDeleteContact.visibility = View.GONE
        }

        // Call Action
        b.btnCallContact.setOnClickListener {
            if (contact.phone.isNotBlank()) {
                dialNumber(context, contact.phone)
            } else {
                Toast.makeText(context, "No phone number available for ${contact.name}", Toast.LENGTH_SHORT).show()
            }
        }

        // Email Action
        b.btnEmailContact.setOnClickListener {
            if (contact.email.isNotBlank()) {
                sendEmail(context, contact.email)
            } else {
                Toast.makeText(context, "No email address available for ${contact.name}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun dialNumber(context: Context, phone: String) {
        val phoneDigits = phone.filter { it.isDigit() || it == '+' }
        if (phoneDigits.isNotBlank()) {
            try {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneDigits"))
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Could not open dialer: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Invalid phone number format", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendEmail(context: Context, email: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$email")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open email client: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount(): Int = contacts.size

    fun updateList(newList: List<ImportantContact>, superAdmin: Boolean = isSuperAdmin) {
        contacts = newList
        isSuperAdmin = superAdmin
        notifyDataSetChanged()
    }
}
