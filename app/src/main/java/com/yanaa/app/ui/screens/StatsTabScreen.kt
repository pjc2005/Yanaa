package com.yanaa.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yanaa.app.data.Record
import com.yanaa.app.data.RecordViewModel
import java.text.SimpleDateFormat
import java.util.*

private enum class StatsPeriod { MONTH, YEAR, ALL }

@Composable
fun StatsTabScreen(viewModel: RecordViewModel = viewModel()) {
    val records by viewModel.allRecords.collectAsState(initial = emptyList())
    var statsPeriod by remember { mutableStateOf(StatsPeriod.MONTH) }

    // Filter records by selected period
    val filteredRecords = remember(records, statsPeriod) {
        if (statsPeriod == StatsPeriod.ALL) records
        else {
            val cal = Calendar.getInstance()
            val now = System.currentTimeMillis()
            val startMillis = when (statsPeriod) {
                StatsPeriod.MONTH -> {
                    cal.timeInMillis = now
                    cal.set(Calendar.DAY_OF_MONTH, 1)
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    cal.timeInMillis
                }
                StatsPeriod.YEAR -> {
                    cal.timeInMillis = now
                    cal.set(Calendar.DAY_OF_YEAR, 1)
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    cal.timeInMillis
                }
                else -> 0L
            }
            records.filter { it.timestamp >= startMillis }
        }
    }

    // Compute period stats
    val totalExpense = remember(filteredRecords) {
        filteredRecords.filter { it.type == "expense" }.sumOf { it.amount }
    }
    val totalIncome = remember(filteredRecords) {
        filteredRecords.filter { it.type == "income" }.sumOf { it.amount }
    }
    val totalBalance = totalIncome - totalExpense

    // Category breakdown: expense categories sorted by amount desc
    val expenseCategories = remember(filteredRecords) {
        filteredRecords.filter { it.type == "expense" }
            .groupBy { it.category }
            .mapValues { (_, recs) -> recs.sumOf { it.amount } to recs.size }
            .entries
            .sortedByDescending { it.value.first }
    }
    val expenseTotal = remember(expenseCategories) { expenseCategories.sumOf { it.value.first } }

    // Category breakdown: income categories sorted by amount desc
    val incomeCategories = remember(filteredRecords) {
        filteredRecords.filter { it.type == "income" }
            .groupBy { it.category }
            .mapValues { (_, recs) -> recs.sumOf { it.amount } to recs.size }
            .entries
            .sortedByDescending { it.value.first }
    }
    val incomeTotal = remember(incomeCategories) { incomeCategories.sumOf { it.value.first } }

    // Monthly trend (last 6 months - expense and income stacked)
    val monthTrend = remember(records) {
        val cal = Calendar.getInstance()
        val result = mutableListOf<Triple<String, Double, Double>>() // label, expense, income
        for (i in 5 downTo 0) {
            cal.timeInMillis = System.currentTimeMillis()
            cal.add(Calendar.MONTH, -i)
            val month = cal.get(Calendar.MONTH)
            val year = cal.get(Calendar.YEAR)
            val monthStart = Calendar.getInstance().apply {
                set(year, month, 1, 0, 0, 0)
            }.timeInMillis
            val monthEnd = Calendar.getInstance().apply {
                set(year, month, 1, 0, 0, 0)
                add(Calendar.MONTH, 1)
                add(Calendar.MILLISECOND, -1)
            }.timeInMillis

            val monthRecs = records.filter { it.timestamp in monthStart..monthEnd }
            val exp = monthRecs.filter { it.type == "expense" }.sumOf { it.amount }
            val inc = monthRecs.filter { it.type == "income" }.sumOf { it.amount }
            val label = SimpleDateFormat("M月", Locale.CHINESE).format(Date(monthStart))
            result.add(Triple(label, exp, inc))
        }
        result
    }
    val maxMonthValue = remember(monthTrend) {
        monthTrend.maxOfOrNull { maxOf(it.second, it.third) }?.coerceAtLeast(1.0) ?: 1.0
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Period selector
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatsPeriod.entries.forEach { p ->
                    val selected = statsPeriod == p
                    FilterChip(
                        selected = selected,
                        onClick = { statsPeriod = p },
                        label = {
                            Text(when (p) {
                                StatsPeriod.MONTH -> "本月"
                                StatsPeriod.YEAR -> "本年"
                                StatsPeriod.ALL -> "全部"
                            })
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Summary card (QianJi style)
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        when (statsPeriod) {
                            StatsPeriod.MONTH -> "本月概览"
                            StatsPeriod.YEAR -> "本年概览"
                            StatsPeriod.ALL -> "全部概览"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Spacer(Modifier.height(16.dp))

                    // Income / Expense row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("收入", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f))
                            Text("¥${String.format("%,.2f", totalIncome)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("支出", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f))
                            Text("¥${String.format("%,.2f", totalExpense)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error)
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Proportion bar (like QianJi)
                    val totalBoth = totalIncome + totalExpense
                    val incomePct = if (totalBoth > 0) (totalIncome / totalBoth * 100) else 0.0
                    val expensePct = if (totalBoth > 0) (totalExpense / totalBoth * 100) else 0.0
                    if (totalBoth > 0) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            // Income bar
                            if (totalIncome > 0) {
                                Box(
                                    modifier = Modifier
                                        .weight((incomePct / (incomePct + expensePct)).toFloat().coerceAtLeast(0.01f))
                                        .height(20.dp)
                                        .padding(end = 1.dp)
                                ) {
                                    Surface(
                                        color = Color(0xFF4CAF50), // green for income
                                        shape = MaterialTheme.shapes.small,
                                        modifier = Modifier.fillMaxSize()
                                    ) { /* income bar */ }
                                }
                            }
                            // Expense bar
                            if (totalExpense > 0) {
                                Box(
                                    modifier = Modifier
                                        .weight((expensePct / (incomePct + expensePct)).toFloat().coerceAtLeast(0.01f))
                                        .height(20.dp)
                                        .padding(start = 1.dp)
                                ) {
                                    Surface(
                                        color = Color(0xFFE53935), // red for expense
                                        shape = MaterialTheme.shapes.small,
                                        modifier = Modifier.fillMaxSize()
                                    ) { /* expense bar */ }
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("收入 ${String.format("%.1f", incomePct)}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF4CAF50))
                            Text("支出 ${String.format("%.1f", expensePct)}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFE53935))
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Balance
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("结余", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                        Text(
                            "¥${String.format("%,.2f", totalBalance)}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (totalBalance >= 0) MaterialTheme.colorScheme.onPrimaryContainer
                                   else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // ═══ Expense Category Ranking ═══
        if (expenseCategories.isNotEmpty()) {
            item {
                Spacer(Modifier.height(4.dp))
                Text("支出排行",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold)
            }

            val maxExpense = expenseCategories.first().value.first.coerceAtLeast(1.0)
            items(expenseCategories) { entry ->
                val pct = if (expenseTotal > 0) entry.value.first / expenseTotal * 100 else 0.0
                CategoryRankBar(
                    name = entry.key,
                    total = entry.value.first,
                    count = entry.value.second,
                    pct = pct,
                    maxAmount = maxExpense,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        // ═══ Income Category Ranking ═══
        if (incomeCategories.isNotEmpty()) {
            item {
                Spacer(Modifier.height(8.dp))
                Text("收入排行",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold)
            }

            val maxIncome = incomeCategories.first().value.first.coerceAtLeast(1.0)
            items(incomeCategories) { entry ->
                val pct = if (incomeTotal > 0) entry.value.first / incomeTotal * 100 else 0.0
                CategoryRankBar(
                    name = entry.key,
                    total = entry.value.first,
                    count = entry.value.second,
                    pct = pct,
                    maxAmount = maxIncome,
                    color = Color(0xFF4CAF50)
                )
            }
        }

        // ═══ Monthly Trend ═══
        item {
            Spacer(Modifier.height(8.dp))
            Text("月度趋势",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold)
        }

        if (monthTrend.all { it.second == 0.0 && it.third == 0.0 }) {
            item {
                Text("暂无数据", style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            items(monthTrend) { (label, exp, inc) ->
            MonthTrendBar(label, exp, inc, maxMonthValue)
            }
        }

        // Bottom spacer
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun CategoryRankBar(
    name: String,
    total: Double,
    count: Int,
    pct: Double,
    maxAmount: Double,
    color: Color
) {
    val fraction = (total / maxAmount).toFloat().coerceIn(0f, 1f)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(name, style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium)
                    Spacer(Modifier.width(8.dp))
                    Text("${count}笔",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("¥${String.format("%,.2f", total)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = color,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Spacer(Modifier.height(2.dp))
            Text("占比 ${String.format("%.1f", pct)}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MonthTrendBar(label: String, expense: Double, income: Double, maxAmount: Double) {
    val expFraction = (expense / maxAmount).toFloat().coerceIn(0f, 1f)
    val incFraction = (income / maxAmount).toFloat().coerceIn(0f, 1f)
    val maxFraction = maxOf(expFraction, incFraction)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Month label
            Text(label, style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))

            if (expense > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("支", style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFE53935),
                        modifier = Modifier.width(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(14.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { expFraction / maxFraction.coerceAtLeast(0.01f) },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(end = 4.dp),
                            color = Color(0xFFE53935),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    }
                    Text("¥${String.format("%,.0f", expense)}",
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(72.dp))
                }
            }
            if (income > 0) {
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("收", style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.width(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(14.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { incFraction / maxFraction.coerceAtLeast(0.01f) },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(end = 4.dp),
                            color = Color(0xFF4CAF50),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    }
                    Text("¥${String.format("%,.0f", income)}",
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(72.dp))
                }
            }

            if (expense == 0.0 && income == 0.0) {
                Text("无记录", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
