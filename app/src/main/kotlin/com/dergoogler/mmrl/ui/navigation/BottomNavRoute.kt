package com.dergoogler.mmrl.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.dergoogler.mmrl.R

enum class BottomNavRoute(
    val route: String,
    @StringRes val label: Int,
    @DrawableRes val icon: Int,
    @DrawableRes val iconFilled: Int,
) {
    Home(
        route = "HomeScreen",
        label = R.string.page_home,
        icon = R.drawable.home,
        iconFilled = R.drawable.home_filled
    ),

    Repository(
        route = "RepositoriesScreen",
        label = R.string.page_repositorys,
        icon = R.drawable.cloud,
        iconFilled = R.drawable.cloud_filled
    ),

    Modules(
        route = "ModulesScreen",
        label = R.string.page_modules,
        icon = R.drawable.keyframes,
        iconFilled = R.drawable.keyframes_filled
    ),

    Search(
        route = "SearchScreen",
        label = R.string.page_search,
        icon = R.drawable.search,
        iconFilled = R.drawable.search_filled
    ),
}