package com.shuaib.classmate.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable as AndroidDrawable
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.shuaib.classmate.R
import com.shuaib.classmate.activities.ImageViewerActivity
import com.shuaib.classmate.chat.model.ChatMessage
import com.shuaib.classmate.notices.NoticeForwardManager
import com.shuaib.classmate.notices.NoticeUi
import com.google.android.flexbox.FlexboxLayout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class ChatAdapter(
    private val currentUserId: String,
    private val isAdminProvider: () -> Boolean,
    private val onDeleteClick: (ChatMessage) -> Unit,
    private val onReplyClick: (ChatMessage) -> Unit,
    private val onReactClick: (ChatMessage, String) -> Unit,
    private val onEditClick: (ChatMessage) -> Unit,
    private val onForwardClick: (ChatMessage) -> Unit,
    private val onPinClick: (ChatMessage, Boolean) -> Unit,
    private val onVotePoll: ((ChatMessage, Int) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var currentList: List<ChatMessage> = emptyList()
        private set

    private var rows: List<Row> = emptyList()

    private var activeMediaPlayer: android.media.MediaPlayer? = null
    private var playingAudioUrl: String? = null
    private var activePlayingHolder: MessageViewHolder? = null
    private val audioHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var audioProgressRunnable: Runnable? = null

    fun submitList(messages: List<ChatMessage>, commitCallback: (() -> Unit)? = null) {
        val oldRows = rows
        currentList = messages
        rows = buildRows(messages)
        DiffUtil.calculateDiff(RowDiff(oldRows, rows)).dispatchUpdatesTo(this)
        commitCallback?.invoke()
    }

    fun getMessageAtAdapterPosition(position: Int): ChatMessage? {
        return (rows.getOrNull(position) as? Row.Message)?.message
    }

    fun getAdapterPositionForMessageId(messageId: String): Int {
        return rows.indexOfFirst { row ->
            row is Row.Message && row.message.id == messageId
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (val row = rows[position]) {
            is Row.DateSeparator -> VIEW_TYPE_DATE_SEPARATOR
            is Row.Message -> if (row.message.senderId == currentUserId) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_DATE_SEPARATOR -> DateViewHolder(inflater.inflate(R.layout.item_chat_date_separator, parent, false))
            VIEW_TYPE_SENT -> MessageViewHolder(inflater.inflate(R.layout.item_chat_message_sent, parent, false))
            else -> MessageViewHolder(inflater.inflate(R.layout.item_chat_message_received, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is Row.DateSeparator -> (holder as DateViewHolder).bind(row.label)
            is Row.Message -> (holder as MessageViewHolder).bind(row.message, row.previous)
        }
    }

    override fun getItemCount(): Int = rows.size

    private fun buildRows(messages: List<ChatMessage>): List<Row> {
        val result = mutableListOf<Row>()
        var previous: ChatMessage? = null
        messages.forEach { message ->
            val previousMessage = previous
            if (previousMessage == null || !isSameDay(previousMessage.timestamp, message.timestamp)) {
                result += Row.DateSeparator("date_${message.timestamp}", formatDay(message.timestamp))
            }
            result += Row.Message(message, previousMessage)
            previous = message
        }
        return result
    }

    class DateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val text: TextView = itemView.findViewById(R.id.tvDateSeparator)
        fun bind(label: String) {
            text.text = label
        }
    }

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val container: View = itemView.findViewById(R.id.messageContainer)
        private val messageText: TextView = itemView.findViewById(R.id.tvMessageText)
        private val timestamp: TextView = itemView.findViewById(R.id.tvTimestamp)
        private val ticks: TextView = itemView.findViewById(R.id.tvTicks)
        private val edited: TextView = itemView.findViewById(R.id.tvEdited)
        private val senderName: TextView? = itemView.findViewById(R.id.tvSenderName)
        private val senderVerified: ImageView? = itemView.findViewById(R.id.ivSenderVerified)
        private val senderAvatar: ImageView? = itemView.findViewById(R.id.ivSenderAvatar)
        private val senderAvatarLetter: TextView? = itemView.findViewById(R.id.tvSenderAvatarLetter)
        private val forwarded: TextView = itemView.findViewById(R.id.tvForwarded)
        private val forwardDivider: View? = itemView.findViewById(R.id.viewForwardDivider)
        private val pinned: TextView = itemView.findViewById(R.id.tvPinned)
        private val replyPreview: View = itemView.findViewById(R.id.replyPreview)
        private val replySender: TextView = itemView.findViewById(R.id.tvReplySender)
        private val replyText: TextView = itemView.findViewById(R.id.tvReplyText)
        private val imageFrame: View = itemView.findViewById(R.id.imageFrame)
        private val imageProgress: View = itemView.findViewById(R.id.progressImage)
        private val messageImage: ImageView = itemView.findViewById(R.id.ivMessageImage)
        private val noticeForwardContainer: ViewGroup = itemView.findViewById(R.id.noticeForwardContainer)
        private val reactionsBar: FlexboxLayout = itemView.findViewById(R.id.reactionsBar)
        private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        val audioContainer: View? = itemView.findViewById(R.id.audioContainer)
        val btnAudioPlayPause: android.widget.ImageButton? = itemView.findViewById(R.id.btnAudioPlayPause)
        val audioSeekBar: android.widget.SeekBar? = itemView.findViewById(R.id.audioSeekBar)
        val tvAudioDuration: TextView? = itemView.findViewById(R.id.tvAudioDuration)

        val contactContainer: View? = itemView.findViewById(R.id.contactContainer)
        val tvContactName: TextView? = itemView.findViewById(R.id.tvContactName)
        val tvContactPhone: TextView? = itemView.findViewById(R.id.tvContactPhone)
        val btnContactAction: TextView? = itemView.findViewById(R.id.btnContactAction)

        val pollContainer: View? = itemView.findViewById(R.id.pollContainer)
        val tvPollQuestion: TextView? = itemView.findViewById(R.id.tvPollQuestion)
        val pollOptionsLayout: LinearLayout? = itemView.findViewById(R.id.pollOptionsLayout)
        val tvPollTotalVotes: TextView? = itemView.findViewById(R.id.tvPollTotalVotes)

        fun bind(message: ChatMessage, previous: ChatMessage?) {
            val isSent = message.senderId == currentUserId
            val firstInGroup = previous == null ||
                previous.senderId != message.senderId ||
                message.timestamp - previous.timestamp > TimeUnit.MINUTES.toMillis(2)
            (itemView.layoutParams as? RecyclerView.LayoutParams)?.let {
                it.topMargin = if (firstInGroup) dp(8) else dp(0)
                itemView.layoutParams = it
            }

            senderName?.isVisible = firstInGroup
            senderName?.text = message.senderName.ifBlank { "User" }
            if (senderName != null && firstInGroup) {
                senderName.setTextColor(getSenderColor(message.senderId.ifBlank { message.senderName }, itemView.context))
            }
            senderVerified?.isVisible = firstInGroup && (
                message.isSenderVerified ||
                message.senderRole in listOf("teacher", "admin", "superadmin", "university_admin") ||
                message.senderName.contains("Dr.", ignoreCase = true) ||
                message.senderName.contains("Prof", ignoreCase = true)
            )
            senderAvatar?.let { avatar ->
                avatar.isInvisible = !firstInGroup
                senderAvatarLetter?.isInvisible = !firstInGroup
                if (firstInGroup) {
                    AvatarUtils.bind(avatar, senderAvatarLetter, message.senderId, message.senderName, message.senderAvatarUrl)
                }
            }

            pinned.isVisible = message.isPinned
            forwarded.isVisible = message.forwardedFrom != null
            forwardDivider?.isVisible = message.forwardedFrom != null
            forwarded.text = message.forwardedFrom?.let { itemView.context.getString(R.string.forwarded_from_format, it) } ?: ""
            replyPreview.isVisible = message.replyToId != null
            replySender.text = message.replyToSender.orEmpty()
            replyText.text = message.replyToText.orEmpty()
            val noticeForward = NoticeForwardManager.decode(message.text)
            imageFrame.isVisible = message.imageUrl != null && noticeForward == null
            message.imageUrl?.takeIf { noticeForward == null }?.let {
                imageProgress.isVisible = true
                Glide.with(itemView)
                    .load(it)
                    .apply(RequestOptions().transform(CenterCrop(), RoundedCorners(dp(12))))
                    .listener(object : RequestListener<AndroidDrawable> {
                        override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<AndroidDrawable>, isFirstResource: Boolean): Boolean {
                            imageProgress.isVisible = false
                            return false
                        }
                        override fun onResourceReady(resource: AndroidDrawable, model: Any, target: Target<AndroidDrawable>?, dataSource: DataSource, isFirstResource: Boolean): Boolean {
                            imageProgress.isVisible = false
                            return false
                        }
                    })
                    .into(messageImage)
                messageImage.setOnClickListener {
                    itemView.context.startActivity(
                        Intent(itemView.context, ImageViewerActivity::class.java)
                            .putExtra(ImageViewerActivity.EXTRA_IMAGE_URL, message.imageUrl)
                    )
                }
            } ?: run {
                imageProgress.isVisible = false
                messageImage.setImageDrawable(null)
                messageImage.setOnClickListener(null)
            }

            // Voice note binding
            val hasAudio = !message.audioUrl.isNullOrBlank()
            audioContainer?.isVisible = hasAudio
            if (hasAudio) {
                val isCurrentlyPlaying = playingAudioUrl == message.audioUrl && activeMediaPlayer?.isPlaying == true
                btnAudioPlayPause?.setImageResource(if (isCurrentlyPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow)
                tvAudioDuration?.text = formatDuration(message.audioDurationSec)
                btnAudioPlayPause?.setOnClickListener {
                    toggleAudioPlayback(this, message)
                }
            } else {
                btnAudioPlayPause?.setOnClickListener(null)
            }

            // Contact binding
            val hasContact = !message.contactName.isNullOrBlank()
            contactContainer?.isVisible = hasContact
            if (hasContact) {
                tvContactName?.text = message.contactName
                tvContactPhone?.text = message.contactPhone ?: message.contactRole ?: "Classmate"
                btnContactAction?.setOnClickListener {
                    val phone = message.contactPhone.orEmpty()
                    if (phone.isNotBlank()) {
                        runCatching {
                            val intent = Intent(Intent.ACTION_DIAL, android.net.Uri.parse("tel:$phone"))
                            itemView.context.startActivity(intent)
                        }
                    }
                }
            } else {
                btnContactAction?.setOnClickListener(null)
            }

            // Poll binding
            val hasPoll = !message.pollQuestion.isNullOrBlank()
            pollContainer?.isVisible = hasPoll
            if (hasPoll) {
                tvPollQuestion?.text = message.pollQuestion
                pollOptionsLayout?.removeAllViews()
                val totalVotes = message.pollVotes.values.sumOf { it.size }
                tvPollTotalVotes?.text = if (totalVotes == 1) "1 vote" else "$totalVotes votes"

                message.pollOptions.forEachIndexed { index, optionText ->
                    val optKey = index.toString()
                    val voters = message.pollVotes[optKey] ?: emptyList()
                    val optVotes = voters.size
                    val pct = if (totalVotes > 0) (optVotes * 100 / totalVotes) else 0
                    val isVoted = currentUserId in voters

                    val optionRow = LinearLayout(itemView.context).apply {
                        orientation = LinearLayout.VERTICAL
                        setBackgroundResource(if (isVoted) R.drawable.bg_poll_option_selected else R.drawable.bg_poll_option)
                        val pad = dp(8)
                        setPadding(pad, pad, pad, pad)
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            setMargins(0, 0, 0, dp(6))
                        }
                    }

                    val titleRow = LinearLayout(itemView.context).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                    }

                    val tvOptText = TextView(itemView.context).apply {
                        text = optionText
                        textSize = 13f
                        setTextColor(ContextCompat.getColor(itemView.context, R.color.wa_text_primary))
                        setTypeface(null, if (isVoted) Typeface.BOLD else Typeface.NORMAL)
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    }

                    val tvOptPct = TextView(itemView.context).apply {
                        text = if (totalVotes > 0) "$optVotes ($pct%)" else "0"
                        textSize = 11f
                        setTextColor(ContextCompat.getColor(itemView.context, R.color.wa_text_secondary))
                    }

                    titleRow.addView(tvOptText)
                    titleRow.addView(tvOptPct)
                    optionRow.addView(titleRow)

                    optionRow.setOnClickListener {
                        onVotePoll?.invoke(message, index)
                    }

                    pollOptionsLayout?.addView(optionRow)
                }
            }

            val isCustomCard = hasAudio || hasContact || hasPoll
            if (message.isDeleted) {
                noticeForwardContainer.isVisible = false
                messageText.isVisible = true
                messageText.setText(R.string.message_deleted)
                messageText.setTextColor(ContextCompat.getColor(itemView.context, R.color.wa_text_secondary))
                messageText.setTypeface(null, Typeface.ITALIC)
            } else if (noticeForward != null) {
                messageText.isVisible = false
                bindNoticeForward(noticeForward)
            } else if (isCustomCard && (message.text.isBlank() || message.text == "Voice message" || message.text.startsWith("Contact:") || message.text.startsWith("Poll:"))) {
                messageText.isVisible = false
                noticeForwardContainer.isVisible = false
            } else {
                noticeForwardContainer.isVisible = false
                messageText.isVisible = message.text.isNotBlank()
                messageText.text = message.text
                messageText.setTextColor(ContextCompat.getColor(itemView.context, R.color.wa_text_primary))
                messageText.setTypeface(null, Typeface.NORMAL)
            }
            timestamp.text = timeFormat.format(Date(message.timestamp))
            edited.isVisible = message.editedAt != null
            val metaColor = ContextCompat.getColor(itemView.context, R.color.wa_text_secondary)
            timestamp.setTextColor(metaColor)
            edited.setTextColor(metaColor)
            ticks.text = if (message.seenBy.isNotEmpty()) "\u2713\u2713" else "\u2713"
            ticks.setTextColor(
                if (message.seenBy.isNotEmpty()) ContextCompat.getColor(itemView.context, R.color.wa_tick_blue)
                else metaColor
            )
            bindReactions(message)
            container.setOnLongClickListener {
                showActions(message)
                true
            }
        }

        private fun getSenderColor(nameOrId: String, context: Context): Int {
            val colors = intArrayOf(
                R.color.wa_sender_blue,
                R.color.wa_sender_orange,
                R.color.wa_sender_green,
                R.color.wa_sender_magenta,
                R.color.wa_sender_purple,
                R.color.wa_sender_teal,
                R.color.wa_sender_amber,
                R.color.wa_sender_crimson
            )
            val index = Math.abs(nameOrId.hashCode()) % colors.size
            return ContextCompat.getColor(context, colors[index])
        }

        private fun bindNoticeForward(payload: com.shuaib.classmate.notices.NoticeForwardPayload) {
            noticeForwardContainer.isVisible = true
            noticeForwardContainer.removeAllViews()
            val card = LayoutInflater.from(itemView.context)
                .inflate(R.layout.item_chat_notice_forward, noticeForwardContainer, false)
            card.findViewById<TextView>(R.id.tvForwardType).apply {
                text = NoticeUi.typeLabel(payload.noticeType)
                setTextColor(NoticeUi.accent(itemView.context, payload.noticeType))
            }
            card.findViewById<TextView>(R.id.tvForwardSubject).text = payload.subject
            card.findViewById<TextView>(R.id.tvForwardDeadline).apply {
                isVisible = payload.deadlineAt != null
                setText(R.string.deadline)
            }
            card.findViewById<TextView>(R.id.tvForwardTitle).text = payload.title
            card.findViewById<TextView>(R.id.tvForwardPreview).text = payload.contentPreview
            card.setOnClickListener {
                NoticeForwardManager.openNotice(itemView.context, payload.noticeId)
            }
            noticeForwardContainer.addView(card)
        }

        private fun bindReactions(message: ChatMessage) {
            reactionsBar.removeAllViews()
            reactionsBar.isVisible = message.reactions.isNotEmpty()
            val currentUser = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
            message.reactions.forEach { reaction ->
                val chip = TextView(itemView.context).apply {
                    text = itemView.context.getString(R.string.reaction_format, reaction.emoji, reaction.userIds.size)
                    setTextColor(com.shuaib.classmate.utils.ThemeColors.textPrimary(itemView.context))
                    textSize = 13f
                    setPadding(dp(8), dp(4), dp(8), dp(4))
                    background = AppCompatResources.getDrawable(itemView.context, 
                        if (currentUser in reaction.userIds) R.drawable.bg_reaction_chip_selected else R.drawable.bg_reaction_chip
                    )
                    setOnClickListener { onReactClick(message, reaction.emoji) }
                }
                reactionsBar.addView(chip, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = dp(4) })
            }
            if (message.reactions.isNotEmpty()) {
                reactionsBar.addView(TextView(itemView.context).apply {
                    text = "+"
                    setTextColor(com.shuaib.classmate.utils.ThemeColors.textSecondary(itemView.context))
                    textSize = 13f
                    gravity = Gravity.CENTER
                    setPadding(dp(10), dp(4), dp(10), dp(4))
                    background = AppCompatResources.getDrawable(itemView.context, R.drawable.bg_reaction_chip)
                    setOnClickListener { showEmojiPicker(message) }
                })
            }
        }

        private fun showActions(message: ChatMessage) {
            val dialog = BottomSheetDialog(itemView.context)
            val actions = LinearLayout(itemView.context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(18), dp(10), dp(18), dp(18))
                background = AppCompatResources.getDrawable(itemView.context, R.drawable.bg_bottom_sheet)
            }
            actions.addView(View(itemView.context).apply {
                background = GradientDrawable().apply {
                    cornerRadius = dp(2).toFloat()
                    setColor(com.shuaib.classmate.utils.ThemeColors.borderStrong(itemView.context))
                }
            }, LinearLayout.LayoutParams(dp(40), dp(4)).apply {
                bottomMargin = dp(12)
                gravity = Gravity.CENTER_HORIZONTAL
            })
            addEmojiPickerRow(actions, dialog, message)
            addAction(actions, R.drawable.ic_reply, "Reply") { dialog.dismiss(); onReplyClick(message) }
            addAction(actions, R.drawable.ic_smile, "React") { dialog.dismiss(); showEmojiPicker(message) }
            if (message.senderId == currentUserId && !message.isDeleted && !isOlderThan48Hours(message)) {
                addAction(actions, R.drawable.ic_edit, "Edit") { dialog.dismiss(); onEditClick(message) }
            }
            addAction(actions, R.drawable.ic_forward, "Forward") { dialog.dismiss(); onForwardClick(message) }
            if (isAdminProvider()) {
                addAction(actions, R.drawable.ic_pin, if (message.isPinned) "Unpin" else "Pin") {
                    dialog.dismiss()
                    onPinClick(message, !message.isPinned)
                }
            }
            addAction(actions, R.drawable.ic_copy, "Copy") { dialog.dismiss(); copyText(message.text) }
            if ((message.senderId == currentUserId || isAdminProvider()) && !message.isDeleted) {
                addAction(actions, R.drawable.ic_delete, "Delete") { dialog.dismiss(); onDeleteClick(message) }
            }
            dialog.setContentView(actions)
            dialog.show()
        }

        private fun addEmojiPickerRow(parent: LinearLayout, dialog: BottomSheetDialog, message: ChatMessage) {
            val emojis = arrayOf("\uD83D\uDC4D", "\u2764\uFE0F", "\uD83D\uDE02", "\uD83D\uDE2E", "\uD83D\uDE22", "\uD83D\uDD25")
            val row = LinearLayout(itemView.context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 0, 0, dp(10))
            }
            emojis.forEach { emoji ->
                row.addView(TextView(itemView.context).apply {
                    text = emoji
                    textSize = 24f
                    gravity = Gravity.CENTER
                    setOnClickListener {
                        dialog.dismiss()
                        onReactClick(message, emoji)
                    }
                }, LinearLayout.LayoutParams(dp(36), dp(36)))
            }
            parent.addView(row)
        }

        private fun addAction(parent: LinearLayout, iconRes: Int, label: String, action: () -> Unit) {
            val row = LinearLayout(itemView.context).apply {
                gravity = Gravity.CENTER_VERTICAL
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(12), 0, dp(12))
                background = selectableBackground()
                setOnClickListener { action() }
            }
            row.addView(ImageView(itemView.context).apply {
                setImageResource(iconRes)
                setColorFilter(com.shuaib.classmate.utils.ThemeColors.textSecondary(itemView.context))
            }, LinearLayout.LayoutParams(dp(24), dp(24)).apply { marginEnd = dp(18) })
            row.addView(TextView(itemView.context).apply {
                text = label
                setTextColor(com.shuaib.classmate.utils.ThemeColors.textPrimary(itemView.context))
                textSize = 16f
            })
            parent.addView(row)
        }

        private fun showEmojiPicker(message: ChatMessage) {
            val dialog = BottomSheetDialog(itemView.context)
            val emojis = arrayOf("\uD83D\uDC4D", "\u2764\uFE0F", "\uD83D\uDE02", "\uD83D\uDE2E", "\uD83D\uDE22", "\uD83D\uDD25")
            val row = LinearLayout(itemView.context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(dp(18), dp(18), dp(18), dp(18))
                background = AppCompatResources.getDrawable(itemView.context, R.drawable.bg_bottom_sheet)
            }
            emojis.forEach { emoji ->
                row.addView(TextView(itemView.context).apply {
                    text = emoji
                    textSize = 26f
                    gravity = Gravity.CENTER
                    setOnClickListener {
                        dialog.dismiss()
                        onReactClick(message, emoji)
                    }
                }, LinearLayout.LayoutParams(0, dp(52), 1f))
            }
            dialog.setContentView(row)
            dialog.show()
        }

        private fun copyText(text: String) {
            val clipboard = itemView.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("message", text))
        }

        private fun isOlderThan48Hours(message: ChatMessage): Boolean {
            return System.currentTimeMillis() - message.timestamp > TimeUnit.HOURS.toMillis(48)
        }

        private fun selectableBackground(): AndroidDrawable? {
            val attrs = intArrayOf(android.R.attr.selectableItemBackground)
            val ta = itemView.context.obtainStyledAttributes(attrs)
            val drawable = ta.getDrawable(0)
            ta.recycle()
            return drawable
        }

        private fun dp(value: Int): Int {
            return (value * itemView.resources.displayMetrics.density).toInt()
        }
    }

    private fun formatDuration(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return String.format(Locale.US, "%d:%02d", m, s)
    }

    private fun toggleAudioPlayback(holder: MessageViewHolder, message: ChatMessage) {
        val audioUrl = message.audioUrl ?: return
        if (playingAudioUrl == audioUrl && activeMediaPlayer != null) {
            if (activeMediaPlayer?.isPlaying == true) {
                activeMediaPlayer?.pause()
                holder.btnAudioPlayPause?.setImageResource(R.drawable.ic_play_arrow)
                stopAudioProgress()
            } else {
                activeMediaPlayer?.start()
                holder.btnAudioPlayPause?.setImageResource(R.drawable.ic_pause)
                startAudioProgress(holder)
            }
            return
        }

        stopAudioPlayback()

        try {
            val mp = android.media.MediaPlayer().apply {
                setDataSource(audioUrl)
                prepareAsync()
                setOnPreparedListener {
                    start()
                    playingAudioUrl = audioUrl
                    activePlayingHolder = holder
                    holder.btnAudioPlayPause?.setImageResource(R.drawable.ic_pause)
                    startAudioProgress(holder)
                }
                setOnCompletionListener {
                    stopAudioPlayback()
                    holder.btnAudioPlayPause?.setImageResource(R.drawable.ic_play_arrow)
                    holder.audioSeekBar?.progress = 0
                    holder.tvAudioDuration?.text = formatDuration(message.audioDurationSec)
                }
                setOnErrorListener { _, _, _ ->
                    stopAudioPlayback()
                    holder.btnAudioPlayPause?.setImageResource(R.drawable.ic_play_arrow)
                    true
                }
            }
            activeMediaPlayer = mp
        } catch (e: Exception) {
            stopAudioPlayback()
        }
    }

    private fun startAudioProgress(holder: MessageViewHolder) {
        stopAudioProgress()
        audioProgressRunnable = object : Runnable {
            override fun run() {
                val mp = activeMediaPlayer ?: return
                if (mp.isPlaying) {
                    val currentPos = mp.currentPosition
                    val totalDuration = mp.duration.coerceAtLeast(1)
                    val progress = (currentPos * 100 / totalDuration).coerceIn(0, 100)
                    holder.audioSeekBar?.progress = progress
                    holder.tvAudioDuration?.text = formatDuration(currentPos / 1000)
                    audioHandler.postDelayed(this, 250)
                }
            }
        }
        audioHandler.post(audioProgressRunnable!!)
    }

    private fun stopAudioProgress() {
        audioProgressRunnable?.let { audioHandler.removeCallbacks(it) }
        audioProgressRunnable = null
    }

    fun stopAudioPlayback() {
        stopAudioProgress()
        try {
            activeMediaPlayer?.stop()
            activeMediaPlayer?.release()
        } catch (ignored: Exception) {}
        activeMediaPlayer = null
        playingAudioUrl = null
        activePlayingHolder?.btnAudioPlayPause?.setImageResource(R.drawable.ic_play_arrow)
        activePlayingHolder = null
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        stopAudioPlayback()
    }

    private fun isSameDay(first: Long, second: Long): Boolean {
        val a = Calendar.getInstance().apply { timeInMillis = first }
        val b = Calendar.getInstance().apply { timeInMillis = second }
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
    }

    private fun formatDay(value: Long): String {
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        return when {
            isSameDay(System.currentTimeMillis(), value) -> "Today"
            isSameDay(yesterday.timeInMillis, value) -> "Yesterday"
            else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(value))
        }
    }

    private sealed class Row {
        data class DateSeparator(val id: String, val label: String) : Row()
        data class Message(val message: ChatMessage, val previous: ChatMessage?) : Row()
    }

    private class RowDiff(private val old: List<Row>, private val new: List<Row>) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = old.size
        override fun getNewListSize(): Int = new.size
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = old[oldItemPosition]
            val newItem = new[newItemPosition]
            return when {
                oldItem is Row.DateSeparator && newItem is Row.DateSeparator -> oldItem.id == newItem.id
                oldItem is Row.Message && newItem is Row.Message -> oldItem.message.id == newItem.message.id
                else -> false
            }
        }
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean = old[oldItemPosition] == new[newItemPosition]
    }

    companion object {
        const val VIEW_TYPE_SENT = 1
        const val VIEW_TYPE_RECEIVED = 3
        const val VIEW_TYPE_DATE_SEPARATOR = 2
    }
}
