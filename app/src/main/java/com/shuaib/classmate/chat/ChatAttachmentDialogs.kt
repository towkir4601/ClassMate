package com.shuaib.classmate.chat

import android.app.Dialog
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.shuaib.classmate.R
import com.shuaib.classmate.chat.model.ChatUser
import com.shuaib.classmate.utils.applyFloatingPostAnimation

object ChatAttachmentDialogs {

    fun showContactPicker(
        context: Context,
        users: List<ChatUser>,
        onContactSelected: (ChatUser) -> Unit
    ) {
        val dialog = BottomSheetDialog(context)
        val view = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_contact_picker, null)
        dialog.setContentView(view)

        val etSearch = view.findViewById<EditText>(R.id.etContactSearch)
        val rvContacts = view.findViewById<RecyclerView>(R.id.rvContacts)

        var displayList = users

        val adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val row = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    val pad = (12 * context.resources.displayMetrics.density).toInt()
                    setPadding(pad, pad, pad, pad)
                    background = ContextCompat.getDrawable(context, android.R.drawable.list_selector_background)
                }

                val avatar = ImageView(context).apply {
                    val size = (40 * context.resources.displayMetrics.density).toInt()
                    layoutParams = LinearLayout.LayoutParams(size, size).apply {
                        marginEnd = (12 * context.resources.displayMetrics.density).toInt()
                    }
                    setImageResource(R.drawable.ic_wa_contact)
                    setBackgroundResource(R.drawable.bg_circle_button)
                }

                val infoLayout = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }

                val tvName = TextView(context).apply {
                    id = View.generateViewId()
                    textSize = 15f
                    setTextColor(ContextCompat.getColor(context, R.color.wa_text_primary))
                    setTypeface(null, android.graphics.Typeface.BOLD)
                }

                val tvRole = TextView(context).apply {
                    id = View.generateViewId()
                    textSize = 12f
                    setTextColor(ContextCompat.getColor(context, R.color.wa_text_secondary))
                }

                infoLayout.addView(tvName)
                infoLayout.addView(tvRole)
                row.addView(avatar)
                row.addView(infoLayout)

                row.tag = Pair(tvName, tvRole)
                return object : RecyclerView.ViewHolder(row) {}
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val user = displayList[position]
                @Suppress("UNCHECKED_CAST")
                val tags = holder.itemView.tag as Pair<TextView, TextView>
                tags.first.text = user.name
                tags.second.text = user.email.ifBlank { user.role }

                holder.itemView.setOnClickListener {
                    dialog.dismiss()
                    onContactSelected(user)
                }
            }

            override fun getItemCount(): Int = displayList.size
        }

        rvContacts.layoutManager = LinearLayoutManager(context)
        rvContacts.adapter = adapter

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val q = s?.toString()?.trim().orEmpty()
                displayList = if (q.isBlank()) users else users.filter {
                    it.name.contains(q, ignoreCase = true) || it.email.contains(q, ignoreCase = true)
                }
                adapter.notifyDataSetChanged()
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })

        dialog.show()
    }

    fun showCreatePoll(
        context: Context,
        onPollCreated: (question: String, options: List<String>) -> Unit
    ) {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val pad = (20 * context.resources.displayMetrics.density).toInt()
        val inputMargin = (10 * context.resources.displayMetrics.density).toInt()

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_dialog_rounded)
            setPadding(pad, pad, pad, pad)
        }

        val tvTitle = TextView(context).apply {
            text = "Create Poll"
            textSize = 18f
            setTextColor(ContextCompat.getColor(context, R.color.wa_text_primary))
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, inputMargin)
        }
        root.addView(tvTitle)

        val etQuestion = EditText(context).apply {
            hint = "Ask question..."
            setBackgroundResource(R.drawable.bg_whatsapp_reply_box)
            setPadding(pad / 2, pad / 2, pad / 2, pad / 2)
            setTextColor(ContextCompat.getColor(context, R.color.wa_text_primary))
            setHintTextColor(ContextCompat.getColor(context, R.color.wa_text_secondary))
        }
        root.addView(etQuestion)

        val etOpt1 = EditText(context).apply {
            hint = "Option 1"
            setBackgroundResource(R.drawable.bg_whatsapp_reply_box)
            setPadding(pad / 2, pad / 2, pad / 2, pad / 2)
            setTextColor(ContextCompat.getColor(context, R.color.wa_text_primary))
            setHintTextColor(ContextCompat.getColor(context, R.color.wa_text_secondary))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = inputMargin
            }
        }
        root.addView(etOpt1)

        val etOpt2 = EditText(context).apply {
            hint = "Option 2"
            setBackgroundResource(R.drawable.bg_whatsapp_reply_box)
            setPadding(pad / 2, pad / 2, pad / 2, pad / 2)
            setTextColor(ContextCompat.getColor(context, R.color.wa_text_primary))
            setHintTextColor(ContextCompat.getColor(context, R.color.wa_text_secondary))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = inputMargin
            }
        }
        root.addView(etOpt2)

        val etOpt3 = EditText(context).apply {
            hint = "Option 3 (optional)"
            setBackgroundResource(R.drawable.bg_whatsapp_reply_box)
            setPadding(pad / 2, pad / 2, pad / 2, pad / 2)
            setTextColor(ContextCompat.getColor(context, R.color.wa_text_primary))
            setHintTextColor(ContextCompat.getColor(context, R.color.wa_text_secondary))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = inputMargin
            }
        }
        root.addView(etOpt3)

        val btnSend = MaterialButton(context).apply {
            text = "Send Poll"
            setBackgroundColor(ContextCompat.getColor(context, R.color.wa_green_bright))
            setTextColor(ContextCompat.getColor(context, R.color.white))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = inputMargin * 2
            }
            setOnClickListener {
                val q = etQuestion.text.toString().trim()
                val o1 = etOpt1.text.toString().trim()
                val o2 = etOpt2.text.toString().trim()
                val o3 = etOpt3.text.toString().trim()

                if (q.isBlank()) {
                    Toast.makeText(context, "Please enter a question", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (o1.isBlank() || o2.isBlank()) {
                    Toast.makeText(context, "Please enter at least 2 options", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val options = mutableListOf(o1, o2)
                if (o3.isNotBlank()) options.add(o3)

                dialog.dismiss()
                onPollCreated(q, options)
            }
        }
        root.addView(btnSend)

        dialog.setContentView(root)
        dialog.show()
        dialog.applyFloatingPostAnimation(context, 0.92, 0.65, isWrapHeight = true)
    }
}
