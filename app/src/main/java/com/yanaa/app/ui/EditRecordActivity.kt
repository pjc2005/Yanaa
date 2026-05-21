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
import com.yanaa.app.data.CategoryDef
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

data class CategoryOption(
    val id: String,
    val name: String,
    val icon: ImageVector
)

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
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var showNewSubDialog by remember { mutableStateOf(false) }
    var newSubName by remember { mutableStateOf("") }
    val dateFormat = remember { SimpleDateFormat("M月d日 EEEE", Locale.CHINESE) }

    // Get subcategories for selected main category
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
                    TextButton(
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
                        }
                    ) { Text("保存", fontWeight = FontWeight.Bold) }
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

            Spacer(Modifier.height(20.dp))

            // Main category grid
            Text("分类", style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.chunked(4).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { cat ->
                            val isSelected = selectedCategory == cat.name
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedCategory = cat.name
                                    selectedSubcategory = ""
                                },
                                label = { Text(cat.name, fontSize = 13.sp) },
                                leadingIcon = {
                                    Icon(
                                        categoryIcons[cat.name] ?: Icons.Default.Category,
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
                        }
                    }
                }
            }

            // Subcategory section (only when a main category is selected)
            if (selectedCategory.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text(selectedCategory + " — 子类",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))

                // Subcategory chips in a flow layout
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
                                    onClick = {
                                        selectedSubcategory = if (isSubSelected) "" else sub
                                    },
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
                    // Add new subcategory button
                    TextButton(
                        onClick = { showNewSubDialog = true },
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null,
                            modifier = Modifier.size(16.dp))
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

            // Date
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
                        Text("日期", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(dateFormat.format(Date(selectedDate)), fontWeight = FontWeight.Medium)
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

    // Date picker
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedDate = it }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) { DatePicker(state = datePickerState) }
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
                        if (newSubName.isNotBlank() && newSubName !in currentSubs) {
                            CategoryManager.addSubcategory(context, selectedCategory, newSubName.trim())
                            selectedSubcategory = newSubName.trim()
                            newSubName = ""
                            showNewSubDialog = false
                        }
                    },
                    enabled = newSubName.isNotBlank() && newSubName !in currentSubs
                ) { Text("添加") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showNewSubDialog = false
                    newSubName = ""
                }) { Text("取消") }
            }
        )
    }
}
