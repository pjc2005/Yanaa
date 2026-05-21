package com.yanaa.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE month = :month")
    suspend fun getByMonth(month: String): List<Budget>

    @Query("SELECT * FROM budgets WHERE month = :month AND category = :category LIMIT 1")
    suspend fun getByMonthAndCategory(month: String, category: String): Budget?

    @Query("SELECT * FROM budgets WHERE month = :month AND category = '' LIMIT 1")
    suspend fun getTotalBudgetByMonth(month: String): Budget?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(budget: Budget)

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM budgets")
    suspend fun deleteAll()
}
