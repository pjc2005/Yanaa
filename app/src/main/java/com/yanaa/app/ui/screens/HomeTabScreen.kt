package com.yanaa.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import android.content.Intent
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yanaa.app.data.Period
import com.yanaa.app.data.RecordViewModel
import com.yanaa.app.ui.components.RecordCard
import com.yanaa.app.ui.EditRecordActivity

@Composable
fun HomeTabScreen(viewModel: RecordViewModel = viewModel()) {
    val records by viewModel.allRecords.collectAsState(initial = emptyList())
    val expense by viewModel.expense.collectAsState(initial = 0.0)
    val income by viewModel.income.collectAsState(initial = 0.0)
    val balance by viewModel.balance.collectAsState(initial = 0.0)
    val count by viewModel.count.collectAsState(initial = 0)
    val currentPeriod by viewModel.period.collectAsState()
    val context = LocalContext.current

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
                Period.entries.forEach { p ->
                    val selected = currentPeriod == p
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.setPeriod(p) },
                        label = {
                            Text(when (p) {
                                Period.WEEK -> "本周"
                                Period.MONTH -> "本月"
                                Period.YEAR -> "本年"
                            })
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Summary card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Period label
                    Text(
                        when (currentPeriod) {
                            Period.WEEK -> "本周概览"
                            Period.MONTH -> "本月概览"
                            Period.YEAR -> "本年概览"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.height(16.dp))

                    // Balance (large)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text("结余",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                            Text(
                                "¥${String.format("%,.2f", balance)}",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (balance >= 0) MaterialTheme.colorScheme.onPrimaryContainer
                                       else MaterialTheme.colorScheme.error
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${count}笔",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // Income & Expense row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("支出",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f))
                            Text("¥${String.format("%,.2f", expense)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("收入",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f))
                            Text("¥${String.format("%,.2f", income)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                }
            }
        }

        // Total summary card (all-time)
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("累计 (全部)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val totalExp by viewModel.totalExpense.collectAsState(initial = 0.0)
                            Text("支出",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("¥${String.format("%,.2f", totalExp)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val totalInc by viewModel.totalIncome.collectAsState(initial = 0.0)
                            Text("收入",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("¥${String.format("%,.2f", totalInc)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val totalBal by viewModel.totalBalance.collectAsState(initial = 0.0)
                            Text("结余",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("¥${String.format("%,.2f", totalBal)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (totalBal >= 0) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        // Recent records header
        item {
            Text("最近记录",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp))
        }

        val recentRecords = records.take(20)
        if (recentRecords.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text("暂无记录",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp))
                }
            }
        } else {
            items(recentRecords, key = { it.id }) { record ->
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
