package com.yummy.app.ui.components


import androidx.annotation.DrawableRes
import com.yummy.app.R

/** Clave de icono de la BD -> drawable. Agrega líneas cuando importes más íconos. */
object CategoryIcons {
    @DrawableRes
    fun forKey(key: String?): Int = when (key) {
        "milk" -> R.drawable.ic_cat_milk
        "grain" -> R.drawable.ic_cat_grain
        "leaf" -> R.drawable.ic_cat_leaf
        "apple" -> R.drawable.ic_cat_apple
        "meat" -> R.drawable.ic_cat_meat
        "bread" -> R.drawable.ic_cat_bread
        "drink" -> R.drawable.ic_cat_drink
        "tag" -> R.drawable.ic_tag
        else -> R.drawable.ic_nav_inventory
    }
}