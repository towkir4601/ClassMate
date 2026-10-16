package com.shuaib.classmate.models

data class HeroSlide(
    val categoryTitle: String,
    val categoryIconRes: Int,
    val categoryColor: Int,
    val actionText: String,
    val statusBadge: String,
    val contextBadge: String,
    val title: String,
    val detail1: String,
    val detail2: String,
    val detail3: String,
    val bgDrawableRes: Int,
    val onClick: () -> Unit
)
