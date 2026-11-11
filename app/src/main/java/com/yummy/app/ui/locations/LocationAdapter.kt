package com.yummy.app.ui.locations

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.yummy.app.R
import com.yummy.app.data.model.LocationRow
import com.yummy.app.databinding.ItemLocationBinding

class LocationAdapter : ListAdapter<LocationRow, LocationAdapter.VH>(Diff) {

    class VH(val b: ItemLocationBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemLocationBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        with(holder.b) {
            txtName.text = item.name
            txtBadge.setText(
                if (item.isDefault) R.string.location_badge_default else R.string.location_badge_custom
            )
        }
    }

    private object Diff : DiffUtil.ItemCallback<LocationRow>() {
        override fun areItemsTheSame(a: LocationRow, b: LocationRow) = a.id == b.id
        override fun areContentsTheSame(a: LocationRow, b: LocationRow) = a == b
    }
}