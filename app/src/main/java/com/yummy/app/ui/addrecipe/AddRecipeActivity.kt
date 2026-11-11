package com.yummy.app.ui.addrecipe

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.ListPopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil3.load
import com.yummy.app.R
import com.yummy.app.data.model.Difficulty
import com.yummy.app.data.model.MealType
import com.yummy.app.data.model.ParsedIngredient
import com.yummy.app.data.model.RecipeUnits
import com.yummy.app.databinding.ActivityAddRecipeBinding
import com.yummy.app.databinding.ItemIngredientInputBinding
import com.yummy.app.ui.components.applyStatusBarPadding
import com.yummy.app.ui.components.pretty
import kotlinx.coroutines.launch

class AddRecipeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddRecipeBinding
    private val viewModel: AddRecipeViewModel by viewModels()

    private lateinit var mealChips: Map<MealType, TextView>
    private lateinit var difficultyChips: Map<Difficulty, TextView>

    // Filas de ingredientes que se están mostrando (id de la fila -> sus vistas)
    private val rowBindings = mutableMapOf<Long, ItemIngredientInputBinding>()
    private var renderedIds: List<Long> = emptyList()

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) viewModel.selectImage(uri) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
        binding = ActivityAddRecipeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.header.applyStatusBarPadding()
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            )
            v.updatePadding(bottom = bars.bottom)
            insets
        }

        mealChips = mapOf(
            MealType.BREAKFAST to binding.chipBreakfast,
            MealType.LUNCH to binding.chipLunch,
            MealType.DINNER to binding.chipDinner,
            MealType.SNACK to binding.chipSnack
        )
        difficultyChips = mapOf(
            Difficulty.EASY to binding.chipEasy,
            Difficulty.MEDIUM to binding.chipMedium,
            Difficulty.HARD to binding.chipHard
        )

        setupClicks()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect { render(it) } }
                launch { viewModel.rows.collect { renderRows(it) } }
            }
        }
    }

    private fun setupClicks() = with(binding) {
        btnBack.setOnClickListener { finish() }
        imageCard.setOnClickListener {
            pickImage.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
        mealChips.forEach { (type, chip) -> chip.setOnClickListener { viewModel.selectMeal(type) } }
        difficultyChips.forEach { (level, chip) ->
            chip.setOnClickListener { viewModel.selectDifficulty(level) }
        }
        btnAddIngredient.setOnClickListener { viewModel.addRow() }
        btnSave.setOnClickListener { doSave() }
    }

    // ---------------- Ingredientes ----------------

    /** Solo reconstruye las tarjetas cuando se agrega o quita una fila (no al escribir). */
    private fun renderRows(rows: List<IngredientInput>) {
        val ids = rows.map { it.id }
        if (ids == renderedIds) return
        val added = renderedIds.isNotEmpty() && ids.size > renderedIds.size
        renderedIds = ids

        val container = binding.ingredientsContainer
        container.removeAllViews()
        rowBindings.clear()
        rows.forEach { row ->
            val b = ItemIngredientInputBinding.inflate(layoutInflater, container, false)
            bindRow(b, row, canRemove = rows.size > 1)
            rowBindings[row.id] = b
            container.addView(b.root)
        }
        if (added) rowBindings[ids.last()]?.etIngName?.requestFocus()
    }

    private fun bindRow(b: ItemIngredientInputBinding, row: IngredientInput, canRemove: Boolean) {
        val id = row.id
        // Los datos viven en el ViewModel; así Android no mezcla el estado de filas con el mismo id
        b.root.isSaveFromParentEnabled = false

        b.etIngName.setText(row.name)
        b.etQty.setText(row.quantity)
        b.txtUnit.text = row.unit
        applyUnitUi(b, row.unit)
        b.btnRemove.isVisible = canRemove

        b.etIngName.doAfterTextChanged { text ->
            viewModel.updateRow(id) { r -> r.copy(name = text.toString()) }
        }
        b.etQty.doAfterTextChanged { text ->
            viewModel.updateRow(id) { r -> r.copy(quantity = text.toString()) }
        }
        b.btnMinus.setOnClickListener { stepQuantity(b, -1) }
        b.btnPlus.setOnClickListener { stepQuantity(b, 1) }
        b.unitField.setOnClickListener { showUnitMenu(b, id) }
        b.btnRemove.setOnClickListener { viewModel.removeRow(id) }
    }

    /** "al gusto" no lleva cantidad: se oculta el selector de cantidad. */
    private fun applyUnitUi(b: ItemIngredientInputBinding, unit: String) {
        b.stepperField.isVisible = unit != RecipeUnits.TO_TASTE
    }

    private fun stepQuantity(b: ItemIngredientInputBinding, direction: Int) {
        val step = RecipeUnits.step(b.txtUnit.text.toString())
        val current = b.etQty.text.toString().trim().replace(',', '.').toDoubleOrNull() ?: 0.0
        val next = Math.round((current + direction * step) * 100) / 100.0
        if (next > 0.0 && next <= MAX_QUANTITY) {
            b.etQty.setText(next.pretty())
            b.etQty.setSelection(b.etQty.text.length)
        }
    }

    private fun showUnitMenu(b: ItemIngredientInputBinding, id: Long) {
        currentFocus?.let {
            getSystemService(InputMethodManager::class.java)
                .hideSoftInputFromWindow(it.windowToken, 0)
            it.clearFocus()
        }
        val units = RecipeUnits.all
        ListPopupWindow(this).apply {
            anchorView = b.unitField
            width = b.unitField.width
            isModal = true
            setBackgroundDrawable(ContextCompat.getDrawable(this@AddRecipeActivity, R.drawable.bg_card))
            setAdapter(ArrayAdapter(this@AddRecipeActivity, R.layout.item_dropdown, units))
            setOnItemClickListener { _, _, position, _ ->
                val unit = units[position]
                b.txtUnit.text = unit
                applyUnitUi(b, unit)
                viewModel.updateRow(id) { r -> r.copy(unit = unit) }
                dismiss()
            }
            show()
        }
    }

    /** Devuelve los ingredientes válidos, o null si hay un error (ya mostrado en pantalla). */
    private fun collectIngredients(): List<ParsedIngredient>? {
        val allRows = viewModel.rows.value
        val filled = allRows.filter { it.name.isNotBlank() }
        if (filled.isEmpty()) {
            rowBindings[allRows.first().id]?.etIngName?.error =
                getString(R.string.error_recipe_ingredients)
            return null
        }

        val result = mutableListOf<ParsedIngredient>()
        for (row in filled) {
            val quantity = if (row.unit == RecipeUnits.TO_TASTE) {
                1.0
            } else {
                row.quantity.trim().replace(',', '.').toDoubleOrNull()
            }
            if (quantity == null || quantity <= 0.0 || quantity > MAX_QUANTITY) {
                rowBindings[row.id]?.etQty?.error = getString(R.string.error_ingredient_quantity)
                return null
            }
            result += ParsedIngredient(
                name = row.name.trim().take(60),
                quantity = Math.round(quantity * 100) / 100.0,
                unit = row.unit
            )
        }
        return result
    }

    // ---------------- Guardar ----------------

    private fun doSave() {
        val name = binding.etName.text.toString().trim()
        val minutes = binding.etMinutes.text.toString().trim().toIntOrNull()
        val steps = binding.etSteps.text.toString()
            .lines().map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n")

        if (name.isEmpty()) {
            binding.etName.error = getString(R.string.error_recipe_name); return
        }
        if (minutes == null || minutes < 1 || minutes > 999) {
            binding.etMinutes.error = getString(R.string.error_recipe_time); return
        }
        val ingredients = collectIngredients() ?: return

        viewModel.save(name, minutes, ingredients, steps.ifEmpty { null })
    }

    private fun render(s: AddRecipeState) = with(binding) {
        mealChips.forEach { (type, chip) ->
            val on = type == s.mealType
            chip.setBackgroundResource(if (on) R.drawable.bg_button_primary else R.drawable.bg_chip_white)
            chip.setTextColor(
                ContextCompat.getColor(this@AddRecipeActivity, if (on) R.color.white else R.color.text_primary)
            )
        }
        difficultyChips.forEach { (level, chip) ->
            val on = level == s.difficulty
            chip.setBackgroundResource(if (on) R.drawable.bg_chip_green else R.drawable.bg_chip_white)
            chip.setTextColor(ContextCompat.getColor(this@AddRecipeActivity, R.color.text_primary))
        }

        if (s.imageUri != null && imgCover.tag != s.imageUri) {
            imgCover.tag = s.imageUri
            imgCover.load(s.imageUri)
        }
        txtImageLabel.setText(
            if (s.imageUri != null) R.string.recipe_image_change else R.string.recipe_image_add
        )

        btnSave.isEnabled = !s.saving
        btnSave.alpha = if (s.saving) 0.6f else 1f
        txtSave.setText(if (s.saving) R.string.btn_saving else R.string.btn_save_recipe)

        if (s.saved) {
            Toast.makeText(this@AddRecipeActivity, R.string.recipe_saved, Toast.LENGTH_SHORT).show()
            setResult(Activity.RESULT_OK)
            finish()
        }
        if (s.showSaveError) {
            Toast.makeText(this@AddRecipeActivity, R.string.recipe_save_error, Toast.LENGTH_LONG).show()
            viewModel.saveErrorShown()
        }
    }

    companion object {
        private const val MAX_QUANTITY = 99999.0

        fun newIntent(context: Context) = Intent(context, AddRecipeActivity::class.java)
    }
}