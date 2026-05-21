package com.yanaa.app.data

import android.content.Context

data class CategoryDef(
    val name: String,
    val subcategories: List<String>
)

object CategoryManager {

    private val defaults = listOf(
        CategoryDef("餐饮", listOf("早餐", "午餐", "晚餐", "水果", "奶茶", "零食", "外卖")),
        CategoryDef("购物", listOf("日用品", "衣服", "数码", "家居", "买菜")),
        CategoryDef("交通", listOf("公交", "地铁", "打车", "加油", "停车")),
        CategoryDef("生活", listOf("水电", "话费", "网费", "物业", "理发")),
        CategoryDef("娱乐", listOf("电影", "游戏", "旅游", "健身", "聚会")),
        CategoryDef("医疗", listOf("门诊", "药品", "体检", "牙科")),
        CategoryDef("教育", listOf("书籍", "课程", "文具", "培训")),
        CategoryDef("其他", listOf("红包", "转账", "其他")),
    )

    private const val PREFS_NAME = "category_prefs"
    private const val KEY_CUSTOM = "custom_subcategories"

    fun getAll(context: Context): List<CategoryDef> {
        val custom = getCustom(context)
        return defaults.map { def ->
            val extras = custom[def.name] ?: emptyList()
            def.copy(subcategories = def.subcategories + extras)
        }
    }

    fun addSubcategory(context: Context, category: String, subcategory: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_CUSTOM, "{}") ?: "{}"
        val map = org.json.JSONObject(raw)
        val existing = map.optJSONArray(category)?.let { arr ->
            (0 until arr.length()).map { arr.getString(it) }
        } ?: emptyList()
        val updated = existing + subcategory
        map.put(category, org.json.JSONArray(updated))
        prefs.edit().putString(KEY_CUSTOM, map.toString()).apply()
    }

    private fun getCustom(context: Context): Map<String, List<String>> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_CUSTOM, "{}") ?: "{}"
        val map = org.json.JSONObject(raw)
        val result = mutableMapOf<String, List<String>>()
        map.keys().forEach { key ->
            val arr = map.optJSONArray(key)?.let { ja ->
                (0 until ja.length()).map { ja.getString(it) }
            } ?: emptyList()
            result[key] = arr
        }
        return result
    }
}
