package com.yummy.app.ui.shopping

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.yummy.app.R
import com.yummy.app.data.model.StorageLocation
import com.yummy.app.databinding.ItemFilterChipBinding
import com.yummy.app.databinding.SheetBuyShoppingItemBinding
import com.yummy.app.ui.components.pretty
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Hoja que aparece al marcar un ítem como comprado. Nombre, categoría y unidad vienen
 * del ítem; aquí solo se confirma cantidad, ubicación y fecha de vencimiento.
 * Comparte el ViewModel con ShoppingFragment.
 */
class BuyItemSheet : BottomSheetDialogFragment(R.layout.sheet_buy_shopping_item) {

    private var _binding: SheetBuyShoppingItemBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ShoppingViewModel by viewModels({ requireParentFragment() })

    private val itemId: String get() = requireArguments().getString(ARG_ITEM_ID).orEmpty()

    private var locationId: String? = null
    private var expiryIso: String? = null
    private var locationsKey: Pair<List<StorageLocation>, String?>? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = SheetBuyShoppingItemBinding.bind(view)

        val item = viewModel.state.value.items.firstOrNull { it.id == itemId }
        if (item == null) { dismissAllowingStateLoss(); return }

        val categoryName = viewModel.state.value.categories
            .firstOrNull { it.id == item.categoryId }?.name
        binding.txtItemName.text = listOfNotNull(item.name, categoryName).joinToString(" · ")
        binding.txtUnit.text = item.unit
        if (savedInstanceState == null) {
            binding.etQty.setText(item.quantity.pretty())
            expiryIso = localIso(DEFAULT_DAYS)
        } else {
            locationId = savedInstanceState.getString(KEY_LOCATION)
            expiryIso = savedInstanceState.getString(KEY_EXPIRY)
        }
        binding.txtDate.text = displayDate(expiryIso)

        binding.btnCancel.setOnClickListener { dismiss() }
        binding.btnSave.setOnClickListener { save() }
        binding.dateField.setOnClickListener { showDatePicker() }
        binding.btnQtyUp.setOnClickListener { stepQuantity(1.0) }
        binding.btnQtyDown.setOnClickListener { stepQuantity(-1.0) }
        binding.etQty.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) { hideKeyboard(); true } else false
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
            d.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_LOCATION, locationId)
        outState.putString(KEY_EXPIRY, expiryIso)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        locationsKey = null
        _binding = null
    }

    private fun stepQuantity(delta: Double) = with(binding) {
        val current = etQty.text.toString().trim().replace(',', '.').toDoubleOrNull() ?: 0.0
        val next = Math.round((current + delta) * 100) / 100.0
        if (next > 0.0 && next <= MAX_QUANTITY) {
            etQty.setText(next.pretty())
            etQty.setSelection(etQty.text.length)
        }
    }

    private fun save() {
        val item = viewModel.state.value.items.firstOrNull { it.id == itemId } ?: return
        val qty = binding.etQty.text.toString().trim().replace(',', '.').toDoubleOrNull()
        if (qty == null || qty <= 0.0) {
            binding.etQty.error = getString(R.string.shopping_error_qty); return
        }
        val loc = locationId
        if (loc == null) {
            Toast.makeText(requireContext(), R.string.error_location, Toast.LENGTH_SHORT).show()
            return
        }
        val expiry = expiryIso
        if (expiry == null || expiry < localIso(0)) {
            Toast.makeText(requireContext(), R.string.error_expiry_date, Toast.LENGTH_SHORT).show()
            return
        }
        viewModel.buy(item, qty, loc, expiry)
    }

    private fun render(s: ShoppingState) = with(binding) {
        if (locationId == null && s.locations.isNotEmpty()) {
            locationId = (s.locations.firstOrNull { it.name.equals("Alacena", true) }
                ?: s.locations.first()).id
        }
        renderLocations(s.locations)

        btnSave.isEnabled = !s.buying
        btnSave.alpha = if (s.buying) 0.6f else 1f
        txtSave.setText(if (s.buying) R.string.btn_saving else R.string.shopping_buy_cta)

        if (s.bought) {
            viewModel.boughtHandled()
            Toast.makeText(requireContext(), R.string.shopping_bought_to_inventory, Toast.LENGTH_SHORT).show()
            dismiss()
            return@with
        }
        if (s.showBuyError) {
            Toast.makeText(requireContext(), R.string.shopping_save_error, Toast.LENGTH_LONG).show()
            viewModel.buyErrorShown()
        }
    }

    private fun renderLocations(locations: List<StorageLocation>) {
        val key = locations to locationId
        if (key == locationsKey) return
        locationsKey = key

        val container: LinearLayout = binding.locationsContainer
        container.removeAllViews()
        locations.forEach { l ->
            val chip = ItemFilterChipBinding.inflate(layoutInflater, container, false).root
            chip.text = l.name
            chip.setBackgroundResource(
                if (l.id == locationId) R.drawable.bg_chip_green else R.drawable.bg_chip_white
            )
            chip.setOnClickListener {
                locationId = l.id
                renderLocations(locations)
            }
            container.addView(chip as TextView)
        }
    }

    private fun showDatePicker() {
        hideKeyboard()
        val today = MaterialDatePicker.todayInUtcMilliseconds()
        val selected = (expiryIso?.let { isoToMillis(it) } ?: today)
            .coerceIn(today, today + MAX_FUTURE_MILLIS)
        val constraints = CalendarConstraints.Builder()
            .setStart(today)
            .setEnd(today + MAX_FUTURE_MILLIS)
            .setValidator(DateValidatorPointForward.from(today))
            .build()

        val picker = MaterialDatePicker.Builder.datePicker()
            .setTheme(R.style.ThemeOverlay_Yummy_DatePicker)
            .setTitleText(R.string.date_picker_title)
            .setSelection(selected)
            .setCalendarConstraints(constraints)
            .build()
        picker.addOnPositiveButtonClickListener { millis ->
            expiryIso = millisToIso(millis)
            binding.txtDate.text = displayDate(expiryIso)
        }
        picker.show(childFragmentManager, "buy_expiry_date")
    }

    private fun hideKeyboard() {
        val focused = dialog?.currentFocus ?: return
        requireContext().getSystemService(InputMethodManager::class.java)
            .hideSoftInputFromWindow(focused.windowToken, 0)
        focused.clearFocus()
    }

    private fun utcFormat(pattern: String) =
        SimpleDateFormat(pattern, Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }

    private fun isoToMillis(iso: String): Long? =
        try { utcFormat("yyyy-MM-dd").parse(iso)?.time } catch (e: Exception) { null }

    private fun millisToIso(millis: Long): String = utcFormat("yyyy-MM-dd").format(Date(millis))

    private fun displayDate(iso: String?): String {
        val millis = iso?.let { isoToMillis(it) } ?: return ""
        return utcFormat("d/M/yyyy").format(Date(millis))
    }

    private fun localIso(daysFromToday: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, daysFromToday)
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }

    companion object {
        const val TAG = "BuyItemSheet"
        private const val ARG_ITEM_ID = "item_id"
        private const val KEY_LOCATION = "location_id"
        private const val KEY_EXPIRY = "expiry"
        private const val DEFAULT_DAYS = 7
        private const val MAX_QUANTITY = 99999.0
        private const val MAX_FUTURE_MILLIS = 3650L * 24L * 60L * 60L * 1000L

        fun newInstance(itemId: String) = BuyItemSheet().apply {
            arguments = Bundle().apply { putString(ARG_ITEM_ID, itemId) }
        }
    }
}