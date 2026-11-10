package com.yummy.app.ui.home

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.yummy.app.R
import com.yummy.app.databinding.FragmentHomeBinding
import com.yummy.app.ui.components.ProductAdapter
import com.yummy.app.ui.main.MainNavigator
import com.yummy.app.ui.main.Tab
import kotlinx.coroutines.launch
import com.yummy.app.ui.inventory.ProductDetailDialog
class HomeFragment : Fragment(R.layout.fragment_home) {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private val productAdapter = ProductAdapter { openDetail(it.id) }
    private val recipeAdapter = RecipeCardAdapter()
    private val nav get() = requireActivity() as MainNavigator

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentHomeBinding.bind(view)
        applyInsets()
        setupLists()
        setupClicks()

        childFragmentManager.setFragmentResultListener(
            ProductDetailDialog.RESULT_KEY, viewLifecycleOwner
        ) { _, _ -> viewModel.refresh() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { render(it) }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.refresh() // se actualiza al volver de otras pantallas
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun applyInsets() {
        // El encabezado rojo llega hasta arriba, detrás de la barra de estado
        val basePadding = binding.header.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(binding.header) { v, insets ->
            val top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.updatePadding(top = basePadding + top)
            insets
        }
        ViewCompat.requestApplyInsets(binding.header)
    }

    private fun setupLists() = with(binding) {
        rvExpiring.adapter = productAdapter
        rvRecipes.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        rvRecipes.adapter = recipeAdapter
    }

    private fun setupClicks() = with(binding) {
        qaInventory.setOnClickListener { nav.navigateTo(Tab.INVENTORY) }
        qaShopping.setOnClickListener { nav.navigateTo(Tab.SHOPPING) }
        qaRecipes.setOnClickListener { nav.navigateTo(Tab.RECIPES) }
        qaAdd.setOnClickListener { nav.openAddMenu() }
        btnSeeAllExpiring.setOnClickListener { nav.navigateTo(Tab.INVENTORY) }
        btnSeeAllRecipes.setOnClickListener { nav.navigateTo(Tab.RECIPES) }
    }

    private fun openDetail(productId: String) {
        if (childFragmentManager.findFragmentByTag(ProductDetailDialog.TAG) != null) return
        ProductDetailDialog.newInstanceFor(productId)
            .show(childFragmentManager, ProductDetailDialog.TAG)
    }

    private fun render(s: HomeState) = with(binding) {
        txtName.text = s.name
        txtExpiring.text = s.summary.expiring.toString()
        txtInStock.text = s.summary.inStock.toString()
        txtExpired.text = s.summary.expired.toString()

        productAdapter.submitList(s.expiring)
        recipeAdapter.submitList(s.recipes)
        txtExpiringEmpty.isVisible = !s.loading && s.expiring.isEmpty()
        txtRecipesEmpty.isVisible = !s.loading && s.recipes.isEmpty()

        if (s.showError) {
            Toast.makeText(requireContext(), R.string.home_load_error, Toast.LENGTH_LONG).show()
            viewModel.errorShown()
        }
    }
}