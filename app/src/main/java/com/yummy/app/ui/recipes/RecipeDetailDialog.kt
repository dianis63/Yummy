package com.yummy.app.ui.recipes

import android.content.DialogInterface
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil3.load
import coil3.request.crossfade
import com.yummy.app.R
import com.yummy.app.data.HomeRepository
import com.yummy.app.data.model.Difficulty
import com.yummy.app.data.model.IngredientAvailability
import com.yummy.app.data.model.MealType
import com.yummy.app.databinding.DialogRecipeDetailBinding
import com.yummy.app.databinding.ItemRecipeIngredientBinding
import com.yummy.app.databinding.ItemRecipeStepBinding
import com.yummy.app.ui.components.applyStatusBarPadding
import kotlinx.coroutines.launch

class RecipeDetailDialog : DialogFragment(R.layout.dialog_recipe_detail) {

    private var _binding: DialogRecipeDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RecipeDetailViewModel by viewModels()

    // Para no reconstruir las listas cuando solo cambia el corazón
    private var renderedIngredients: List<IngredientAvailability>? = null

    override fun getTheme(): Int = R.style.Theme_Yummy_FullScreenDialog

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = DialogRecipeDetailBinding.bind(view)
        viewModel.start(requireArguments().getString(ARG_ID)!!)

        binding.topBar.applyStatusBarPadding()
        applyBottomInset()
        binding.btnBack.setOnClickListener { dismiss() }
        binding.btnFavorite.setOnClickListener { viewModel.toggleFavorite() }

        binding.chipTime.imgIcon.setImageResource(R.drawable.ic_clock)
        binding.chipMeal.imgIcon.setImageResource(R.drawable.ic_nav_recipes)
        binding.chipDifficulty.imgIcon.setImageResource(R.drawable.ic_flame)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { render(it) }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = true
        }
        view?.let { ViewCompat.requestApplyInsets(it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        renderedIngredients = null
        _binding = null
    }

    override fun onDismiss(dialog: DialogInterface) {
        if (viewModel.state.value.changed) {
            parentFragmentManager.setFragmentResult(RESULT_KEY, Bundle())
        }
        super.onDismiss(dialog)
    }

    private fun applyBottomInset() {
        val base = binding.scroll.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.scroll) { v, insets ->
            val bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            v.updatePadding(bottom = base + bottom)
            insets
        }
        ViewCompat.requestApplyInsets(binding.scroll)
    }

    private fun render(s: RecipeDetailState) {
        val ctx = requireContext()

        if (s.showError) {
            Toast.makeText(ctx, R.string.recipe_detail_error, Toast.LENGTH_LONG).show()
            viewModel.errorShown()
            if (s.detail == null) {
                dismiss()
                return
            }
        }

        val d = s.detail
        binding.root.alpha = if (d == null) 0f else 1f
        if (d == null) return
        val r = d.recipe

        with(binding) {
            val imageUrl = r.imagePath?.let { HomeRepository.recipeImageUrl(it) }
            if (imgRecipe.tag != imageUrl) {
                imgRecipe.tag = imageUrl
                imgRecipe.load(imageUrl) { crossfade(true) }
            }

            txtMatch.text = getString(R.string.recipe_match_full, d.matchPercent)
            txtTitle.text = r.title
            chipTime.txtLabel.text = getString(R.string.recipe_minutes, r.prepMinutes)
            chipMeal.txtLabel.setText(MealType.from(r.mealType).label)
            chipDifficulty.txtLabel.setText(Difficulty.from(r.difficulty).label)

            btnFavorite.setImageResource(
                if (d.isFavorite) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
            )

            txtIngredientsCount.text =
                getString(R.string.recipe_ingredients_count, d.available, d.total)

            if (renderedIngredients !== d.ingredients) {
                renderedIngredients = d.ingredients
                renderIngredients(d.ingredients)
                renderSteps(r.instructions)
            }
        }
    }

    private fun renderIngredients(items: List<IngredientAvailability>) {
        val ctx = requireContext()
        val container = binding.ingredientsContainer
        container.removeAllViews()
        items.forEach { item ->
            val row = ItemRecipeIngredientBinding.inflate(layoutInflater, container, false)
            val fg = ContextCompat.getColor(
                ctx, if (item.available) R.color.status_fresh_fg else R.color.text_hint
            )
            val bg = ContextCompat.getColor(
                ctx, if (item.available) R.color.status_fresh_bg else R.color.tile_neutral
            )
            row.imgState.setImageResource(
                if (item.available) R.drawable.ic_check else R.drawable.ic_minus
            )
            row.imgState.backgroundTintList = ColorStateList.valueOf(bg)
            row.imgState.imageTintList = ColorStateList.valueOf(fg)
            row.txtName.text = item.name
            row.txtAmount.text = formatAmount(item.quantity, item.unit)
            container.addView(row.root)
        }
    }

    private fun renderSteps(instructions: String?) {
        val steps = instructions.orEmpty().lines().map { it.trim() }.filter { it.isNotEmpty() }
        binding.stepsSection.isVisible = steps.isNotEmpty()
        val container = binding.stepsContainer
        container.removeAllViews()
        steps.forEachIndexed { index, text ->
            val row = ItemRecipeStepBinding.inflate(LayoutInflater.from(requireContext()), container, false)
            row.txtNumber.text = (index + 1).toString()
            row.txtStep.text = text
            container.addView(row.root)
        }
    }

    companion object {
        const val TAG = "RecipeDetailDialog"
        const val RESULT_KEY = "recipe_changed"
        private const val ARG_ID = "recipe_id"

        fun newInstance(recipeId: String) = RecipeDetailDialog().apply {
            arguments = Bundle().apply { putString(ARG_ID, recipeId) }
        }
    }
}