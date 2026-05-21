package com.yanaa.app.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

object DataExporter {

    fun exportToJson(context: Context, records: List<Record>): String {
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

    fun importFromJson(context: Context, uri: Uri): List<Record> {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return emptyList()
        val text = inputStream.bufferedReader().use { it.readText() }
        val json = JSONObject(text)
        val arr = json.optJSONArray("records") ?: return emptyList()
        val records = mutableListOf<Record>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            records.add(Record(
                amount = obj.optDouble("amount", 0.0),
                category = obj.optString("category", "其他"),
                subcategory = obj.optString("subcategory", ""),
                note = obj.optString("note", ""),
                timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                isAuto = obj.optBoolean("isAuto", false)
            ))
        }
        return records
    }
}
