package com.yummy.app.ui.locations

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.yummy.app.R
import com.yummy.app.databinding.ActivityLocationsBinding
import com.yummy.app.ui.components.applyStatusBarPadding
import kotlinx.coroutines.launch

class LocationsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLocationsBinding
    private val viewModel: LocationsViewModel by viewModels()
    private val adapter = LocationAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
        binding = ActivityLocationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.header.applyStatusBarPadding()
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            v.updatePadding(bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom)
            insets
        }

        binding.rvLocations.adapter = adapter
        binding.btnBack.setOnClickListener { finish() }
        binding.fabAdd.setOnClickListener { openSheet() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { render(it) }
            }
        }
    }

    private fun openSheet() {
        if (supportFragmentManager.findFragmentByTag(NewLocationSheet.TAG) != null) return
        NewLocationSheet().show(supportFragmentManager, NewLocationSheet.TAG)
    }

    private fun render(s: LocationsState) = with(binding) {
        adapter.submitList(s.items)
        emptyState.isVisible = !s.loading && s.items.isEmpty()

        if (s.showLoadError) {
            Toast.makeText(this@LocationsActivity, R.string.locations_load_error, Toast.LENGTH_LONG).show()
            viewModel.loadErrorShown()
        }
    }

    companion object {
        fun newIntent(context: Context) = Intent(context, LocationsActivity::class.java)
    }
}