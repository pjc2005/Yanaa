package com.yanaa.app.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

enum class Period { WEEK, MONTH, YEAR }

class RecordViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val dao = db.recordDao()

    val allRecords: Flow<List<Record>> = dao.getAll()

    private val _period = MutableStateFlow(Period.MONTH)
    val period: StateFlow<Period> = _period

    private val _expense = MutableStateFlow(0.0)
    val expense: StateFlow<Double> = _expense

    private val _income = MutableStateFlow(0.0)
    val income: StateFlow<Double> = _income

    private val _balance = MutableStateFlow(0.0)
    val balance: StateFlow<Double> = _balance

    private val _count = MutableStateFlow(0)
    val count: StateFlow<Int> = _count

    init {
        viewModelScope.launch {
            combine(allRecords, period) { records, p -> records to p }
                .collect { (records, p) ->
                    val cal = Calendar.getInstance()
                    val now = System.currentTimeMillis()

                    val startMillis = when (p) {
                        Period.WEEK -> {
                            cal.timeInMillis = now
                            cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                            cal.set(Calendar.HOUR_OF_DAY, 0)
                            cal.set(Calendar.MINUTE, 0)
                            cal.set(Calendar.SECOND, 0)
                            cal.set(Calendar.MILLISECOND, 0)
                            cal.timeInMillis
                        }
                        Period.MONTH -> {
                            cal.timeInMillis = now
                            cal.set(Calendar.DAY_OF_MONTH, 1)
                            cal.set(Calendar.HOUR_OF_DAY, 0)
                            cal.set(Calendar.MINUTE, 0)
                            cal.set(Calendar.SECOND, 0)
                            cal.set(Calendar.MILLISECOND, 0)
                            cal.timeInMillis
                        }
                        Period.YEAR -> {
                            cal.timeInMillis = now
                            cal.set(Calendar.DAY_OF_YEAR, 1)
                            cal.set(Calendar.HOUR_OF_DAY, 0)
                            cal.set(Calendar.MINUTE, 0)
                            cal.set(Calendar.SECOND, 0)
                            cal.set(Calendar.MILLISECOND, 0)
                            cal.timeInMillis
                        }
                    }

                    val filtered = records.filter { it.timestamp >= startMillis }
                    _expense.value = filtered.filter { it.type == "expense" }.sumOf { it.amount }
                    _income.value = filtered.filter { it.type == "income" }.sumOf { it.amount }
                    _balance.value = _income.value - _expense.value
                    _count.value = filtered.size
                }
        }
    }

    fun setPeriod(p: Period) { _period.value = p }

    fun insert(record: Record, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            dao.insert(record)
            onDone()
        }
    }

    fun deleteAll(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            dao.deleteAll()
            onDone()
        }
    }
}
