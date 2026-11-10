package com.yummy.app.ui.components


import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.yummy.app.data.model.ExpiringProduct
import com.yummy.app.databinding.ItemProductBinding

class ProductAdapter(
    private val onClick: ((ExpiringProduct) -> Unit)? = null
) : ListAdapter<ExpiringProduct, ProductAdapter.VH>(Diff) {
    class VH(val b: ItemProductBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemProductBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val p = getItem(position)
        val ctx = holder.itemView.context
        val ui = ExpiryUi.from(p.expiryStatus)
        val fg = ColorStateList.valueOf(ContextCompat.getColor(ctx, ui.fg))
        val bg = ColorStateList.valueOf(ContextCompat.getColor(ctx, ui.bg))

        with(holder.b) {
            txtName.text = p.name
            txtSub.text = listOfNotNull("${p.quantity.pretty()} ${p.unit}".trim(), p.locationName)
                .joinToString(" · ")
            txtDays.text = ExpiryUi.daysText(ctx, p.daysLeft)
            txtDays.setTextColor(fg)

            imgIcon.setImageResource(CategoryIcons.forKey(p.categoryIcon))
            imgIcon.backgroundTintList = bg
            imgIcon.imageTintList = fg

            chip.backgroundTintList = bg
            dot.backgroundTintList = fg
            txtChip.setText(ui.label)
            txtChip.setTextColor(fg)
            root.setOnClickListener { onClick?.invoke(p) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<ExpiringProduct>() {
        override fun areItemsTheSame(a: ExpiringProduct, b: ExpiringProduct) = a.id == b.id
        override fun areContentsTheSame(a: ExpiringProduct, b: ExpiringProduct) = a == b
    }
}