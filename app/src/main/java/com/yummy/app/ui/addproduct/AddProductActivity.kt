package com.yummy.app.ui.addproduct

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ListPopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.yummy.app.R
import com.yummy.app.data.model.InventoryProduct
import com.yummy.app.data.model.ProductInput
import com.yummy.app.databinding.ActivityAddProductBinding
import com.yummy.app.databinding.ItemFilterChipBinding
import com.yummy.app.ui.components.applyStatusBarPadding
import com.yummy.app.ui.components.pretty
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class AddProductActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddProductBinding
    private val viewModel: AddProductViewModel by viewModels()

    private var editing: InventoryProduct? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
        binding = ActivityAddProductBinding.inflate(layoutInflater)
        setContentView(binding.root)

        editing = intent.getStringExtra(EXTRA_PRODUCT)
            ?.let { Json.decodeFromString(InventoryProduct.serializer(), it) }

        binding.header.applyStatusBarPadding()
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            )
            v.updatePadding(bottom = bars.bottom)
            insets
        }

        setupTexts()
        if (savedInstanceState == null) fillForm(editing)
        setupClicks()
        viewModel.start(editing, localIso(DEFAULT_DAYS))

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { render(it) }
            }
        }
    }

    private fun setupTexts() = with(binding) {
        val isEdit = editing != null
        txtTitle.setText(if (isEdit) R.string.edit_product_title else R.string.add_product_title)
        txtSubtitle.setText(
            if (isEdit) R.string.edit_product_subtitle else R.string.add_product_subtitle
        )
        txtSave.setText(if (isEdit) R.string.btn_save_changes else R.string.btn_save_product)
    }

    private fun fillForm(p: InventoryProduct?) = with(binding) {
        if (p == null) {
            etQuantity.setText("1")
            txtUnit.text = "pzs"
        } else {
            etName.setText(p.name)
            etQuantity.setText(p.quantity.pretty())
            txtUnit.text = p.unit.ifEmpty { "pzs" }
        }
    }

    private fun setupClicks() = with(binding) {
        btnBack.setOnClickListener { finish() }
        btnSave.setOnClickListener { doSave() }
        unitField.setOnClickListener { showUnitMenu() }
        dateField.setOnClickListener { showDatePicker() }
        btnQtyUp.setOnClickListener { stepQuantity(1.0) }
        btnQtyDown.setOnClickListener { stepQuantity(-1.0) }
    }

    private fun stepQuantity(delta: Double) = with(binding) {
        val current = etQuantity.text.toString().trim().replace(',', '.').toDoubleOrNull() ?: 0.0
        val next = Math.round((current + delta) * 100) / 100.0
        if (next >= 1.0 && next <= MAX_QUANTITY) {
            etQuantity.setText(next.pretty())
            etQuantity.setSelection(etQuantity.text.length)
        }
    }

    private fun doSave() {
        val name = binding.etName.text.toString().trim()
        val quantity = binding.etQuantity.text.toString().trim().replace(',', '.').toDoubleOrNull()
        val unit = binding.txtUnit.text.toString().trim().ifEmpty { "pzs" }
        val s = viewModel.state.value
        val expiry = s.expiryDate

        if (name.isEmpty()) {
            binding.etName.error = getString(R.string.error_name_required); return
        }
        if (quantity == null || quantity < 1.0 || quantity > MAX_QUANTITY) {
            binding.etQuantity.error = getString(R.string.error_quantity); return
        }
        if (s.categoryId == null) {
            Toast.makeText(this, R.string.error_category, Toast.LENGTH_SHORT).show(); return
        }
        if (s.locationId == null) {
            Toast.makeText(this, R.string.error_location, Toast.LENGTH_SHORT).show(); return
        }
        if (expiry == null || (expiry < localIso(0) && expiry != editing?.expiryDate)) {
            Toast.makeText(this, R.string.error_expiry_date, Toast.LENGTH_SHORT).show(); return
        }

        viewModel.save(
            editing?.id,
            ProductInput(
                name = name,
                quantity = quantity,
                unit = unit,
                categoryId = s.categoryId,
                locationId = s.locationId,
                expiryDate = expiry
            )
        )
    }

    private fun showUnitMenu() = with(binding) {
        currentFocus?.let {
            getSystemService(InputMethodManager::class.java)
                .hideSoftInputFromWindow(it.windowToken, 0)
            it.clearFocus()
        }
        ListPopupWindow(this@AddProductActivity).apply {
            anchorView = unitField
            width = unitField.width
            isModal = true
            setBackgroundDrawable(
                ContextCompat.getDrawable(this@AddProductActivity, R.drawable.bg_card)
            )
            setAdapter(ArrayAdapter(this@AddProductActivity, R.layout.item_dropdown, UNITS))
            setOnItemClickListener { _, _, position, _ ->
                txtUnit.text = UNITS[position]
                dismiss()
            }
            show()
        }
    }

    private fun showDatePicker() {
        currentFocus?.let {
            getSystemService(InputMethodManager::class.java)
                .hideSoftInputFromWindow(it.windowToken, 0)
            it.clearFocus()
        }
        val today = MaterialDatePicker.todayInUtcMilliseconds()
        val selected = (viewModel.state.value.expiryDate?.let { isoToMillis(it) } ?: today)
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
            viewModel.selectDate(millisToIso(millis))
        }
        picker.show(supportFragmentManager, "expiry_date")
    }

    private fun render(s: AddProductState) = with(binding) {
        bindChips(
            categoriesContainer,
            s.categories.map { it.id to it.name },
            s.categoryId,
            viewModel::selectCategory
        )
        bindChips(
            locationsContainer,
            s.locations.map { it.id to it.name },
            s.locationId,
            viewModel::selectLocation
        )
        txtDate.text = displayDate(s.expiryDate)

        btnSave.isEnabled = !s.saving
        btnSave.alpha = if (s.saving) 0.6f else 1f
        txtSave.setText(
            when {
                s.saving -> R.string.btn_saving
                editing != null -> R.string.btn_save_changes
                else -> R.string.btn_save_product
            }
        )

        if (s.saved) {
            Toast.makeText(
                this@AddProductActivity,
                if (editing != null) R.string.product_updated else R.string.product_saved,
                Toast.LENGTH_SHORT
            ).show()
            setResult(Activity.RESULT_OK)
            finish()
        }
        if (s.showLoadError) {
            Toast.makeText(
                this@AddProductActivity, R.string.add_product_load_error, Toast.LENGTH_LONG
            ).show()
            viewModel.loadErrorShown()
        }
        if (s.showSaveError) {
            Toast.makeText(
                this@AddProductActivity, R.string.product_save_error, Toast.LENGTH_LONG
            ).show()
            viewModel.saveErrorShown()
        }
    }


    private fun bindChips(
        container: LinearLayout,
        items: List<Pair<String, String>>, // id to nombre
        selectedId: String?,
        onPick: (String) -> Unit
    ) {
        val sameItems = container.childCount == items.size &&
                items.indices.all { container.getChildAt(it).tag == items[it].first }

        if (!sameItems) {
            container.removeAllViews()
            items.forEach { (id, name) ->
                val chip = ItemFilterChipBinding.inflate(layoutInflater, container, false).root
                chip.text = name
                chip.tag = id
                chip.setOnClickListener { onPick(id) }
                container.addView(chip)
            }
        }

        for (i in 0 until container.childCount) {
            val chip = container.getChildAt(i) as TextView
            chip.setBackgroundResource(
                if (chip.tag == selectedId) R.drawable.bg_chip_green else R.drawable.bg_chip_white
            )
        }
    }


    private fun utcFormat(pattern: String) =
        SimpleDateFormat(pattern, Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }

    private fun isoToMillis(iso: String): Long? =
        try {
            utcFormat("yyyy-MM-dd").parse(iso)?.time
        } catch (e: Exception) {
            null
        }

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
        private const val EXTRA_PRODUCT = "product"
        private const val DEFAULT_DAYS = 7
        private const val MAX_QUANTITY = 99999.0
        private const val MAX_FUTURE_MILLIS = 3650L * 24L * 60L * 60L * 1000L
        private val UNITS = listOf("pzs", "kg", "g", "lb", "oz", "L", "ml", "paq")

        fun newIntent(context: Context, product: InventoryProduct? = null) =
            Intent(context, AddProductActivity::class.java).apply {
                product?.let {
                    putExtra(EXTRA_PRODUCT, Json.encodeToString(InventoryProduct.serializer(), it))
                }
            }
    }
}