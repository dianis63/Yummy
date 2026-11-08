package com.yummy.app.ui.components

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.yummy.app.R
import com.yummy.app.databinding.ItemBottomNavBinding
import com.yummy.app.ui.main.Tab

class YummyBottomNav @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    var onTabSelected: ((Tab) -> Unit)? = null
    private val items = mutableMapOf<Tab, ItemBottomNavBinding>()

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER
        setBackgroundResource(R.drawable.bg_pill)
        elevation = context.dp(8).toFloat()
        setPadding(context.dp(8), context.dp(8), context.dp(8), context.dp(8))

        Tab.entries.forEach { tab ->
            val item = ItemBottomNavBinding.inflate(LayoutInflater.from(context), this, false)
            item.imgIcon.setImageResource(tab.icon)
            item.txtLabel.setText(tab.label)
            item.root.setOnClickListener { onTabSelected?.invoke(tab) }
            addView(item.root)
            items[tab] = item
        }
        select(Tab.HOME)
    }

    fun select(selected: Tab) {
        items.forEach { (tab, item) ->
            val on = tab == selected
            item.root.setBackgroundResource(if (on) R.drawable.bg_button_primary else 0)
            item.txtLabel.isVisible = on
            item.imgIcon.imageTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, if (on) R.color.white else R.color.icon_gray)
            )
            (item.root.layoutParams as LayoutParams).weight = if (on) 2.4f else 1f
            item.root.requestLayout()
        }
    }
}