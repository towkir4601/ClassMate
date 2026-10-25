package com.shuaib.classmate.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface NoticeDao {
    @Query("SELECT * FROM notices WHERE isDeleted = 0 ORDER BY isPinned DESC, timestampMillis DESC LIMIT :limit")
    fun observeNotices(limit: Int = 200): Flow<List<NoticeEntity>>

    @Query("SELECT * FROM notices WHERE isDeleted = 0 ORDER BY isPinned DESC, timestampMillis DESC LIMIT :limit")
    suspend fun getNoticesLocal(limit: Int = 200): List<NoticeEntity>

    @Query("SELECT * FROM notices WHERE isDeleted = 0 AND isCancel = 1 ORDER BY timestampMillis DESC")
    fun observeCancelNotices(): Flow<List<NoticeEntity>>

    @Query("SELECT * FROM notices WHERE isDeleted = 0 AND isCancel = 1 ORDER BY timestampMillis DESC")
    suspend fun getCancelNoticesSync(): List<NoticeEntity>

    @Query("SELECT * FROM notices WHERE id = :noticeId AND isDeleted = 0 LIMIT 1")
    fun observeNotice(noticeId: String): Flow<NoticeEntity?>

    @Query("SELECT * FROM notices WHERE isDeleted = 0 AND (targetBatch = 'all' OR targetBatch = '' OR targetBatch = :userBatch) ORDER BY timestampMillis DESC LIMIT 1")
    fun getLatestNoticeSync(userBatch: String): NoticeEntity?

    @Upsert
    suspend fun upsertAll(notices: List<NoticeEntity>)

    @Query("UPDATE notices SET isDeleted = 1 WHERE id NOT IN (:activeIds)")
    suspend fun markMissingDeleted(activeIds: List<String>)

    @Query("UPDATE notices SET isDeleted = 1 WHERE id = :noticeId")
    suspend fun markDeleted(noticeId: String)

    @Query("UPDATE notices SET isDeleted = 1")
    suspend fun markAllDeleted()
}
