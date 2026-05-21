package com.yanaa.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordDao {
    @Insert
    suspend fun insert(record: Record)

    @Insert
    suspend fun insertAll(records: List<Record>)

    @androidx.room.Update
    suspend fun update(record: Record)

    @Query("DELETE FROM records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM records ORDER BY timestamp DESC")
    fun getAll(): Flow<List<Record>>

    @Query("SELECT * FROM records ORDER BY timestamp DESC")
    suspend fun getAllSync(): List<Record>

    @Query("SELECT * FROM records WHERE id = :id")
    suspend fun getById(id: Long): Record?

    @Query("SELECT * FROM records WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    suspend fun getBetween(start: Long, end: Long): List<Record>

    // Search & filter
    @Query("""
        SELECT * FROM records
        WHERE (:keyword = '' OR note LIKE '%' || :keyword || '%'
               OR category LIKE '%' || :keyword || '%'
               OR subcategory LIKE '%' || :keyword || '%'
               OR merchant LIKE '%' || :keyword || '%')
          AND (:type = '' OR type = :type)
          AND (:categoryFilter = '' OR category = :categoryFilter)
          AND (:startTs = 0 OR timestamp >= :startTs)
          AND (:endTs = 0 OR timestamp <= :endTs)
        ORDER BY timestamp DESC
    """)
    suspend fun search(
        keyword: String = "",
        type: String = "",
        categoryFilter: String = "",
        startTs: Long = 0,
        endTs: Long = 0
    ): List<Record>

    @Query("DELETE FROM records")
    suspend fun deleteAll()

    @Query("SELECT DISTINCT category FROM records WHERE type = 'expense'")
    suspend fun getExpenseCategories(): List<String>

    @Query("SELECT DISTINCT category FROM records WHERE type = 'income'")
    suspend fun getIncomeCategories(): List<String>
}
