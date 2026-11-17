package com.yummy.app.ui.categories

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.yummy.app.R
import com.yummy.app.databinding.ItemIconOptionBinding
import com.yummy.app.databinding.SheetNewCategoryBinding
import com.yummy.app.ui.components.CategoryIcons
import kotlinx.coroutines.launch

class NewCategorySheet : BottomSheetDialogFragment(R.layout.sheet_new_category) {

    private var _binding: SheetNewCategoryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CategoriesViewModel by activityViewModels()

    private var icon = ICONS.first()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = SheetNewCategoryBinding.bind(view)

        renderIcons()
        binding.btnCancel.setOnClickListener { dismiss() }
        binding.btnSave.setOnClickListener { save() }
        binding.etName.setOnEditorActionListener { _, actionId, _ ->
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
        _binding = null
    }

    private fun save() {
        val name = binding.etName.text.toString().trim().replace(Regex("\\s+"), " ")
        if (name.isEmpty()) {
            binding.etName.error = getString(R.string.category_error_name); return
        }
        val exists = viewModel.state.value.items.any { it.name.equals(name, ignoreCase = true) }
        if (exists) {
            binding.etName.error = getString(R.string.category_error_duplicate); return
        }
        viewModel.add(name, icon)
    }

    private fun renderIcons() {
        binding.iconsContainer.removeAllViews()
        ICONS.forEach { key -> binding.iconsContainer.addView(iconTile(key)) }
    }

    private fun iconTile(key: String): ImageView {
        val tile = ItemIconOptionBinding
            .inflate(layoutInflater, binding.iconsContainer, false).root
        val selected = key == icon
        tile.setImageResource(CategoryIcons.forKey(key))
        tile.backgroundTintList = color(if (selected) R.color.primary else R.color.white)
        tile.imageTintList = color(if (selected) R.color.white else R.color.icon_gray)
        tile.setOnClickListener {
            icon = key
            renderIcons()
        }
        return tile
    }

    private fun color(@ColorRes resId: Int) =
        ColorStateList.valueOf(ContextCompat.getColor(requireContext(), resId))

    private fun render(s: CategoriesState) = with(binding) {
        btnSave.isEnabled = !s.saving
        btnSave.alpha = if (s.saving) 0.6f else 1f
        txtSave.setText(if (s.saving) R.string.btn_saving else R.string.category_save)

        if (s.saved) {
            viewModel.savedHandled()
            Toast.makeText(requireContext(), R.string.category_saved, Toast.LENGTH_SHORT).show()
            dismiss()
            return@with
        }
        if (s.showSaveError) {
            Toast.makeText(requireContext(), R.string.category_save_error, Toast.LENGTH_LONG).show()
            viewModel.saveErrorShown()
        }
    }

    companion object {
        const val TAG = "NewCategorySheet"

        /** Orden en que se ofrecen los íconos. Las claves son las de CategoryIcons. */
        private val ICONS = listOf(
            "tag", "bag", "apple", "leaf", "milk", "grain", "meat", "bread", "drink", "seafood"
        )
    }
}
