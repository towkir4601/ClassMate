package com.shuaib.classmate.models

enum class TopAlertType {
    NOTICE,
    HOLIDAY,
    BLOOD,
    LOST_FOUND
}

data class TopAlertItem(
    val id: String,
    val type: TopAlertType,
    val title: String,
    val subtitle: String,
    val timeTag: String,
    val bgDrawableRes: Int,
    val iconRes: Int,
    val onClick: () -> Unit,
    val onDismiss: () -> Unit
)
