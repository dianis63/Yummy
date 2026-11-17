package com.yummy.app.ui.shopping

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.ListPopupWindow
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.yummy.app.R
import com.yummy.app.data.model.ProductCategory
import com.yummy.app.databinding.ItemFilterChipBinding
import com.yummy.app.databinding.SheetAddShoppingItemBinding
import kotlinx.coroutines.launch

/** Hoja para agregar un producto a la lista. Comparte el ViewModel con ShoppingFragment. */
class AddShoppingItemSheet : BottomSheetDialogFragment(R.layout.sheet_add_shopping_item) {

    private var _binding: SheetAddShoppingItemBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ShoppingViewModel by viewModels({ requireParentFragment() })

    private var categoryId: String? = null
    private var chipsKey: Pair<List<ProductCategory>, String?>? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = SheetAddShoppingItemBinding.bind(view)

        binding.etQty.setText(DEFAULT_QTY)
        binding.btnCancel.setOnClickListener { dismiss() }
        binding.btnSave.setOnClickListener { save() }
        binding.unitField.setOnClickListener { showUnitMenu() }
        binding.etQty.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) { save(); true } else false
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { render(it) }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        (dialog as? BottomSheetDialog)?.let { d ->
            d.behavior.state = BottomSheetBehavior.STATE_EXPANDED
            d.behavior.skipCollapsed = true
            d.window?.setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
                        WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        chipsKey = null
        _binding = null
    }

    private fun save() {
        val s = viewModel.state.value
        val name = binding.etName.text.toString().trim().replace(Regex("\\s+"), " ")
        if (name.isEmpty()) {
            binding.etName.error = getString(R.string.shopping_error_name)
            return
        }
        val qty = binding.etQty.text.toString().trim().replace(',', '.').toDoubleOrNull()
        if (qty == null || qty <= 0.0) {
            binding.etQty.error = getString(R.string.shopping_error_qty)
            return
        }
        val exists = s.items.any { !it.isChecked && it.name.equals(name, ignoreCase = true) }
        if (exists) {
            binding.etName.error = getString(R.string.shopping_error_duplicate)
            return
        }
        val unit = binding.txtUnit.text.toString().ifEmpty { DEFAULT_UNIT }
        viewModel.add(name, qty, unit, categoryId)
    }

    private fun showUnitMenu() {
        hideKeyboard()
        val units = viewModel.state.value.units
        if (units.isEmpty()) return
        ListPopupWindow(requireContext()).apply {
            anchorView = binding.unitField
            width = binding.unitField.width
            isModal = true
            setBackgroundDrawable(
                ContextCompat.getDrawable(requireContext(), R.drawable.bg_card)
            )
            setAdapter(ArrayAdapter(requireContext(), R.layout.item_dropdown, units))
            setOnItemClickListener { _, _, position, _ ->
                binding.txtUnit.text = units[position]
                dismiss()
            }
            show()
        }
    }

    private fun hideKeyboard() {
        val focused = dialog?.currentFocus ?: return
        requireContext().getSystemService(InputMethodManager::class.java)
            .hideSoftInputFromWindow(focused.windowToken, 0)
        focused.clearFocus()
    }

    private fun render(s: ShoppingState) = with(binding) {
        if (txtUnit.text.isNullOrBlank()) {
            txtUnit.text = s.units.firstOrNull() ?: DEFAULT_UNIT
        }
        renderChips(s.categories)

        btnSave.isEnabled = !s.saving
        btnSave.alpha = if (s.saving) 0.6f else 1f
        txtSave.setText(if (s.saving) R.string.btn_saving else R.string.shopping_add_cta)

        if (s.saved) {
            viewModel.savedHandled()
            Toast.makeText(requireContext(), R.string.shopping_saved, Toast.LENGTH_SHORT).show()
            dismiss()
            return@with
        }
        if (s.showSaveError) {
            Toast.makeText(requireContext(), R.string.shopping_save_error, Toast.LENGTH_LONG).show()
            viewModel.saveErrorShown()
        }
    }

    private fun renderChips(categories: List<ProductCategory>) {
        val key = categories to categoryId
        if (key == chipsKey) return
        chipsKey = key

        binding.chipsContainer.removeAllViews()
        categories.forEach { c ->
            addChip(c.name, categoryId == c.id) {
                categoryId = if (categoryId == c.id) null else c.id
                renderChips(categories)
            }
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

    companion object {
        const val TAG = "AddShoppingItemSheet"
        private const val DEFAULT_QTY = "1"
        private const val DEFAULT_UNIT = "pzs"
    }
}
