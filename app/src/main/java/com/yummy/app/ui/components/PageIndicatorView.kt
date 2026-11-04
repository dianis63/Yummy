package com.yummy.app.ui.components


import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.yummy.app.R

class PageIndicatorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val density = resources.displayMetrics.density
    private val dotSize = 8 * density
    private val activeWidth = 26 * density
    private val gap = 8 * density

    private val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.primary)
    }
    private val inactivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.dot_inactive)
    }
    private val rect = RectF()

    var count: Int = 0
        set(value) {
            field = value
            requestLayout()
            invalidate()
        }

    var selectedIndex: Int = 0
        set(value) {
            field = value
            invalidate()
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val total = if (count == 0) 0f
        else activeWidth + (count - 1) * (dotSize + gap)
        setMeasuredDimension(
            resolveSize(total.toInt(), widthMeasureSpec),
            resolveSize(dotSize.toInt(), heightMeasureSpec)
        )
    }

    override fun onDraw(canvas: Canvas) {
        var x = 0f
        for (i in 0 until count) {
            val isActive = i == selectedIndex
            val w = if (isActive) activeWidth else dotSize
            rect.set(x, 0f, x + w, dotSize)
            canvas.drawRoundRect(rect, dotSize / 2, dotSize / 2, if (isActive) activePaint else inactivePaint)
            x += w + gap
        }
    }
}