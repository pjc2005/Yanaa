package com.yanaa.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.yanaa.app.data.AppDatabase
import com.yanaa.app.data.SavingsPlan
import com.yanaa.app.ui.theme.YanaaTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class SavingsPlanActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            YanaaTheme { SavingsPlanScreen() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SavingsPlanScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getInstance(context) }
    var plans by remember { mutableStateOf<List<SavingsPlan>>(emptyList()) }

    fun load() {
        scope.launch {
            plans = withContext(Dispatchers.IO) { db.savingsPlanDao().getAll() }
        }
    }
    LaunchedEffect(Unit) { load() }

    val totalSaved = remember(plans) { plans.filter { it.isActive }.sumOf { it.currentAmount } }
    val totalTarget = remember(plans) { plans.filter { it.isActive }.sumOf { it.targetAmount } }
    val totalPct = if (totalTarget > 0) (totalSaved / totalTarget * 100).coerceAtMost(100.0) else 0.0

    // Dialogs
    var showNewPlanDialog by remember { mutableStateOf(false) }
    var showAddMoneyDialog by remember { mutableStateOf<Long?>(null) }
    var showEditDialog by remember { mutableStateOf<SavingsPlan?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("攒钱计划") },
                navigationIcon = {
                    IconButton(onClick = { (context as? ComponentActivity)?.finish() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewPlanDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "新建计划")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Total overview card
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("攒钱总进度", style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(Modifier.height(16.dp))
                        Text("¥${String.format("%,.2f", totalSaved)} / ¥${String.format("%,.2f", totalTarget)}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { (totalPct / 100.0).toFloat() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp),
                            color = Color(0xFF4CAF50),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("已完成 ${String.format("%.1f", totalPct)}%",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }

            // Active plans
            val activePlans = plans.filter { it.isActive }
            if (activePlans.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.AccountBalance, contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(12.dp))
                            Text("还没有攒钱计划", style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("点击右下角 + 新建你的第一个目标",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            items(activePlans) { plan ->
                val pct = if (plan.targetAmount > 0) (plan.currentAmount / plan.targetAmount * 100).coerceAtMost(100.0) else 0.0
                val remaining = plan.targetAmount - plan.currentAmount
                val daysLeft = plan.deadline?.let { deadline ->
                    val diff = deadline - System.currentTimeMillis()
                    if (diff > 0) (diff / 86400000).toInt() + 1 else 0
                }

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Savings, contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(plan.name, style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold)
                            }
                            Row {
                                IconButton(
                                    onClick = { showEditDialog = plan },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "编辑",
                                        modifier = Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = { showDeleteConfirm = plan.id },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "删除",
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // Amount row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("已存 ¥${String.format("%,.2f", plan.currentAmount)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold)
                            Text("目标 ¥${String.format("%,.2f", plan.targetAmount)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Spacer(Modifier.height(8.dp))

                        // Progress bar
                        LinearProgressIndicator(
                            progress = { (pct / 100.0).toFloat() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp),
                            color = if (pct >= 100) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )

                        Spacer(Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${String.format("%.1f", pct)}%",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (pct >= 100) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary)
                            if (remaining > 0 && daysLeft != null) {
                                Text("剩余 ${daysLeft}天",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        if (remaining > 0) {
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = { showAddMoneyDialog = plan.id },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("存钱（还差 ¥${String.format("%,.0f", remaining)}）")
                            }
                        } else {
                            Spacer(Modifier.height(8.dp))
                            Surface(
                                color = Color(0xFF4CAF50).copy(alpha = 0.15f),
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("目标达成！🎉",
                                    modifier = Modifier.padding(12.dp),
                                    color = Color(0xFF4CAF50),
                                    fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Completed plans
            val completedPlans = plans.filter { !it.isActive }
            if (completedPlans.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text("已完成", style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                items(completedPlans) { plan ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(plan.name, style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium)
                                Text("¥${String.format("%,.2f", plan.currentAmount)} / ¥${String.format("%,.2f", plan.targetAmount)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("已完成", style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF4CAF50))
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(72.dp)) }
        }
    }

    // ───── New Plan Dialog ─────
    if (showNewPlanDialog) {
        NewPlanDialog(
            onDismiss = { showNewPlanDialog = false },
            onSave = { name, target, deadline ->
                scope.launch {
                    withContext(Dispatchers.IO) {
                        db.savingsPlanDao().upsert(SavingsPlan(
                            name = name,
                            targetAmount = target,
                            deadline = deadline
                        ))
                    }
                    load()
                }
                showNewPlanDialog = false
            }
        )
    }

    // ───── Add Money Dialog ─────
    showAddMoneyDialog?.let { planId ->
        AddMoneyDialog(
            onDismiss = { showAddMoneyDialog = null },
            onAdd = { amount ->
                scope.launch {
                    withContext(Dispatchers.IO) { db.savingsPlanDao().addAmount(planId, amount) }
                    load()
                }
                showAddMoneyDialog = null
            }
        )
    }

    // ───── Edit Dialog ─────
    showEditDialog?.let { plan ->
        EditPlanDialog(
            plan = plan,
            onDismiss = { showEditDialog = null },
            onSave = { name, target, deadline ->
                scope.launch {
                    withContext(Dispatchers.IO) {
                        db.savingsPlanDao().upsert(plan.copy(
                            name = name,
                            targetAmount = target,
                            deadline = deadline
                        ))
                    }
                    load()
                }
                showEditDialog = null
            }
        )
    }

    // ───── Delete Confirm ─────
    showDeleteConfirm?.let { planId ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("确认删除") },
            text = { Text("确定要删除这个攒钱计划吗？\n累计存入的金额将会丢失。") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        withContext(Dispatchers.IO) { db.savingsPlanDao().deleteById(planId) }
                        load()
                    }
                    showDeleteConfirm = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun NewPlanDialog(onDismiss: () -> Unit, onSave: (String, Double, Long?) -> Unit) {
    var name by remember { mutableStateOf("") }
    var targetAmount by remember { mutableStateOf("") }
    var hasDeadline by remember { mutableStateOf(false) }
    var deadlineDate by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建攒钱计划") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("计划名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = targetAmount,
                    onValueChange = { targetAmount = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("目标金额") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    prefix = { Text("¥") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = hasDeadline, onCheckedChange = { hasDeadline = it })
                    Text("设定截止日期", style = MaterialTheme.typography.bodyMedium)
                }
                if (hasDeadline) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = deadlineDate,
                        onValueChange = { deadlineDate = it },
                        label = { Text("截止日期 (yyyy-MM-dd)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val target = targetAmount.toDoubleOrNull() ?: 0.0
                    val deadline = if (hasDeadline && deadlineDate.isNotBlank()) {
                        try {
                            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                .parse(deadlineDate)?.time
                        } catch (_: Exception) { null }
                    } else null
                    if (name.isNotBlank() && target > 0) {
                        onSave(name, target, deadline)
                    }
                },
                enabled = name.isNotBlank() && targetAmount.toDoubleOrNull() ?: 0.0 > 0
            ) { Text("创建") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun AddMoneyDialog(onDismiss: () -> Unit, onAdd: (Double) -> Unit) {
    var amount by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("存入金额") },
        text = {
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("存入金额") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                prefix = { Text("¥") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    if (amt > 0) onAdd(amt)
                },
                enabled = amount.toDoubleOrNull() ?: 0.0 > 0
            ) { Text("确认存入") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun EditPlanDialog(plan: SavingsPlan, onDismiss: () -> Unit, onSave: (String, Double, Long?) -> Unit) {
    var name by remember { mutableStateOf(plan.name) }
    var targetAmount by remember { mutableStateOf(String.format("%.0f", plan.targetAmount)) }
    var hasDeadline by remember { mutableStateOf(plan.deadline != null) }
    var deadlineDate by remember {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        mutableStateOf(plan.deadline?.let { sdf.format(Date(it)) } ?: "")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑计划") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("计划名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = targetAmount,
                    onValueChange = { targetAmount = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("目标金额") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    prefix = { Text("¥") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = hasDeadline, onCheckedChange = { hasDeadline = it })
                    Text("设定截止日期", style = MaterialTheme.typography.bodyMedium)
                }
                if (hasDeadline) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = deadlineDate,
                        onValueChange = { deadlineDate = it },
                        label = { Text("截止日期 (yyyy-MM-dd)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val target = targetAmount.toDoubleOrNull() ?: 0.0
                val deadline = if (hasDeadline && deadlineDate.isNotBlank()) {
                    try {
                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            .parse(deadlineDate)?.time
                    } catch (_: Exception) { null }
                } else null
                if (name.isNotBlank() && target > 0) onSave(name, target, deadline)
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
