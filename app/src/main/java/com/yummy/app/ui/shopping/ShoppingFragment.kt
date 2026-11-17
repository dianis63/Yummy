package com.yummy.app.ui.shopping

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.yummy.app.R
import com.yummy.app.databinding.FragmentShoppingBinding
import com.yummy.app.ui.components.applyStatusBarPadding
import com.yummy.app.ui.components.dp
import kotlinx.coroutines.launch

class ShoppingFragment : Fragment(R.layout.fragment_shopping) {

    private var _binding: FragmentShoppingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ShoppingViewModel by viewModels()

    private val adapter = ShoppingAdapter(
        onToggle = { viewModel.toggleChecked(it) },
        onRemove = { viewModel.remove(it) }
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentShoppingBinding.bind(view)
        binding.header.applyStatusBarPadding()
        applyFabInset()
        binding.rvItems.adapter = adapter
        setupClicks()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { render(it) }
            }
        }

        // Entrada desde el botón "Agregar Compra" del menú flotante
        if (savedInstanceState == null && arguments?.getBoolean(ARG_OPEN_ADD) == true) {
            arguments?.putBoolean(ARG_OPEN_ADD, false)
            openSheet()
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.refresh()
    }

    override fun onDestroyView() {
        super.onDestroyView()
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

    private fun setupClicks() = with(binding) {
        fabAdd.setOnClickListener { openSheet() }
        btnClear.setOnClickListener { showClearMenu() }
    }

    private fun openSheet() {
        if (childFragmentManager.findFragmentByTag(AddShoppingItemSheet.TAG) != null) return
        AddShoppingItemSheet().show(childFragmentManager, AddShoppingItemSheet.TAG)
    }

    private fun showClearMenu() {
        val s = viewModel.state.value
        if (s.items.isEmpty()) return
        val options = arrayOf(
            getString(R.string.shopping_clear_bought),
            getString(R.string.shopping_clear_all)
        )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.shopping_clear_title)
            .setItems(options) { _, which ->
                if (which == 0) clearBought() else confirmClearAll()
            }
            .show()
    }

    private fun clearBought() {
        if (!viewModel.state.value.hasChecked) {
            Toast.makeText(requireContext(), R.string.shopping_no_bought, Toast.LENGTH_SHORT).show()
            return
        }
        viewModel.clearChecked()
    }

    private fun confirmClearAll() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.shopping_clear_all)
            .setMessage(R.string.shopping_clear_all_confirm)
            .setPositiveButton(R.string.btn_delete) { _, _ -> viewModel.clearAll() }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun render(s: ShoppingState) = with(binding) {
        txtProgress.text = getString(R.string.shopping_progress, s.checked, s.total)
        progress.progress = if (s.total == 0) 0 else s.checked * 100 / s.total
        adapter.submitList(s.listItems)
        txtEmpty.isVisible = !s.loading && s.items.isEmpty()

        if (s.showLoadError) {
            Toast.makeText(requireContext(), R.string.shopping_load_error, Toast.LENGTH_LONG).show()
            viewModel.loadErrorShown()
        }
        if (s.showActionError) {
            Toast.makeText(requireContext(), R.string.shopping_save_error, Toast.LENGTH_LONG).show()
            viewModel.actionErrorShown()
        }
    }

    companion object {
        private const val ARG_OPEN_ADD = "open_add"

        /** Con openAdd la pantalla abre sola la hoja para agregar un producto. */
        fun newInstance(openAdd: Boolean) = ShoppingFragment().apply {
            arguments = Bundle().apply { putBoolean(ARG_OPEN_ADD, openAdd) }
        }
    }
}
