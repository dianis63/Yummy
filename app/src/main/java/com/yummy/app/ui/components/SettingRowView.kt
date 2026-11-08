package com.yummy.app.ui.components

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import androidx.core.view.isVisible
import com.yummy.app.R
import com.yummy.app.databinding.ViewSettingRowBinding

/** Fila de configuración: ícono + título + valor opcional a la derecha. */
class SettingRowView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val b = ViewSettingRowBinding.inflate(LayoutInflater.from(context), this)

    var value: CharSequence?
        get() = b.txtValue.text
        set(v) {
            b.txtValue.text = v
            b.txtValue.isVisible = !v.isNullOrEmpty()
        }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setBackgroundResource(R.drawable.bg_card)
        elevation = context.dp(2).toFloat()
        setPadding(context.dp(12), context.dp(12), context.dp(18), context.dp(12))
        isClickable = true
        isFocusable = true

        val primary = ContextCompat.getColor(context, R.color.primary)
        context.withStyledAttributes(attrs, R.styleable.SettingRowView) {
            b.imgIcon.setImageResource(getResourceId(R.styleable.SettingRowView_srIcon, 0))
            b.txtTitle.text = getString(R.styleable.SettingRowView_srTitle)
            b.imgIcon.imageTintList = ColorStateList.valueOf(primary)

            if (getBoolean(R.styleable.SettingRowView_srDanger, false)) {
                b.tile.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.ring_pink)
                )
                b.txtTitle.setTextColor(primary)
            }
        }
    }
}