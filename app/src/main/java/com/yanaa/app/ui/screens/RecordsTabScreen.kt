package com.yanaa.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yanaa.app.data.Record
import com.yanaa.app.data.RecordViewModel
import com.yanaa.app.ui.EditRecordActivity
import com.yanaa.app.ui.components.RecordCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

private enum class RecordViewMode { LIST, CALENDAR }
private enum class DateFilter { ALL, TODAY, WEEK, MONTH, CUSTOM }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecordsTabScreen(viewModel: RecordViewModel = viewModel()) {
    val context = LocalContext.current
    val db = remember { com.yanaa.app.data.AppDatabase.getInstance(context) }
    val scope = rememberCoroutineScope()
    val allRecords by viewModel.allRecords.collectAsState(initial = emptyList())
    val dateFormat = remember { SimpleDateFormat("yyyy年MM月dd日", Locale.CHINESE) }

    // Search & filter state
    var viewMode by remember { mutableStateOf(RecordViewMode.LIST) }
    var searchQuery by remember { mutableStateOf("") }
    var showFilterRow by remember { mutableStateOf(false) }
    var dateFilter by remember { mutableStateOf(DateFilter.ALL) }
    var typeFilter by remember { mutableStateOf("") } // "", "expense", "income"
    var categoryFilter by remember { mutableStateOf("") }
    var customStartDate by remember { mutableStateOf("") }
    var customEndDate by remember { mutableStateOf("") }

    // Calendar state
    var calendarMonth by remember { mutableStateOf(Calendar.getInstance()) }
    var selectedDay by remember { mutableStateOf<Int?>(null) }

    // Get available categories for filter
    var expenseCategories by remember { mutableStateOf<List<String>>(emptyList()) }
    var incomeCategories by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(Unit) {
        expenseCategories = withContext(Dispatchers.IO) { db.recordDao().getExpenseCategories() }
        incomeCategories = withContext(Dispatchers.IO) { db.recordDao().getIncomeCategories() }
    }
    val allCategories = remember(expenseCategories, incomeCategories) {
        (expenseCategories + incomeCategories).distinct().sorted()
    }

    // Compute filtered records
    val filteredRecords = remember(allRecords, searchQuery, dateFilter, typeFilter, categoryFilter, customStartDate, customEndDate) {
        var result = allRecords

        // Keyword search
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.lowercase()
            result = result.filter {
                it.note.lowercase().contains(q) ||
                it.category.lowercase().contains(q) ||
                it.subcategory.lowercase().contains(q) ||
                it.merchant.lowercase().contains(q) ||
                String.format("%.0f", it.amount).contains(q)
            }
        }

        // Type filter
        if (typeFilter.isNotBlank()) {
            result = result.filter { it.type == typeFilter }
        }

        // Category filter
        if (categoryFilter.isNotBlank()) {
            result = result.filter { it.category == categoryFilter }
        }

        // Date filter
        val cal = Calendar.getInstance()
        val now = System.currentTimeMillis()
        when (dateFilter) {
            DateFilter.TODAY -> {
                cal.timeInMillis = now
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.add(Calendar.DAY_OF_MONTH, 1)
                result = result.filter { it.timestamp in start..cal.timeInMillis }
            }
            DateFilter.WEEK -> {
                cal.timeInMillis = now
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.add(Calendar.DAY_OF_MONTH, 7)
                result = result.filter { it.timestamp in start..cal.timeInMillis }
            }
            DateFilter.MONTH -> {
                cal.timeInMillis = now
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.add(Calendar.MONTH, 1)
                result = result.filter { it.timestamp in start..cal.timeInMillis }
            }
            DateFilter.CUSTOM -> {
                try {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val start = if (customStartDate.isNotBlank()) sdf.parse(customStartDate)?.time ?: 0 else 0
                    val end = if (customEndDate.isNotBlank()) {
                        val e = sdf.parse(customEndDate)?.time ?: Long.MAX_VALUE
                        e + 86400000 // end of day
                    } else Long.MAX_VALUE
                    result = result.filter { it.timestamp in start..end }
                } catch (_: Exception) {}
            }
            DateFilter.ALL -> {}
        }

