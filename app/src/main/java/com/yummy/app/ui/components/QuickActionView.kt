package com.yummy.app.ui.components

import android.content.Context
import android.content.res.ColorStateList
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import com.yummy.app.R
import com.yummy.app.databinding.ViewQuickActionBinding

class QuickActionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        val b = ViewQuickActionBinding.inflate(LayoutInflater.from(context), this)
        val primary = ContextCompat.getColor(context, R.color.primary)

        context.withStyledAttributes(attrs, R.styleable.QuickActionView) {
            b.imgIcon.setImageResource(getResourceId(R.styleable.QuickActionView_qaIcon, 0))
            b.txtLabel.text = getString(R.styleable.QuickActionView_qaLabel)

            if (getBoolean(R.styleable.QuickActionView_qaAccent, false)) {
                b.tile.backgroundTintList = ColorStateList.valueOf(primary)
                b.imgIcon.imageTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.white)
                )
                if (Build.VERSION.SDK_INT >= 28) {
                    b.tile.outlineSpotShadowColor = primary
                    b.tile.outlineAmbientShadowColor = primary
                }
            } else {
                b.imgIcon.imageTintList = ColorStateList.valueOf(primary)
            }
        }
    }
}