package com.yummy.app.ui.recipes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import coil3.request.crossfade
import com.yummy.app.R
import com.yummy.app.data.HomeRepository
import com.yummy.app.data.model.RecipeSummary
import com.yummy.app.databinding.ItemRecipeFeaturedBinding
import com.yummy.app.databinding.ItemRecipeGridBinding

sealed class RecipeListItem {
    abstract val recipe: RecipeSummary

    /** Tarjeta grande de "Recomendada hoy" */
    data class Featured(override val recipe: RecipeSummary) : RecipeListItem()

    /** Tarjeta de la cuadrícula */
    data class Card(override val recipe: RecipeSummary) : RecipeListItem()
}

class RecipeAdapter(
    private val onClick: (RecipeSummary) -> Unit
) : ListAdapter<RecipeListItem, RecyclerView.ViewHolder>(Diff) {

    class FeaturedVH(val b: ItemRecipeFeaturedBinding) : RecyclerView.ViewHolder(b.root)
    class CardVH(val b: ItemRecipeGridBinding) : RecyclerView.ViewHolder(b.root)

    override fun getItemViewType(position: Int): Int =
        if (getItem(position) is RecipeListItem.Featured) TYPE_FEATURED else TYPE_CARD

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_FEATURED) {
            FeaturedVH(ItemRecipeFeaturedBinding.inflate(inflater, parent, false))
        } else {
            CardVH(ItemRecipeGridBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val recipe = getItem(position).recipe
        when (holder) {
            is FeaturedVH -> bindFeatured(holder.b, recipe)
            is CardVH -> bindCard(holder.b, recipe)
        }
    }

    private fun bindFeatured(b: ItemRecipeFeaturedBinding, r: RecipeSummary) = with(b) {
        val ctx = root.context
        imgRecipe.load(imageUrl(r)) { crossfade(true) }
        txtBadge.text = ctx.getString(R.string.recipe_featured, r.matchPercent)
        txtTitle.text = r.title
        txtMeta.text = ctx.getString(R.string.recipe_meta_long, r.prepMinutes, r.available, r.total)
        root.setOnClickListener { onClick(r) }
    }

    private fun bindCard(b: ItemRecipeGridBinding, r: RecipeSummary) = with(b) {
        val ctx = root.context
        imgRecipe.load(imageUrl(r)) { crossfade(true) }
        imgFavorite.isVisible = r.isFavorite
        txtMatch.text = "${r.matchPercent}%"
        txtTitle.text = r.title
        txtMeta.text = ctx.getString(R.string.recipe_meta, r.prepMinutes, r.available, r.total)
        root.setOnClickListener { onClick(r) }
    }

    private fun imageUrl(r: RecipeSummary): String? =
        r.imagePath?.let { HomeRepository.recipeImageUrl(it) }

    companion object {
        const val TYPE_FEATURED = 0
        const val TYPE_CARD = 1
    }

    private object Diff : DiffUtil.ItemCallback<RecipeListItem>() {
        override fun areItemsTheSame(a: RecipeListItem, b: RecipeListItem) =
            a::class == b::class && a.recipe.id == b.recipe.id

        override fun areContentsTheSame(a: RecipeListItem, b: RecipeListItem) = a == b
    }
}