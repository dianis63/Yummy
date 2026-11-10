package com.yummy.app.ui.inventory

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.yummy.app.data.model.InventoryProduct
import com.yummy.app.databinding.ItemInventoryProductBinding
import com.yummy.app.ui.components.CategoryIcons
import com.yummy.app.ui.components.ExpiryUi
import com.yummy.app.ui.components.pretty

class InventoryAdapter(
    private val onClick: (InventoryProduct) -> Unit,
    private val onMinus: (InventoryProduct) -> Unit,
    private val onPlus: (InventoryProduct) -> Unit
) : ListAdapter<InventoryProduct, InventoryAdapter.VH>(Diff) {

    class VH(val b: ItemInventoryProductBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemInventoryProductBinding.inflate(LayoutInflater.from(parent.context), parent, false))

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

            root.setOnClickListener { onClick(p) }
            btnMinus.setOnClickListener { onMinus(p) }
            btnPlus.setOnClickListener { onPlus(p) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<InventoryProduct>() {
        override fun areItemsTheSame(a: InventoryProduct, b: InventoryProduct) = a.id == b.id
        override fun areContentsTheSame(a: InventoryProduct, b: InventoryProduct) = a == b
    }
}