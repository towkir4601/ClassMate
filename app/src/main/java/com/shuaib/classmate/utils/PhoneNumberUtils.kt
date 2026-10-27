package com.shuaib.classmate.utils

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

object PhoneNumberUtils {

    /**
     * Normalizes a phone number to standard international Bangladeshi format (+8801XXXXXXXXX).
     * - If input starts with "01", prepends "+88".
     * - If input starts with "8801", prepends "+".
     * - If already "+8801...", leaves as is.
     * - Strips extraneous spaces, hyphens, and parentheses while preserving valid digits and '+'.
     * - Preserves short emergency numbers (e.g. 999, 16122) and non-mobile numbers without alteration.
     */
    fun normalize(rawPhone: String?): String {
        if (rawPhone.isNullOrBlank()) return ""
        val trimmed = rawPhone.trim()
        val cleaned = trimmed.replace("[^0-9+]".toRegex(), "")

        return when {
            cleaned.startsWith("+8801") -> cleaned
            cleaned.startsWith("8801") -> "+$cleaned"
            cleaned.startsWith("01") -> "+88$cleaned"
            else -> trimmed
        }
    }

    /**
     * Attaches a TextWatcher to an EditText that ensures if the user types or pastes
     * a Bangladeshi phone number starting with "01", it automatically prepends "+88"
     * in real-time, giving immediate feedback and preventing missing country codes.
     */
    fun attachPhonePrefixWatcher(editText: EditText) {
        editText.addTextChangedListener(object : TextWatcher {
            private var isUpdating = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isUpdating || s == null) return
                val text = s.toString().trim()

                if (text.startsWith("01") && text.length >= 2) {
                    isUpdating = true
                    val formatted = "+88$text"
                    editText.setText(formatted)
                    editText.setSelection(formatted.length.coerceAtMost(editText.text?.length ?: 0))
                    isUpdating = false
                } else if (text.startsWith("8801")) {
                    isUpdating = true
                    val formatted = "+$text"
                    editText.setText(formatted)
                    editText.setSelection(formatted.length.coerceAtMost(editText.text?.length ?: 0))
                    isUpdating = false
                }
            }
        })
    }
}
