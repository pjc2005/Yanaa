package com.yanaa.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SavingsPlanDao {
    @Query("SELECT * FROM savings_plans ORDER BY isActive DESC, deadline ASC")
    suspend fun getAll(): List<SavingsPlan>

    @Query("SELECT * FROM savings_plans WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): SavingsPlan?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(plan: SavingsPlan)

    @Query("DELETE FROM savings_plans WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE savings_plans SET currentAmount = currentAmount + :amount WHERE id = :id")
    suspend fun addAmount(id: Long, amount: Double)

    @Query("UPDATE savings_plans SET isActive = :active WHERE id = :id")
    suspend fun setActive(id: Long, active: Boolean)
}
