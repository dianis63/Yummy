package com.yummy.app.ui.inventory

import android.app.Activity
import android.content.DialogInterface
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.yummy.app.R
import com.yummy.app.data.model.InventoryProduct
import com.yummy.app.databinding.DialogProductDetailBinding
import com.yummy.app.ui.addproduct.AddProductActivity
import com.yummy.app.ui.components.CategoryIcons
import com.yummy.app.ui.components.ExpiryUi
import com.yummy.app.ui.components.applyStatusBarPadding
import com.yummy.app.ui.components.pretty
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class ProductDetailDialog : DialogFragment(R.layout.dialog_product_detail) {

    private var _binding: DialogProductDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProductDetailViewModel by viewModels()

    private val editLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) viewModel.reload()
    }

    override fun getTheme(): Int = R.style.Theme_Yummy_FullScreenDialog

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = DialogProductDetailBinding.bind(view)

        val args = requireArguments()
        val json = args.getString(ARG_PRODUCT)
        if (json != null) {
            viewModel.start(Json.decodeFromString(InventoryProduct.serializer(), json))
        } else {
            viewModel.startWithId(args.getString(ARG_ID)!!)
        }

        binding.header.applyStatusBarPadding()
        applyBottomInset()
        setupRows()
        setupClicks()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { render(it) }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = true
        }
        view?.let { ViewCompat.requestApplyInsets(it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDismiss(dialog: DialogInterface) {
        if (viewModel.state.value.changed) {
            parentFragmentManager.setFragmentResult(RESULT_KEY, Bundle())
        }
        super.onDismiss(dialog)
    }

    private fun applyBottomInset() {
        val base = binding.bottomBar.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomBar) { v, insets ->
            val bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            v.updatePadding(bottom = base + bottom)
            insets
        }
        ViewCompat.requestApplyInsets(binding.bottomBar)
    }

    private fun setupRows() = with(binding) {
        rowQuantity.imgIcon.setImageResource(R.drawable.ic_nav_inventory)
        rowQuantity.txtLabel.setText(R.string.detail_quantity)
        rowCategory.imgIcon.setImageResource(R.drawable.ic_tag)
        rowCategory.txtLabel.setText(R.string.detail_category)
        rowLocation.imgIcon.setImageResource(R.drawable.ic_pin)
        rowLocation.txtLabel.setText(R.string.detail_location)
        rowPurchase.imgIcon.setImageResource(R.drawable.ic_calendar)
        rowPurchase.txtLabel.setText(R.string.detail_purchase_date)
        rowExpiry.imgIcon.setImageResource(R.drawable.ic_alarm)
        rowExpiry.txtLabel.setText(R.string.detail_expiry)
    }

    private fun setupClicks() = with(binding) {
        btnBack.setOnClickListener { dismiss() }
        btnFavorite.setOnClickListener { viewModel.toggleFavorite() }
        btnEdit.setOnClickListener {
            val product = viewModel.state.value.product ?: return@setOnClickListener
            editLauncher.launch(AddProductActivity.newIntent(requireContext(), product))
        }
        btnDelete.setOnClickListener { confirmDelete() }
        btnConsumed.setOnClickListener { viewModel.markConsumed() }
    }

    private fun confirmDelete() {
        val product = viewModel.state.value.product ?: return
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_confirm_title)
            .setMessage(getString(R.string.delete_confirm_message, product.name))
            .setPositiveButton(R.string.btn_delete) { _, _ -> viewModel.delete() }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun render(s: ProductDetailState) {
        val ctx = requireContext()

        if (s.showError) {
            Toast.makeText(ctx, R.string.detail_error, Toast.LENGTH_LONG).show()
            viewModel.errorShown()
            if (s.product == null) {
                dismiss()
                return
            }
        }

        val p = s.product
        binding.root.alpha = if (p == null) 0f else 1f
        if (p == null) return

        val ui = ExpiryUi.from(p.expiryStatus)
        val fg = ColorStateList.valueOf(ContextCompat.getColor(ctx, ui.fg))
        val bg = ColorStateList.valueOf(ContextCompat.getColor(ctx, ui.bg))

        with(binding) {
            imgIcon.setImageResource(CategoryIcons.forKey(p.categoryIcon))
            imgIcon.backgroundTintList = bg
            imgIcon.imageTintList = fg
            txtName.text = p.name

            chip.backgroundTintList = bg
            dot.backgroundTintList = fg
            txtChip.setText(ui.label)
            txtChip.setTextColor(fg)

            txtDays.text = daysText(p.daysLeft)
            txtDays.isVisible = txtDays.text.isNotEmpty()

            btnFavorite.setImageResource(
                if (p.isFavorite) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
            )

            rowQuantity.txtValue.text = "${p.quantity.pretty()} ${p.unit}".trim()
            rowCategory.txtValue.text = p.categoryName ?: getString(R.string.detail_no_category)
            rowLocation.txtValue.text = p.locationName ?: getString(R.string.detail_no_location)
            rowPurchase.txtValue.text = purchaseDate(p.createdAt)
            rowExpiry.txtValue.text = formatDate(p.expiryDate)

            btnDelete.isEnabled = !s.busy
            btnConsumed.isEnabled = !s.busy
            bottomBar.alpha = if (s.busy) 0.6f else 1f
        }

        s.result?.let { result ->
            Toast.makeText(
                ctx,
                if (result == DetailResult.CONSUMED) R.string.product_consumed
                else R.string.product_deleted,
                Toast.LENGTH_SHORT
            ).show()
            viewModel.resultHandled()
            dismiss()
        }
    }

    private fun daysText(days: Int?): String = when {
        days == null -> ""
        days < 0 -> getString(R.string.detail_expired_ago, daysPlural(-days))
        days == 0 -> getString(R.string.expiry_today)
        else -> getString(R.string.detail_expires_in, daysPlural(days))
    }

    private fun daysPlural(n: Int) = resources.getQuantityString(R.plurals.days, n, n)

    private fun formatDate(iso: String?): String {
        val parts = iso?.take(10)?.split("-")
        val year = parts?.getOrNull(0)?.toIntOrNull()
        val month = parts?.getOrNull(1)?.toIntOrNull()
        val day = parts?.getOrNull(2)?.toIntOrNull()
        return if (year == null || month == null || day == null) NO_DATE else "$day/$month/$year"
    }

    private fun purchaseDate(createdAt: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = parser.parse(createdAt.take(19)) ?: return NO_DATE
            SimpleDateFormat("d/M/yyyy", Locale.US).format(date)
        } catch (e: Exception) {
            formatDate(createdAt)
        }
    }

    companion object {
        const val TAG = "ProductDetailDialog"
        const val RESULT_KEY = "product_changed"
        private const val ARG_PRODUCT = "product"
        private const val ARG_ID = "product_id"
        private const val NO_DATE = "—"

        fun newInstance(product: InventoryProduct) = ProductDetailDialog().apply {
            arguments = Bundle().apply {
                putString(ARG_PRODUCT, Json.encodeToString(InventoryProduct.serializer(), product))
            }
        }

        fun newInstanceFor(productId: String) = ProductDetailDialog().apply {
            arguments = Bundle().apply { putString(ARG_ID, productId) }
        }
    }
}