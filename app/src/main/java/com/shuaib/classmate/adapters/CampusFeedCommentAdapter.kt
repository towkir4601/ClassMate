package com.shuaib.classmate.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.shuaib.classmate.R
import com.shuaib.classmate.databinding.ItemCampusFeedCommentBinding
import com.shuaib.classmate.models.CampusFeedComment

class CampusFeedCommentAdapter :
    ListAdapter<CampusFeedComment, CampusFeedCommentAdapter.CommentViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val binding = ItemCampusFeedCommentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CommentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CommentViewHolder(
        private val binding: ItemCampusFeedCommentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(comment: CampusFeedComment) {
            val ctx = itemView.context

            binding.tvCommentAuthorName.text = comment.authorName.ifBlank { "GSTU Member" }
            binding.tvCommentTime.text = CampusFeedAdapter.formatRelativeTime(comment.timestamp)
            binding.tvCommentContent.text = comment.content

            // Role Badge
            if (comment.authorRole.isNotBlank() && !comment.authorRole.equals("student", ignoreCase = true)) {
                binding.tvCommentRoleBadge.isVisible = true
                binding.tvCommentRoleBadge.text = comment.authorRole.uppercase()
            } else {
                binding.tvCommentRoleBadge.isVisible = false
            }

            // Avatar
            if (comment.authorAvatar.isNotBlank()) {
                binding.ivCommentAvatar.isVisible = true
                binding.tvCommentAvatarFallback.isVisible = false
                Glide.with(ctx)
                    .load(comment.authorAvatar)
                    .circleCrop()
                    .placeholder(R.drawable.ic_person)
                    .into(binding.ivCommentAvatar)
            } else {
                binding.ivCommentAvatar.isVisible = false
                binding.tvCommentAvatarFallback.isVisible = true
                binding.tvCommentAvatarFallback.text =
                    comment.authorName.firstOrNull()?.uppercaseChar()?.toString() ?: "U"
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<CampusFeedComment>() {
        override fun areItemsTheSame(oldItem: CampusFeedComment, newItem: CampusFeedComment): Boolean {
            return if (oldItem.id.isNotBlank() && newItem.id.isNotBlank()) {
                oldItem.id == newItem.id
            } else {
                oldItem.content == newItem.content && oldItem.authorId == newItem.authorId
            }
        }

        override fun areContentsTheSame(oldItem: CampusFeedComment, newItem: CampusFeedComment): Boolean {
            return oldItem == newItem
        }
    }
}