        result
    }

    // Group for calendar view
    val dayTotals = remember(filteredRecords, calendarMonth) {
        val cal = Calendar.getInstance()
        cal.timeInMillis = calendarMonth.timeInMillis
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val monthStart = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        val monthEnd = cal.timeInMillis

        filteredRecords.filter { it.timestamp in monthStart..monthEnd }
            .groupBy { record ->
                val c = Calendar.getInstance()
                c.timeInMillis = record.timestamp
                c.get(Calendar.DAY_OF_MONTH)
            }
            .mapValues { (_, recs) ->
                val expense = recs.filter { it.type == "expense" }.sumOf { it.amount }
                val income = recs.filter { it.type == "income" }.sumOf { it.amount }
                Triple(recs.size, expense, income)
            }
    }

    // Records for selected day in calendar
    val selectedDayRecords = remember(filteredRecords, calendarMonth, selectedDay) {
        if (selectedDay == null) return@remember emptyList()
        val cal = Calendar.getInstance()
        cal.timeInMillis = calendarMonth.timeInMillis
        cal.set(Calendar.DAY_OF_MONTH, selectedDay!!)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val dayStart = cal.timeInMillis
        cal.add(Calendar.DAY_OF_MONTH, 1)
        val dayEnd = cal.timeInMillis
        filteredRecords.filter { it.timestamp in dayStart..dayEnd }.sortedByDescending { it.timestamp }
    }

    // Group records by day for list view
    val grouped = remember(filteredRecords) {
        filteredRecords.groupBy { record ->
            val cal = Calendar.getInstance()
            cal.timeInMillis = record.timestamp
            cal.get(Calendar.YEAR) to cal.get(Calendar.DAY_OF_YEAR)
        }.entries.sortedByDescending { (key, _) ->
            key.first.toLong() * 1000 + key.second
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // ── Search bar + toolbar ──
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("搜索备注、分类、金额…") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "清除")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                )
                // Filter toggle
                IconButton(onClick = { showFilterRow = !showFilterRow }) {
                    Icon(
                        if (showFilterRow) Icons.Default.FilterList else Icons.Default.FilterList,
                        contentDescription = "筛选",
                        tint = if (dateFilter != DateFilter.ALL || typeFilter.isNotBlank() || categoryFilter.isNotBlank())
                                  MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // View mode toggle
                IconButton(onClick = {
                    viewMode = if (viewMode == RecordViewMode.LIST) RecordViewMode.CALENDAR
                              else RecordViewMode.LIST
                }) {
                    Icon(
                        if (viewMode == RecordViewMode.LIST) Icons.Default.CalendarMonth
                        else Icons.Default.List,
                        contentDescription = if (viewMode == RecordViewMode.LIST) "日历视图" else "列表视图"
                    )
                }
            }
        }

        // ── Filter chips (expandable) ──
        if (showFilterRow) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // Date filter chips
                        Text("日期", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            DateFilter.entries.forEach { df ->
                                FilterChip(
                                    selected = dateFilter == df,
                                    onClick = { dateFilter = df },
                                    label = { Text(when (df) {
                                        DateFilter.ALL -> "全部"
                                        DateFilter.TODAY -> "今天"
                                        DateFilter.WEEK -> "本周"
                                        DateFilter.MONTH -> "本月"
                                        DateFilter.CUSTOM -> "自定义"
                                    }) }
                                )
                            }
                        }

                        // Custom date range
                        if (dateFilter == DateFilter.CUSTOM) {
                            Spacer(Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = customStartDate,
                                    onValueChange = { customStartDate = it },
                                    label = { Text("开始 (yyyy-MM-dd)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                Text("—", style = MaterialTheme.typography.bodyMedium)
                                OutlinedTextField(
                                    value = customEndDate,
                                    onValueChange = { customEndDate = it },
                                    label = { Text("结束 (yyyy-MM-dd)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // Type filter
                        Text("类型", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = typeFilter == "",
                                onClick = { typeFilter = "" },
                                label = { Text("全部") }
                            )
                            FilterChip(
                                selected = typeFilter == "expense",
                                onClick = { typeFilter = "expense" },
                                label = { Text("支出") }
                            )
                            FilterChip(
                                selected = typeFilter == "income",
                                onClick = { typeFilter = "income" },
                                label = { Text("收入") }
                            )
                        }

                        // Category filter
                        if (allCategories.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text("分类", style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(6.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip(
                                    selected = categoryFilter == "",
                                    onClick = { categoryFilter = "" },
                                    label = { Text("全部") }
                                )
                                allCategories.take(15).forEach { cat ->
                                    FilterChip(
                                        selected = categoryFilter == cat,
                                        onClick = { categoryFilter = cat },
                                        label = { Text(cat) }
                                    )
                                }
                            }
                            if (allCategories.size > 15) {
                                Text("+${allCategories.size - 15} 更多分类…",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        // ── Search result count ──
        if (searchQuery.isNotBlank() || showFilterRow) {
            item {
                Text("共 ${filteredRecords.size} 条记录",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // ═══════════ LIST VIEW ═══════════
        if (viewMode == RecordViewMode.LIST) {
            if (filteredRecords.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.SearchOff, contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            Spacer(Modifier.height(8.dp))
                            Text(if (searchQuery.isNotBlank() || showFilterRow) "没有匹配的记录" else "暂无记录",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {

                grouped.forEach { (_, dayRecords) ->
                    val dayTotal = dayRecords.sumOf { it.amount }
                    val dayLabel = dateFormat.format(Date(dayRecords.first().timestamp))

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(dayLabel, style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold)
                            Text("-¥${String.format("%,.2f", dayTotal)}",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Medium)
                        }
                    }

                    items(dayRecords, key = { it.id }) { record ->
                        RecordCard(record, onClick = {
                            val intent = Intent(context, EditRecordActivity::class.java).apply {
                                putExtra("recordId", record.id)
                                putExtra("amount", String.format("%.2f", record.amount))
                            }
                            context.startActivity(intent)
                        })
                    }
                }
            }
        }

        // ═══════════ CALENDAR VIEW ═══════════
        if (viewMode == RecordViewMode.CALENDAR) {
            item {
                // Month navigation
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Month header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                calendarMonth.add(Calendar.MONTH, -1)
                                selectedDay = null
                            }) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "上月")
                            }
                            Text(
                                SimpleDateFormat("yyyy年M月", Locale.CHINESE).format(calendarMonth.time),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = {
                                calendarMonth.add(Calendar.MONTH, 1)
                                selectedDay = null
                            }) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "下月")
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // Day-of-week header
                        Row(modifier = Modifier.fillMaxWidth()) {
                            val dayNames = listOf("一", "二", "三", "四", "五", "六", "日")
                            dayNames.forEach { name ->
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.labelSmall,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // Calendar grid
                        val cal = remember { Calendar.getInstance() }
                        cal.timeInMillis = calendarMonth.timeInMillis
                        cal.set(Calendar.DAY_OF_MONTH, 1)
                        val firstDayOfWeek = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Mon=0
                        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                        val today = Calendar.getInstance()

                        val cells = mutableListOf<Int?>() // null = empty cell
                        repeat(firstDayOfWeek) { cells.add(null) }
                        for (day in 1..daysInMonth) {
                            cells.add(day)
                        }

                        // 7 columns grid
                        val rows = cells.chunked(7)
                        rows.forEach { week ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                week.forEach { day ->
                                    if (day == null) {
                                        Spacer(Modifier.weight(1f))
                                    } else {
                                        val dayInfo = dayTotals[day]
                                        val isToday = today.get(Calendar.YEAR) == calendarMonth.get(Calendar.YEAR) &&
                                                      today.get(Calendar.MONTH) == calendarMonth.get(Calendar.MONTH) &&
                                                      today.get(Calendar.DAY_OF_MONTH) == day
                                        val isSelected = selectedDay == day

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(2.dp)
                                                .size(40.dp)
                                                .then(
                                                    if (isSelected) Modifier.background(
                                                        MaterialTheme.colorScheme.primaryContainer,
                                                        CircleShape
                                                    ) else Modifier
                                                )
                                                .clickable { selectedDay = if (selectedDay == day) null else day },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    "$day",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                                           else MaterialTheme.colorScheme.onSurface
                                                )
                                                if (dayInfo != null && dayInfo.first > 0) {
                                                    Text(
                                                        "•",
                                                        fontSize = 8.sp,
                                                        color = if (dayInfo.second > dayInfo.third)
                                                                  MaterialTheme.colorScheme.error
                                                               else Color(0xFF4CAF50)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(2.dp))
                        }

                        // Monthly summary
                        val monthTot = remember(dayTotals) {
                            val totalExp = dayTotals.values.sumOf { it.second }
                            val totalInc = dayTotals.values.sumOf { it.third }
                            totalExp to totalInc
                        }
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("支出", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error)
                                Text("¥${String.format("%,.0f", monthTot.first)}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("收入", style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF4CAF50))
                                Text("¥${String.format("%,.0f", monthTot.second)}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4CAF50))
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("结余", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                val bal = monthTot.second - monthTot.first
                                Text("¥${String.format("%,.0f", bal)}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (bal >= 0) MaterialTheme.colorScheme.primary
                                           else MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            // Selected day records
            if (selectedDay != null && selectedDayRecords.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${selectedDay}日 共 ${selectedDayRecords.size} 条",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold)
                    }
                }

                items(selectedDayRecords, key = { it.id }) { record ->
                    RecordCard(record, onClick = {
                        val intent = Intent(context, EditRecordActivity::class.java).apply {
                            putExtra("recordId", record.id)
                            putExtra("amount", String.format("%.2f", record.amount))
                        }
                        context.startActivity(intent)
                    })
                }
            }

            if (selectedDay != null && selectedDayRecords.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text("当日无记录", modifier = Modifier.padding(24.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
