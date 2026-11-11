package com.yummy.app.ui.recipes

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.yummy.app.R
import com.yummy.app.databinding.FragmentRecipesBinding
import com.yummy.app.databinding.ItemFilterChipBinding
import com.yummy.app.ui.addrecipe.AddRecipeActivity
import com.yummy.app.ui.components.applyStatusBarPadding
import com.yummy.app.ui.components.dp
import kotlinx.coroutines.launch

class RecipesFragment : Fragment(R.layout.fragment_recipes) {

    private var _binding: FragmentRecipesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RecipesViewModel by viewModels()
    private val adapter = RecipeAdapter { openDetail(it.id) }

    private var chipsKey: RecipeFilter? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentRecipesBinding.bind(view)
        binding.header.applyStatusBarPadding()
        applyFabInset()
        setupList()

        binding.fabAdd.setOnClickListener {
            startActivity(AddRecipeActivity.newIntent(requireContext()))
        }

        childFragmentManager.setFragmentResultListener(
            RecipeDetailDialog.RESULT_KEY, viewLifecycleOwner
        ) { _, _ -> viewModel.refresh() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { render(it) }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.refresh() // se actualiza al volver de crear una receta
    }

    override fun onDestroyView() {
        super.onDestroyView()
        chipsKey = null
        _binding = null
    }

    private fun applyFabInset() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.fabAdd) { v, insets ->
            val bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            v.updateLayoutParams<FrameLayout.LayoutParams> {
                bottomMargin = bottom + requireContext().dp(96)
            }
            insets
        }
        ViewCompat.requestApplyInsets(binding.fabAdd)
    }

    private fun setupList() {
        val layoutManager = GridLayoutManager(requireContext(), 2)
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            // La tarjeta destacada ocupa las dos columnas
            override fun getSpanSize(position: Int): Int =
                if (adapter.getItemViewType(position) == RecipeAdapter.TYPE_FEATURED) 2 else 1
        }
        binding.rvRecipes.layoutManager = layoutManager
        binding.rvRecipes.adapter = adapter
    }

    private fun openDetail(recipeId: String) {
        if (childFragmentManager.findFragmentByTag(RecipeDetailDialog.TAG) != null) return
        RecipeDetailDialog.newInstance(recipeId)
            .show(childFragmentManager, RecipeDetailDialog.TAG)
    }

    private fun render(s: RecipesState) = with(binding) {
        txtCount.text =
            if (s.loading && s.all.isEmpty()) "" else getString(R.string.recipes_count, s.all.size)
        renderChips(s)

        val visible = s.visible
        val featured =
            if (s.filter == RecipeFilter.ALL) visible.firstOrNull()?.takeIf { it.matchPercent > 0 }
            else null
        val items = buildList {
            if (featured != null) add(RecipeListItem.Featured(featured))
            visible.forEach { add(RecipeListItem.Card(it)) }
        }
        adapter.submitList(items)

        txtEmpty.isVisible = !s.loading && visible.isEmpty()
        txtEmpty.setText(
            if (s.filter == RecipeFilter.ALL) R.string.recipes_empty else R.string.recipes_empty_filtered
        )

        if (s.showError) {
            Toast.makeText(requireContext(), R.string.recipes_load_error, Toast.LENGTH_LONG).show()
            viewModel.errorShown()
        }
    }

    private fun renderChips(s: RecipesState) {
        if (s.filter == chipsKey) return
        chipsKey = s.filter

        binding.chipsContainer.removeAllViews()
        RecipeFilter.entries.forEach { filter ->
            val selected = filter == s.filter
            val chip = ItemFilterChipBinding.inflate(layoutInflater, binding.chipsContainer, false).root
            chip.setText(filter.label)
            chip.setBackgroundResource(
                if (selected) R.drawable.bg_button_primary else R.drawable.bg_chip_white
            )
            chip.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (selected) R.color.white else R.color.text_primary
                )
            )
            chip.setOnClickListener { viewModel.selectFilter(filter) }
            binding.chipsContainer.addView(chip)
        }
    }
}