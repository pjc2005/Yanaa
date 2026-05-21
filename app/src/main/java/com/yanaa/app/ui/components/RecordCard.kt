package com.yanaa.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yanaa.app.data.Record
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RecordCard(record: Record, onClick: () -> Unit = {}) {
    val dateFormat = remember { SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()) }
    val isExpense = record.type == "expense"
    val amountColor = if (isExpense) MaterialTheme.colorScheme.error
                      else MaterialTheme.colorScheme.tertiary
    val prefix = if (isExpense) "-" else "+"

    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(record.category,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium)
                    if (record.subcategory.isNotEmpty()) {
                        Text(" · ${record.subcategory}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (record.note.isNotEmpty()) {
                        Text(record.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(dateFormat.format(Date(record.timestamp)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text("${prefix}¥${"%.2f".format(record.amount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = amountColor)
        }
    }
}
