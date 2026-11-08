package com.yummy.app.ui.components


import androidx.annotation.DrawableRes
import com.yummy.app.R

/** Clave de icono de la BD -> drawable. Agrega líneas cuando importes más íconos. */
object CategoryIcons {
    @DrawableRes
    fun forKey(key: String?): Int = when (key) {
        // "bread" -> R.drawable.ic_cat_bread
        // "milk"  -> R.drawable.ic_cat_milk
        else -> R.drawable.ic_nav_inventory
    }
}