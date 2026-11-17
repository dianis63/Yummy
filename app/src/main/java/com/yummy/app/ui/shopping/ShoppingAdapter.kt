package com.yummy.app.ui.shopping

import android.content.res.ColorStateList
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.yummy.app.R
import com.yummy.app.data.model.ShoppingItemRow
import com.yummy.app.databinding.ItemShoppingBoughtHeaderBinding
import com.yummy.app.databinding.ItemShoppingEntryBinding
import com.yummy.app.databinding.ItemShoppingSectionBinding
import com.yummy.app.ui.components.CategoryIcons
import com.yummy.app.ui.components.pretty

/** Id de la sección que recoge los ítems sin categoría conocida. */
const val OTHER_ID = "other"

sealed class ShoppingListItem {

    /** Cabecera de categoría. Con el nombre vacío se muestra "Otros". */
    data class CategoryHeader(
        val id: String,
        val name: String,
        val icon: String,
        val count: Int
    ) : ShoppingListItem()

    /** Cabecera de la sección de comprados. */
    data object BoughtHeader : ShoppingListItem()

    data class Entry(val item: ShoppingItemRow) : ShoppingListItem()
}

class ShoppingAdapter(
    private val onToggle: (ShoppingItemRow) -> Unit,
    private val onRemove: (ShoppingItemRow) -> Unit
) : ListAdapter<ShoppingListItem, RecyclerView.ViewHolder>(Diff) {

    class SectionVH(val b: ItemShoppingSectionBinding) : RecyclerView.ViewHolder(b.root)
    class BoughtVH(val b: ItemShoppingBoughtHeaderBinding) : RecyclerView.ViewHolder(b.root)
    class EntryVH(val b: ItemShoppingEntryBinding) : RecyclerView.ViewHolder(b.root)

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is ShoppingListItem.CategoryHeader -> TYPE_SECTION
        is ShoppingListItem.BoughtHeader -> TYPE_BOUGHT
        is ShoppingListItem.Entry -> TYPE_ENTRY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_SECTION -> SectionVH(ItemShoppingSectionBinding.inflate(inflater, parent, false))
            TYPE_BOUGHT -> BoughtVH(
                ItemShoppingBoughtHeaderBinding.inflate(inflater, parent, false)
            )
            else -> EntryVH(ItemShoppingEntryBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = getItem(position)) {
            is ShoppingListItem.CategoryHeader -> bindSection(holder as SectionVH, row)
            is ShoppingListItem.Entry -> bindEntry(holder as EntryVH, row.item)
            is ShoppingListItem.BoughtHeader -> Unit
        }
    }

    private fun bindSection(holder: SectionVH, row: ShoppingListItem.CategoryHeader) =
        with(holder.b) {
            val ctx = root.context
            imgIcon.setImageResource(CategoryIcons.forKey(row.icon))
            txtName.text =
                row.name.ifBlank { ctx.getString(R.string.shopping_category_other) }
            txtCount.text = row.count.toString()
        }

    private fun bindEntry(holder: EntryVH, item: ShoppingItemRow) = with(holder.b) {
        val ctx = root.context
        txtName.text = item.name
        txtSub.text = listOfNotNull(
            "${item.quantity.pretty()} ${item.unit}".trim(),
            if (item.isAuto) ctx.getString(R.string.shopping_auto) else null
        ).joinToString(" · ")

        if (item.isChecked) {
            btnCheck.setBackgroundResource(R.drawable.bg_circle)
            btnCheck.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.chip_green))
            btnCheck.setImageResource(R.drawable.ic_check)
            txtName.paintFlags = txtName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            root.alpha = 0.6f
        } else {
            btnCheck.setBackgroundResource(R.drawable.bg_circle_outline)
            btnCheck.backgroundTintList = null
            btnCheck.setImageDrawable(null)
            txtName.paintFlags = txtName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            root.alpha = 1f
        }

        btnCheck.setOnClickListener { onToggle(item) }
        btnRemove.setOnClickListener { onRemove(item) }
    }

    companion object {
        const val TYPE_SECTION = 0
        const val TYPE_BOUGHT = 1
        const val TYPE_ENTRY = 2
    }

    private object Diff : DiffUtil.ItemCallback<ShoppingListItem>() {
        override fun areItemsTheSame(a: ShoppingListItem, b: ShoppingListItem) = when {
            a is ShoppingListItem.CategoryHeader && b is ShoppingListItem.CategoryHeader ->
                a.id == b.id
            a is ShoppingListItem.BoughtHeader && b is ShoppingListItem.BoughtHeader -> true
            a is ShoppingListItem.Entry && b is ShoppingListItem.Entry -> a.item.id == b.item.id
            else -> false
        }

        override fun areContentsTheSame(a: ShoppingListItem, b: ShoppingListItem) = a == b
    }
}
