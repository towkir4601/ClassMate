package com.shuaib.classmate.network

import com.google.gson.annotations.SerializedName

data class TelegramApiResponse<T>(
    @SerializedName("ok") val ok: Boolean,
    @SerializedName("result") val result: T?,
    @SerializedName("description") val description: String?,
    @SerializedName("error_code") val errorCode: Int?
)

data class TelegramFile(
    @SerializedName("file_id") val fileId: String,
    @SerializedName("file_unique_id") val fileUniqueId: String,
    @SerializedName("file_size") val fileSize: Long?,
    @SerializedName("file_path") val filePath: String?
)
