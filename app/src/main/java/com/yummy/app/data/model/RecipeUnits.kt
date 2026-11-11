package com.yummy.app.data.model

/** Unidades que se ofrecen al crear los ingredientes de una receta. */
object RecipeUnits {
    const val DEFAULT = "pzs"
    const val TO_TASTE = "al gusto"

    val all = listOf("pzs", "g", "kg", "ml", "L", "cdas", "cdtas", "tazas", TO_TASTE)

    /** Cuánto suben o bajan los botones − y + según la unidad. */
    fun step(unit: String): Double = when (unit) {
        "g", "ml" -> 50.0
        else -> 0.5
    }
}