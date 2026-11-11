package com.yummy.app.ui.recipes

import com.yummy.app.data.model.RecipeUnits
import com.yummy.app.ui.components.pretty
import kotlin.math.abs

/** 2 pzs -> "2 pzs", 1 pzs -> "1 pza", 0.5 pzs -> "1/2 pza", 1 cdas -> "1 cda", "al gusto" -> "al gusto" */
fun formatAmount(quantity: Double, unit: String): String {
    if (unit == RecipeUnits.TO_TASTE) return unit
    val single = quantity <= 1.0
    val shownUnit = when (unit) {
        "pzs" -> if (single) "pza" else "pzs"
        "cdas" -> if (single) "cda" else "cdas"
        "cdtas" -> if (single) "cdta" else "cdtas"
        "tazas" -> if (single) "taza" else "tazas"
        else -> unit
    }
    return "${formatQuantity(quantity)} $shownUnit".trim()
}

private fun formatQuantity(q: Double): String {
    val whole = q.toInt()
    val frac = q - whole
    val fraction = when {
        abs(frac - 0.5) < 0.01 -> "1/2"
        abs(frac - 0.25) < 0.01 -> "1/4"
        abs(frac - 0.75) < 0.01 -> "3/4"
        abs(frac - 1.0 / 3) < 0.01 -> "1/3"
        abs(frac - 2.0 / 3) < 0.01 -> "2/3"
        else -> null
    }
    return when {
        fraction == null -> q.pretty()
        whole == 0 -> fraction
        else -> "$whole $fraction"
    }
}