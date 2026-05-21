package com.yanaa.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanaa.app.data.AppDatabase
import com.yanaa.app.data.CategoryManager
import com.yanaa.app.data.Record
import com.yanaa.app.ui.theme.YanaaTheme
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class EditRecordActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val amount = intent.getStringExtra("amount") ?: ""

        setContent {
            YanaaTheme {
                EditRecordScreen(
                    initialAmount = amount,
                    context = this@EditRecordActivity,
                    onSave = { record ->
                        val db = AppDatabase.getInstance(this@EditRecordActivity)
                        MainScope().launch {
                            db.recordDao().insert(record)
                            finish()
                        }
                    },
                    onCancel = { finish() }
                )
            }
        }
    }
}

private val categoryIcons = mapOf(
    "餐饮" to Icons.Default.Restaurant,
    "购物" to Icons.Default.ShoppingCart,
    "交通" to Icons.Default.DirectionsBus,
    "生活" to Icons.Default.Home,
    "娱乐" to Icons.Default.Movie,
    "医疗" to Icons.Default.LocalHospital,
    "教育" to Icons.Default.School,
    "其他" to Icons.Default.Category,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRecordScreen(
    initialAmount: String,
    context: android.content.Context,
    onSave: (Record) -> Unit,
    onCancel: () -> Unit
) {
    val categories = remember { CategoryManager.getAll(context) }
    var amount by remember { mutableStateOf(initialAmount) }
    var note by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }
    var selectedSubcategory by remember { mutableStateOf("") }
    var recordType by remember { mutableStateOf("expense") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var showNewSubDialog by remember { mutableStateOf(false) }
    var showNewMainDialog by remember { mutableStateOf(false) }
    var newSubName by remember { mutableStateOf("") }
    var newMainName by remember { mutableStateOf("") }
    val dateTimeFormat = remember { SimpleDateFormat("M月d日 HH:mm", Locale.CHINESE) }

    val currentSubs = remember(selectedCategory, categories) {
        categories.find { it.name == selectedCategory }?.subcategories ?: emptyList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("记一笔") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        val amountValue = amount.toDoubleOrNull() ?: 0.0
                        if (amountValue > 0 && selectedCategory.isNotEmpty()) {
                            onSave(Record(
                                amount = amountValue,
                                type = recordType,
                                category = selectedCategory,
                                subcategory = selectedSubcategory,
                                note = note,
                                timestamp = selectedDate,
                                isAuto = false
                            ))
                        }
                    }) { Text("保存", fontWeight = FontWeight.Bold) }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(20.dp))

            // Amount input
            Text("¥", style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = amount,
                onValueChange = { newVal ->
                    if (newVal.isEmpty() || newVal.matches(Regex("^\\d*\\.?\\d{0,2}$"))) amount = newVal
                },
                placeholder = { Text("0.00", fontSize = 36.sp) },
                textStyle = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold, fontSize = 42.sp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                shape = MaterialTheme.shapes.medium
            )

            Spacer(Modifier.height(12.dp))

            // Expense / Income toggle
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = recordType == "expense",
                    onClick = { recordType = "expense" },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) { Text("支出") }
                SegmentedButton(
                    selected = recordType == "income",
                    onClick = { recordType = "income" },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) { Text("收入") }
            }

            Spacer(Modifier.height(20.dp))

            // Main category grid
            Text("分类", style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                (categories + null).chunked(4).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { item ->
                            if (item != null) {
                                // Normal category chip
                                val isSelected = selectedCategory == item.name
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedCategory = item.name
                                        selectedSubcategory = ""
                                    },
                                    label = { Text(item.name, fontSize = 13.sp) },
                                    leadingIcon = {
                                        Icon(
                                            categoryIcons[item.name] ?: Icons.Default.Category,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    border = if (isSelected)
                                        BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                                    else
                                        BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                )
                            } else {
                                // "+" button for new category
                                OutlinedCard(
                                    onClick = { showNewMainDialog = true },
                                    modifier = Modifier.weight(1f),
                                    border = BorderStroke(1.dp,
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.primary)
                                        Text("新增", fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Subcategory section
            if (selectedCategory.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text("$selectedCategory — 子类",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    currentSubs.chunked(4).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            row.forEach { sub ->
                                val isSubSelected = selectedSubcategory == sub
                                AssistChip(
                                    onClick = { selectedSubcategory = if (isSubSelected) "" else sub },
                                    label = { Text(sub, fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f),
                                    border = if (isSubSelected)
                                        BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary)
                                    else
                                        BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                )
                            }
                        }
                    }
                    TextButton(
                        onClick = { showNewSubDialog = true },
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("新增子类", fontSize = 13.sp)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Note
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("备注（可选）") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            // Date & Time card
            OutlinedCard(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("日期时间", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(dateTimeFormat.format(Date(selectedDate)), fontWeight = FontWeight.Medium)
                }
            }

            Spacer(Modifier.height(24.dp))

            // Save button
            Button(
                onClick = {
                    val amountValue = amount.toDoubleOrNull() ?: 0.0
                    if (amountValue > 0 && selectedCategory.isNotEmpty()) {
                        onSave(Record(
                            amount = amountValue,
                            category = selectedCategory,
                            subcategory = selectedSubcategory,
                            note = note,
                            timestamp = selectedDate,
                            isAuto = false
                        ))
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = amount.toDoubleOrNull()?.let { it > 0 } == true && selectedCategory.isNotEmpty()
            ) {
                Text("保存记录", fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    // Date picker dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { dateMillis ->
                        // Preserve the time portion from selectedDate
                        val cal = Calendar.getInstance()
                        cal.timeInMillis = dateMillis
                        val oldCal = Calendar.getInstance()
                        oldCal.timeInMillis = selectedDate
                        cal.set(Calendar.HOUR_OF_DAY, oldCal.get(Calendar.HOUR_OF_DAY))
                        cal.set(Calendar.MINUTE, oldCal.get(Calendar.MINUTE))
                        cal.set(Calendar.SECOND, 0)
                        selectedDate = cal.timeInMillis
                        showDatePicker = false
                        showTimePicker = true  // Show time picker after date
                    }
                }) { Text("下一步 — 时间") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    // Time picker dialog
    if (showTimePicker) {
        val cal = Calendar.getInstance().apply { timeInMillis = selectedDate }
        val timePickerState = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("选择时间") },
            text = {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val cal = Calendar.getInstance()
                    cal.timeInMillis = selectedDate
                    cal.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                    cal.set(Calendar.MINUTE, timePickerState.minute)
                    cal.set(Calendar.SECOND, 0)
                    selectedDate = cal.timeInMillis
                    showTimePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("取消") }
            }
        )
    }

    // New subcategory dialog
    if (showNewSubDialog) {
        AlertDialog(
            onDismissRequest = { showNewSubDialog = false },
            title = { Text("新增子类 — $selectedCategory") },
            text = {
                OutlinedTextField(
                    value = newSubName,
                    onValueChange = { newSubName = it },
                    label = { Text("子类名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val name = newSubName.trim()
                        if (name.isNotBlank() && name !in currentSubs) {
                            CategoryManager.addSubcategory(context, selectedCategory, name)
                            selectedSubcategory = name
                            newSubName = ""
                            showNewSubDialog = false
                        }
                    },
                    enabled = newSubName.isNotBlank()
                ) { Text("添加") }
            },
            dismissButton = {
                TextButton(onClick = { showNewSubDialog = false; newSubName = "" }) { Text("取消") }
            }
        )
    }

    // New main category dialog
    if (showNewMainDialog) {
        AlertDialog(
            onDismissRequest = { showNewMainDialog = false },
            title = { Text("新增分类") },
            text = {
                OutlinedTextField(
                    value = newMainName,
                    onValueChange = { newMainName = it },
                    label = { Text("分类名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val name = newMainName.trim()
                        if (name.isNotBlank()) {
                            CategoryManager.addMainCategory(context, name)
                            selectedCategory = name
                            selectedSubcategory = ""
                            newMainName = ""
                            showNewMainDialog = false
                        }
                    },
                    enabled = newMainName.isNotBlank()
                ) { Text("添加") }
            },
            dismissButton = {
                TextButton(onClick = { showNewMainDialog = false; newMainName = "" }) { Text("取消") }
            }
        )
    }
}
