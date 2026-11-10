package com.yummy.app.ui.inventory

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.yummy.app.R
import com.yummy.app.data.model.InventoryFilter
import com.yummy.app.data.model.InventoryProduct
import com.yummy.app.data.model.ProductCategory
import com.yummy.app.databinding.FragmentInventoryBinding
import com.yummy.app.databinding.ItemFilterChipBinding
import com.yummy.app.ui.addproduct.AddProductActivity
import com.yummy.app.ui.components.applyStatusBarPadding
import com.yummy.app.ui.components.dp
import kotlinx.coroutines.launch

class InventoryFragment : Fragment(R.layout.fragment_inventory) {

    private var _binding: FragmentInventoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InventoryViewModel by viewModels()

    private val adapter = InventoryAdapter(
        onClick = { openDetail(it) },
        onMinus = { minus(it) },
        onPlus = { viewModel.changeQuantity(it.id, 1.0) }
    )

    private var chipsKey: Triple<List<ProductCategory>, InventoryFilter, String?>? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentInventoryBinding.bind(view)
        binding.header.applyStatusBarPadding()
        applyFabInset()
        binding.rvProducts.adapter = adapter
        setupSearch()
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
        viewModel.refresh()
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

    private fun setupSearch() = with(binding) {
        etSearch.doAfterTextChanged { viewModel.onSearch(it?.toString().orEmpty()) }
        etSearch.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                v.clearFocus()
                val imm = requireContext().getSystemService(InputMethodManager::class.java)
                imm.hideSoftInputFromWindow(v.windowToken, 0)
                true
            } else {
                false
            }
        }
    }

    private fun setupClicks() = with(binding) {
        fabAdd.setOnClickListener {
            startActivity(AddProductActivity.newIntent(requireContext()))
        }
    }

    private fun openDetail(product: InventoryProduct) {
        if (childFragmentManager.findFragmentByTag(ProductDetailDialog.TAG) != null) return
        ProductDetailDialog.newInstance(product)
            .show(childFragmentManager, ProductDetailDialog.TAG)
    }

    private fun minus(p: InventoryProduct) {
        if (p.quantity >= 2.0) {
            viewModel.changeQuantity(p.id, -1.0)
            return
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.consume_confirm_title)
            .setMessage(getString(R.string.consume_confirm_message, p.name))
            .setPositiveButton(R.string.consume_confirm_action) { _, _ ->
                viewModel.changeQuantity(p.id, -p.quantity)
                Toast.makeText(requireContext(), R.string.product_consumed, Toast.LENGTH_SHORT)
                    .show()
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun render(s: InventoryState) = with(binding) {
        txtCount.text = resources.getQuantityString(R.plurals.inventory_count, s.total, s.total)
        adapter.submitList(s.products)
        renderChips(s)

        val noFilters = s.search.isBlank() &&
                s.filter == InventoryFilter.ALL && s.categoryId == null
        txtEmpty.isVisible = !s.loading && s.products.isEmpty()
        txtEmpty.setText(
            if (noFilters) R.string.inventory_empty else R.string.inventory_empty_filtered
        )

        if (s.showError) {
            Toast.makeText(requireContext(), R.string.inventory_load_error, Toast.LENGTH_LONG).show()
            viewModel.errorShown()
        }
    }

    private fun renderChips(s: InventoryState) {
        val key = Triple(s.categories, s.filter, s.categoryId)
        if (key == chipsKey) return
        chipsKey = key

        binding.chipsContainer.removeAllViews()
        addChip(
            getString(R.string.inventory_filter_all),
            s.filter == InventoryFilter.ALL && s.categoryId == null
        ) { viewModel.selectStatus(InventoryFilter.ALL) }
        addChip(
            getString(R.string.inventory_filter_expiring),
            s.filter == InventoryFilter.EXPIRING
        ) { viewModel.selectStatus(InventoryFilter.EXPIRING) }
        addChip(
            getString(R.string.inventory_filter_expired),
            s.filter == InventoryFilter.EXPIRED
        ) { viewModel.selectStatus(InventoryFilter.EXPIRED) }
        addChip(
            getString(R.string.inventory_filter_favorites),
            s.filter == InventoryFilter.FAVORITES
        ) { viewModel.selectStatus(InventoryFilter.FAVORITES) }
        s.categories.forEach { c ->
            addChip(c.name, s.categoryId == c.id) { viewModel.selectCategory(c.id) }
        }
    }

    private fun addChip(label: String, selected: Boolean, onClick: () -> Unit) {
        val chip = ItemFilterChipBinding.inflate(layoutInflater, binding.chipsContainer, false).root
        chip.text = label
        chip.setBackgroundResource(
            if (selected) R.drawable.bg_button_primary else R.drawable.bg_chip_white
        )
        chip.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (selected) R.color.white else R.color.text_primary
            )
        )
        chip.setOnClickListener { onClick() }
        binding.chipsContainer.addView(chip)
    }
}