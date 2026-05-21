package com.yanaa.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "savings_plans")
data class SavingsPlan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val deadline: Long? = null, // optional deadline timestamp
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)
