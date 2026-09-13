package com.shuaib.classmate.chat.model

data class ChatUser(
    val id: String,
    val name: String,
    val avatarUrl: String,
    val role: String = "student",
    val isOnline: Boolean = false
)
