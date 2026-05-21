package com.yanaa.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

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
    private const val KEY_SUB = "custom_subcategories"
    private const val KEY_MAIN = "custom_main_categories"

    fun getAll(context: Context): List<CategoryDef> {
        val custom = getCustomSubs(context)
        val mains = getCustomMainCategories(context)
        val allMainNames = (defaults + mains).map { it.name }
        return allMainNames.map { name ->
            val def = defaults.find { it.name == name }
            val mainDef = mains.find { it.name == name }
            val extras = custom[name] ?: emptyList()
            val subs = (def?.subcategories ?: mainDef?.subcategories ?: emptyList()) + extras
            CategoryDef(name, subs)
        }
    }

    fun addSubcategory(context: Context, category: String, subcategory: String) {
        val map = loadMap(context, KEY_SUB)
        val existing = map.optJSONArray(category)?.let { arr ->
            (0 until arr.length()).map { arr.getString(it) }
        } ?: emptyList()
        val updated = existing + subcategory
        map.put(category, JSONArray(updated))
        saveMap(context, KEY_SUB, map)
    }

    fun addMainCategory(context: Context, name: String) {
        val map = loadMap(context, KEY_MAIN)
        map.put(name, JSONArray())
        saveMap(context, KEY_MAIN, map)
    }

    private fun getCustomMainCategories(context: Context): List<CategoryDef> {
        val map = loadMap(context, KEY_MAIN)
        val result = mutableListOf<CategoryDef>()
        map.keys().forEach { key ->
            val arr = map.optJSONArray(key)?.let { ja ->
                (0 until ja.length()).map { ja.getString(it) }
            } ?: emptyList()
            result.add(CategoryDef(key, arr))
        }
        return result
    }

    private fun getCustomSubs(context: Context): Map<String, List<String>> {
        val map = loadMap(context, KEY_SUB)
        val result = mutableMapOf<String, List<String>>()
        map.keys().forEach { key ->
            val arr = map.optJSONArray(key)?.let { ja ->
                (0 until ja.length()).map { ja.getString(it) }
            } ?: emptyList()
            result[key] = arr
        }
        return result
    }

    private fun loadMap(context: Context, key: String): JSONObject {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(key, "{}") ?: "{}"
        return JSONObject(raw)
    }

    private fun saveMap(context: Context, key: String, map: JSONObject) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(key, map.toString()).apply()
    }
}
