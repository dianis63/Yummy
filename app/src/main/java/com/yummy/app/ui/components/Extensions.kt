package com.yummy.app.ui.components

import android.content.Context
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import android.text.InputType
import android.widget.EditText
import android.widget.ImageView
import androidx.core.content.res.ResourcesCompat
import com.yummy.app.R

fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

/** 1.0 -> "1", 0.5 -> "0.5" */
fun Double.pretty(): String = if (this % 1.0 == 0.0) toInt().toString() else toString()

fun View.applyStatusBarPadding() {
    val base = paddingTop
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        v.updatePadding(top = base + insets.getInsets(WindowInsetsCompat.Type.statusBars()).top)
        insets
    }
    ViewCompat.requestApplyInsets(this)
}

/** Conecta un ícono de ojo para mostrar u ocultar la contraseña de este campo. */
fun EditText.bindPasswordToggle(toggle: ImageView) {
    var visible = false
    toggle.setOnClickListener {
        visible = !visible
        inputType = InputType.TYPE_CLASS_TEXT or
                if (visible) InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                else InputType.TYPE_TEXT_VARIATION_PASSWORD
        // Al cambiar inputType se pierde la fuente, la volvemos a poner
        typeface = ResourcesCompat.getFont(context, R.font.segoe_ui)
        setSelection(text.length)
        toggle.setImageResource(if (visible) R.drawable.ic_eye_off else R.drawable.ic_eye)
    }
}