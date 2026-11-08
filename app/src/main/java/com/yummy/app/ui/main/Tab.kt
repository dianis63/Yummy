package com.yummy.app.ui.main

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.yummy.app.R

enum class Tab(
    @DrawableRes val icon: Int,
    @StringRes val label: Int,
    val showFab: Boolean
) {
    HOME(R.drawable.ic_nav_home, R.string.nav_home, true),
    INVENTORY(R.drawable.ic_nav_inventory, R.string.nav_inventory, false),
    SHOPPING(R.drawable.ic_nav_cart, R.string.nav_shopping, false),
    RECIPES(R.drawable.ic_nav_recipes, R.string.nav_recipes, false),
    PROFILE(R.drawable.ic_person, R.string.nav_profile, false)
}