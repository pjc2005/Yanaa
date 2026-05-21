package com.yanaa.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "records")
data class Record(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val type: String = "expense", // "expense" or "income"
    val category: String,
    val subcategory: String = "",
    val merchant: String = "",
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isAuto: Boolean = false
)
