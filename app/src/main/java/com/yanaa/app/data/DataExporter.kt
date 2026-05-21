package com.yanaa.app.data

import android.net.Uri
import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale

object DataExporter {

    fun exportToJson(records: List<Record>): String {
        val arr = JSONArray()
        records.forEach { r ->
            arr.put(JSONObject().apply {
                put("amount", r.amount)
                put("category", r.category)
                put("subcategory", r.subcategory)
                put("note", r.note)
                put("timestamp", r.timestamp)
                put("isAuto", r.isAuto)
            })
        }
        return JSONObject().apply {
            put("version", 1)
            put("exportTime", System.currentTimeMillis())
            put("count", records.size)
            put("records", arr)
        }.toString(2)
    }

    fun importFromUri(context: Context, uri: Uri): List<Record> {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return emptyList()
        val text = inputStream.bufferedReader().use { it.readText() }
        return parseJson(text)
    }

    fun parseJson(text: String): List<Record> {
        val trimmed = text.trim()

        // Detect format: if starts with [, it's QianJi format
        return if (trimmed.startsWith("[")) {
            parseQianJi(JSONArray(trimmed))
        } else {
            parseYanaa(JSONObject(trimmed))
        }
    }

    private fun parseQianJi(arr: JSONArray): List<Record> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val records = mutableListOf<Record>()
        for (i in 0 until arr.length()) {
            try {
                val obj = arr.getJSONObject(i)
                val type = obj.optString("type", "支出")
                // Only import expenses (支出); skip income (收入) for now
                if (type != "支出") continue

                val money = obj.optDouble("money", 0.0)
                val category = obj.optString("category", "其他")
                val dateStr = obj.optString("date", "")
                val timestamp = try {
                    dateFormat.parse(dateStr)?.time ?: System.currentTimeMillis()
                } catch (e: Exception) {
                    System.currentTimeMillis()
                }

                records.add(Record(
                    amount = money,
                    category = category,
                    subcategory = "",
                    note = "",
                    timestamp = timestamp,
                    isAuto = false
                ))
            } catch (_: Exception) {}
        }
        return records
    }

    private fun parseYanaa(json: JSONObject): List<Record> {
        val arr = json.optJSONArray("records") ?: return emptyList()
        val records = mutableListOf<Record>()
        for (i in 0 until arr.length()) {
            try {
                val obj = arr.getJSONObject(i)
                records.add(Record(
                    amount = obj.optDouble("amount", 0.0),
                    category = obj.optString("category", "其他"),
                    subcategory = obj.optString("subcategory", ""),
                    note = obj.optString("note", ""),
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                    isAuto = obj.optBoolean("isAuto", false)
                ))
            } catch (_: Exception) {}
        }
        return records
    }
}
