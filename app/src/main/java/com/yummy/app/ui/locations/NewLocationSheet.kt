package com.yummy.app.ui.locations

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.yummy.app.R
import com.yummy.app.databinding.SheetNewLocationBinding
import kotlinx.coroutines.launch

class NewLocationSheet : BottomSheetDialogFragment(R.layout.sheet_new_location) {

    private var _binding: SheetNewLocationBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LocationsViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = SheetNewLocationBinding.bind(view)

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
            d.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                ?.setBackgroundResource(R.drawable.bg_sheet_top)
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
            binding.etName.error = getString(R.string.location_error_name); return
        }
        val exists = viewModel.state.value.items.any { it.name.equals(name, ignoreCase = true) }
        if (exists) {
            binding.etName.error = getString(R.string.location_error_duplicate); return
        }
        viewModel.add(name)
    }

    private fun render(s: LocationsState) = with(binding) {
        btnSave.isEnabled = !s.saving
        btnSave.alpha = if (s.saving) 0.6f else 1f
        txtSave.setText(if (s.saving) R.string.btn_saving else R.string.location_save)

        if (s.saved) {
            viewModel.savedHandled()
            Toast.makeText(requireContext(), R.string.location_saved, Toast.LENGTH_SHORT).show()
            dismiss()
            return@with
        }
        if (s.showSaveError) {
            Toast.makeText(requireContext(), R.string.location_save_error, Toast.LENGTH_LONG).show()
            viewModel.saveErrorShown()
        }
    }

    companion object {
        const val TAG = "NewLocationSheet"
    }
}