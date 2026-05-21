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
import com.yanaa.app.data.*
import com.yanaa.app.ui.theme.YanaaTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class BudgetActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            YanaaTheme { BudgetScreen() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getInstance(context) }

    var currentMonth by remember { mutableStateOf(
        SimpleDateFormat("yyyy-MM", Locale.CHINESE).format(Date())
    ) }
    var budgets by remember { mutableStateOf<List<Budget>>(emptyList()) }
    var allRecords by remember { mutableStateOf<List<Record>>(emptyList()) }

    // Load data
    fun load() {
        scope.launch {
            val b = withContext(Dispatchers.IO) { db.budgetDao().getByMonth(currentMonth) }
            val r = withContext(Dispatchers.IO) { db.recordDao().getAllSync() }
            budgets = b
            allRecords = r
        }
    }

    LaunchedEffect(currentMonth) { load() }

    // Compute actual spending
    val cal = Calendar.getInstance()
    val ym = currentMonth.split("-")
    cal.set(ym[0].toInt(), ym[1].toInt() - 1, 1, 0, 0, 0)
    val monthStart = cal.timeInMillis
    cal.add(Calendar.MONTH, 1)
    val monthEnd = cal.timeInMillis - 1

    val monthRecords = remember(allRecords, currentMonth) {
        allRecords.filter { it.timestamp in monthStart..monthEnd }
    }
    val monthExpense = remember(monthRecords) {
        monthRecords.filter { it.type == "expense" }.sumOf { it.amount }
    }
    val totalBudget = remember(budgets) {
        budgets.find { it.category == "" }
    }
    val categoryBudgets = remember(budgets) {
        budgets.filter { it.category != "" }
    }
    val allCategories = remember(monthRecords) {
        monthRecords.filter { it.type == "expense" }
            .groupBy { it.category }
            .mapValues { (_, recs) -> recs.sumOf { it.amount } }
    }

    // Dialogs
    var showTotalBudgetDialog by remember { mutableStateOf(false) }
    var showCategoryBudgetDialog by remember { mutableStateOf(false) }
    var dialogAmount by remember { mutableStateOf("") }
    var dialogCategory by remember { mutableStateOf("") }
    var dialogCategoryAmount by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("预算管理") },
                navigationIcon = {
                    IconButton(onClick = { (context as? ComponentActivity)?.finish() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    // Month navigation
                    IconButton(onClick = {
                        cal.timeInMillis = monthStart
                        cal.add(Calendar.MONTH, -1)
                        currentMonth = SimpleDateFormat("yyyy-MM", Locale.CHINESE).format(cal.time)
                    }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "上个月")
                    }
                    Text(SimpleDateFormat("yyyy年M月", Locale.CHINESE).format(Date(monthStart)),
                        style = MaterialTheme.typography.titleSmall)
                    IconButton(onClick = {
                        cal.timeInMillis = monthStart
                        cal.add(Calendar.MONTH, 1)
                        currentMonth = SimpleDateFormat("yyyy-MM", Locale.CHINESE).format(cal.time)
                    }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "下个月")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Total budget card
            item {
                val budgetAmount = totalBudget?.amount ?: 0.0
                val pct = if (budgetAmount > 0) (monthExpense / budgetAmount * 100).coerceAtMost(100.0) else 0.0
                val isOver = budgetAmount > 0 && monthExpense > budgetAmount

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isOver) MaterialTheme.colorScheme.errorContainer
                                      else MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("总预算",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isOver) MaterialTheme.colorScheme.onErrorContainer
                                       else MaterialTheme.colorScheme.onPrimaryContainer)

                            TextButton(onClick = { showTotalBudgetDialog = true }) {
                                Text(if (totalBudget == null) "设置" else "修改")
                            }
                        }

                        if (budgetAmount > 0) {
                            Spacer(Modifier.height(12.dp))
                            LinearProgressIndicator(
                                progress = { (pct / 100.0).toFloat().coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(12.dp),
                                color = if (isOver) MaterialTheme.colorScheme.error
                                       else MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("已支出 ¥${String.format("%,.2f", monthExpense)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium)
                                Text("预算 ¥${String.format("%,.2f", budgetAmount)}",
                                    style = MaterialTheme.typography.bodyMedium)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text("已用 ${String.format("%.1f", pct)}%",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (isOver) MaterialTheme.colorScheme.error
                                       else MaterialTheme.colorScheme.primary)
                        } else {
                            Spacer(Modifier.height(8.dp))
                            Text("点击「设置」为本月设定预算",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f))
                        }
                    }
                }
            }

            // Category budgets section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("分类预算",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold)
                    TextButton(onClick = { showCategoryBudgetDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("添加")
                    }
                }
            }

            if (categoryBudgets.isEmpty() && totalBudget == null) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text("暂无分类预算", modifier = Modifier.padding(24.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Per-category budget cards
            items(categoryBudgets) { budget ->
                val actual = allCategories[budget.category] ?: 0.0
                val pct = if (budget.amount > 0) (actual / budget.amount * 100).coerceAtMost(100.0) else 0.0
                val isOver = budget.amount > 0 && actual > budget.amount

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(budget.category, style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium)
                            Row {
                                Text("¥${String.format("%,.0f", actual)} / ¥${String.format("%,.0f", budget.amount)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            withContext(Dispatchers.IO) { db.budgetDao().deleteById(budget.id) }
                                            load()
                                        }
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "删除",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { (pct / 100.0).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = if (isOver) MaterialTheme.colorScheme.error
                                   else MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text("${String.format("%.1f", pct)}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isOver) MaterialTheme.colorScheme.error
                                   else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Expense breakdown (no budget set) as reference
            if (totalBudget == null && categoryBudgets.isEmpty() && allCategories.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text("本月支出分布（仅供参考）",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                items(allCategories.entries.toList().sortedByDescending { it.value }) { (category, amount) ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(category, style = MaterialTheme.typography.bodyMedium)
                            Text("¥${String.format("%,.2f", amount)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    // Total budget dialog
    if (showTotalBudgetDialog) {
        AlertDialog(
            onDismissRequest = { showTotalBudgetDialog = false },
            title = { Text(if (totalBudget == null) "设置总预算" else "修改总预算") },
            text = {
                Column {
                    Text("输入本月预算金额：", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = dialogAmount,
                        onValueChange = { dialogAmount = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("金额") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        prefix = { Text("¥") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val amt = dialogAmount.toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                db.budgetDao().upsert(Budget(
                                    id = totalBudget?.id ?: 0,
                                    category = "",
                                    amount = amt,
                                    month = currentMonth
                                ))
                            }
                            load()
                        }
                    }
                    showTotalBudgetDialog = false
                    dialogAmount = ""
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showTotalBudgetDialog = false
                    dialogAmount = ""
                }) { Text("取消") }
            }
        )
    }

    // Category budget dialog
    if (showCategoryBudgetDialog) {
        val expenseCategories = remember(monthRecords) {
            monthRecords.filter { it.type == "expense" }
                .map { it.category }.distinct().sorted()
        }
        AlertDialog(
            onDismissRequest = { showCategoryBudgetDialog = false },
            title = { Text("添加分类预算") },
            text = {
                Column {
                    if (expenseCategories.isNotEmpty()) {
                        Text("选择分类：", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        // Category selector as chips
                        @OptIn(ExperimentalLayoutApi::class)
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            expenseCategories.forEach { cat ->
                                FilterChip(
                                    selected = dialogCategory == cat,
                                    onClick = { dialogCategory = cat },
                                    label = { Text(cat) }
                                )
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = dialogCategory,
                            onValueChange = { dialogCategory = it },
                            label = { Text("分类名称") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = dialogCategoryAmount,
                        onValueChange = { dialogCategoryAmount = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("预算金额") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        prefix = { Text("¥") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val amt = dialogCategoryAmount.toDoubleOrNull() ?: 0.0
                    if (dialogCategory.isNotBlank() && amt > 0) {
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                db.budgetDao().upsert(Budget(
                                    category = dialogCategory,
                                    amount = amt,
                                    month = currentMonth
                                ))
                            }
                            load()
                        }
                    }
                    showCategoryBudgetDialog = false
                    dialogCategory = ""
                    dialogCategoryAmount = ""
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCategoryBudgetDialog = false
                    dialogCategory = ""
                    dialogCategoryAmount = ""
                }) { Text("取消") }
            }
        )
    }
}
