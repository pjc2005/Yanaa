package com.yanaa.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yanaa.app.data.RecordViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StatsTabScreen(viewModel: RecordViewModel = viewModel()) {
    val records by viewModel.allRecords.collectAsState(initial = emptyList())
    val expense by viewModel.expense.collectAsState(initial = 0.0)

    // Calculate category breakdown
    val categoryStats = remember(records) {
        records.groupBy { it.category }
            .mapValues { (_, recs) ->
                recs.sumOf { it.amount } to recs.size
            }
            .entries
            .sortedByDescending { it.value.first }
    }

    // Monthly trend (last 6 months)
    val monthTrend = remember(records) {
        val cal = Calendar.getInstance()
        val result = mutableListOf<Pair<String, Double>>()
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

            val total = records.filter { it.timestamp in monthStart..monthEnd }
                .sumOf { it.amount }
            val label = SimpleDateFormat("M月", Locale.CHINESE).format(Date(monthStart))
            result.add(label to total)
        }
        result
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Total expense card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("本月总支出", style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer)
                    Text("¥${"%.2f".format( expense)}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer)
                }
            }
        }

        // Category breakdown
        item {
            Text("分类支出排行", style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
        }

        if (categoryStats.isEmpty()) {
            item {
                Text("暂无数据", style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            val maxAmount = categoryStats.firstOrNull()?.value?.first ?: 1.0
            items(categoryStats) { entry ->
                val category = entry.key
                val total = entry.value.first
                val count = entry.value.second
                CategoryBar(category, total, count, maxAmount)
            }
        }

        // Monthly trend
        item {
            Spacer(Modifier.height(8.dp))
            Text("月度趋势", style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold)
        }

        if (monthTrend.any { it.second > 0 }) {
            val maxMonth = monthTrend.maxOf { it.second }.coerceAtLeast(1.0)
            items(monthTrend) { (label, total) ->
                MonthBar(label, total, maxMonth)
            }
        } else {
            item {
                Text("暂无月度数据", style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CategoryBar(category: String, total: Double, count: Int, maxAmount: Double) {
    val fraction = (total / maxAmount).toFloat().coerceIn(0f, 1f)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(category, style = MaterialTheme.typography.bodyMedium)
                Text("¥${"%.2f".format( total)} (${count}笔)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}

@Composable
private fun MonthBar(label: String, total: Double, maxAmount: Double) {
    val fraction = (total / maxAmount).toFloat().coerceIn(0f, 1f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(40.dp))
        Spacer(Modifier.width(8.dp))
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .weight(1f)
                .height(6.dp),
            color = MaterialTheme.colorScheme.tertiary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        Spacer(Modifier.width(8.dp))
        Text("¥${"%.0f".format( total)}",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(60.dp))
    }
}
