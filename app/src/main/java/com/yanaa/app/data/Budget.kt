package com.yanaa.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String = "", // "" = total budget for the month
    val amount: Double,
    val month: String // "2026-05"
)
