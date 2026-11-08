package com.yummy.app.ui.components


import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.view.updatePadding
import com.yummy.app.databinding.ItemSpeedDialActionBinding
import com.yummy.app.databinding.ViewSpeedDialBinding

data class SpeedDialAction(
    val id: Int,
    @StringRes val label: Int,
    @DrawableRes val icon: Int
)

class SpeedDialView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val binding = ViewSpeedDialBinding.inflate(LayoutInflater.from(context), this)
    private val itemViews = mutableListOf<View>()

    var onActionClick: ((SpeedDialAction) -> Unit)? = null
    var isOpen = false
        private set

    init {
        binding.fab.setOnClickListener { if (isOpen) close() else open() }
        binding.scrim.setOnClickListener { close() }
    }

    fun setActions(actions: List<SpeedDialAction>) {
        binding.actionsColumn.removeAllViews()
        itemViews.clear()
        actions.forEach { action ->
            val item = ItemSpeedDialActionBinding.inflate(
                LayoutInflater.from(context), binding.actionsColumn, false
            )
            item.txtLabel.setText(action.label)
            item.imgIcon.setImageResource(action.icon)
            item.root.setOnClickListener {
                close()
                onActionClick?.invoke(action)
            }
            binding.actionsColumn.addView(item.root)
            itemViews.add(item.root)
        }
    }

    /** Espacio inferior para que el botón quede sobre la barra de navegación. */
    fun setBottomInset(px: Int) = binding.content.updatePadding(bottom = px)

    fun setFabVisible(show: Boolean) {
        if (!show) close()
        binding.fab.visibility = if (show) VISIBLE else GONE
    }

    fun open() {
        if (isOpen) return
        isOpen = true
        binding.scrim.apply {
            visibility = VISIBLE
            alpha = 0f
            animate().alpha(1f).setDuration(180).start()
        }
        binding.fab.animate().rotation(45f).setDuration(180).start()
        itemViews.forEachIndexed { i, v ->
            v.visibility = VISIBLE
            v.alpha = 0f
            v.translationY = 40f
            v.animate().alpha(1f).translationY(0f)
                .setStartDelay((itemViews.size - 1 - i) * 40L).setDuration(160).start()
        }
    }

    fun close() {
        if (!isOpen) return
        isOpen = false
        binding.fab.animate().rotation(0f).setDuration(180).start()
        binding.scrim.animate().alpha(0f).setDuration(150)
            .withEndAction { if (!isOpen) binding.scrim.visibility = GONE }.start()
        itemViews.forEach { v ->
            v.animate().alpha(0f).setStartDelay(0).setDuration(120)
                .withEndAction { if (!isOpen) v.visibility = GONE }.start()
        }
    }
}