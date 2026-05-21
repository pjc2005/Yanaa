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
import com.yanaa.app.data.Record
import com.yanaa.app.data.RecordViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RecordsTabScreen(viewModel: RecordViewModel = viewModel()) {
    val records by viewModel.allRecords.collectAsState(initial = emptyList())
    val dateFormat = remember { SimpleDateFormat("yyyy年MM月dd日", Locale.CHINESE) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    // Group records by date (year + dayOfYear)
    val grouped = remember(records) {
        records.groupBy { record ->
            val cal = Calendar.getInstance()
            cal.timeInMillis = record.timestamp
            cal.get(Calendar.YEAR) to cal.get(Calendar.DAY_OF_YEAR)
        }.entries.sortedByDescending { (key, _) ->
            key.first.toLong() * 1000 + key.second
        }
    }

    if (records.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("暂无记录", style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            grouped.forEach { (_, dayRecords) ->
                val dayTotal = dayRecords.sumOf { it.amount }
                val dayLabel = dateFormat.format(Date(dayRecords.first().timestamp))

                // Date header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(dayLabel, style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold)
                        Text("-¥${String.format("%.2f", dayTotal)}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium)
                    }
                }

                items(dayRecords, key = { it.id }) { record ->
                    RecordCard(record)
                }
            }
        }
    }
}
