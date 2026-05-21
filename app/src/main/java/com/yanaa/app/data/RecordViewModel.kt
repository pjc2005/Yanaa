package com.yanaa.app.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RecordViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val dao = db.recordDao()

    val allRecords: Flow<List<Record>> = dao.getAll()

    private val _todayTotal = MutableStateFlow(0.0)
    val todayTotal: StateFlow<Double> = _todayTotal

    private val _monthTotal = MutableStateFlow(0.0)
    val monthTotal: StateFlow<Double> = _monthTotal

    init {
        viewModelScope.launch {
            allRecords.collect { records ->
                val now = System.currentTimeMillis()
                val dayStart = now - (now % 86400000L)
                val monthStart = now - (now % 86400000L) - ((java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH) - 1) * 86400000L)

                _todayTotal.value = records
                    .filter { it.timestamp >= dayStart }
                    .sumOf { it.amount }

                _monthTotal.value = records
                    .filter { it.timestamp >= monthStart }
                    .sumOf { it.amount }
            }
        }
    }

    fun insert(record: Record, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            dao.insert(record)
            onDone()
        }
    }
}
