package com.yummy.app.ui.categories

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.yummy.app.R
import com.yummy.app.data.model.CategoryRow
import com.yummy.app.databinding.ActivityCategoriesBinding
import com.yummy.app.ui.components.applyStatusBarPadding
import kotlinx.coroutines.launch

class CategoriesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCategoriesBinding
    private val viewModel: CategoriesViewModel by viewModels()
    private val adapter = CategoryAdapter(onDelete = { viewModel.requestDelete(it) })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
        binding = ActivityCategoriesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.header.applyStatusBarPadding()
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            v.updatePadding(bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom)
            insets
        }

        binding.rvCategories.adapter = adapter
        binding.btnBack.setOnClickListener { finish() }
        binding.fabAdd.setOnClickListener { openSheet() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { render(it) }
            }
        }
    }

    private fun openSheet() {
        if (supportFragmentManager.findFragmentByTag(NewCategorySheet.TAG) != null) return
        NewCategorySheet().show(supportFragmentManager, NewCategorySheet.TAG)
    }

    private fun confirmDelete(category: CategoryRow) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.category_delete_title)
            .setMessage(getString(R.string.category_delete_message, category.name))
            .setPositiveButton(R.string.btn_delete) { _, _ -> viewModel.delete(category) }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun render(s: CategoriesState) = with(binding) {
        adapter.submitList(s.items)
        emptyState.isVisible = !s.loading && s.items.isEmpty()

        s.confirmDelete?.let {
            viewModel.confirmHandled()
            confirmDelete(it)
        }
        if (s.inUseName != null) {
            val message = resources.getQuantityString(
                R.plurals.category_in_use, s.inUseCount, s.inUseName, s.inUseCount
            )
            viewModel.inUseHandled()
            toast(message)
        }
        if (s.showLoadError) {
            toast(R.string.categories_load_error)
            viewModel.loadErrorShown()
        }
        if (s.showDeleteError) {
            toast(R.string.category_delete_error)
            viewModel.deleteErrorShown()
        }
    }

    private fun toast(@StringRes resId: Int) = toast(getString(resId))

    private fun toast(message: String) =
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()

    companion object {
        fun newIntent(context: Context) = Intent(context, CategoriesActivity::class.java)
    }
}
