package com.yummy.app.ui.categories

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.yummy.app.data.model.CategoryRow
import com.yummy.app.databinding.ItemCategoryBinding
import com.yummy.app.ui.components.CategoryIcons

class CategoryAdapter(
    private val onDelete: (CategoryRow) -> Unit
) : ListAdapter<CategoryRow, CategoryAdapter.VH>(Diff) {

    class VH(val b: ItemCategoryBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        with(holder.b) {
            imgIcon.setImageResource(CategoryIcons.forKey(item.icon))
            txtName.text = item.name
            btnDelete.setOnClickListener { onDelete(item) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<CategoryRow>() {
        override fun areItemsTheSame(a: CategoryRow, b: CategoryRow) = a.id == b.id
        override fun areContentsTheSame(a: CategoryRow, b: CategoryRow) = a == b
    }
}
