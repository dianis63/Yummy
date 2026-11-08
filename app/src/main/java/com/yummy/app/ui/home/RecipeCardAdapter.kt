package com.yummy.app.ui.home
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import coil3.request.crossfade
import com.yummy.app.R
import com.yummy.app.data.HomeRepository
import coil3.request.crossfade
import com.yummy.app.data.model.SuggestedRecipe
import com.yummy.app.databinding.ItemRecipeCardBinding

class RecipeCardAdapter : ListAdapter<SuggestedRecipe, RecipeCardAdapter.VH>(Diff) {

    class VH(val b: ItemRecipeCardBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemRecipeCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val r = getItem(position)
        with(holder.b) {
            txtTitle.text = r.title
            txtMatch.text = "${r.matchPercent}%"
            txtMeta.text = root.context.getString(
                R.string.recipe_meta, r.prepMinutes, r.available, r.total
            )
            if (r.imagePath != null) {
                imgRecipe.load(HomeRepository.recipeImageUrl(r.imagePath)) { crossfade(true) }
            } else {
                imgRecipe.setImageDrawable(null)
            }
        }
    }

    private object Diff : DiffUtil.ItemCallback<SuggestedRecipe>() {
        override fun areItemsTheSame(a: SuggestedRecipe, b: SuggestedRecipe) = a.recipeId == b.recipeId
        override fun areContentsTheSame(a: SuggestedRecipe, b: SuggestedRecipe) = a == b
    }
}