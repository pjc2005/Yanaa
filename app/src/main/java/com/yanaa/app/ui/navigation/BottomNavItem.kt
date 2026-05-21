package com.yanaa.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Home : BottomNavItem("home", "首页", Icons.Default.Home)
    data object Records : BottomNavItem("records", "记录", Icons.Default.List)
    data object Stats : BottomNavItem("stats", "统计", Icons.Default.BarChart)
    data object Profile : BottomNavItem("profile", "我的", Icons.Default.Person)

    companion object {
        val items = listOf(Home, Records, Stats, Profile)
    }
}
