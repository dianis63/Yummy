package com.yummy.app.ui.profile

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.yummy.app.BuildConfig
import com.yummy.app.R
import com.yummy.app.databinding.FragmentProfileBinding
import com.yummy.app.ui.components.applyStatusBarPadding
import com.yummy.app.ui.main.MainNavigator
import kotlinx.coroutines.launch

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()

    private val daysOptions = listOf(1, 2, 3, 5, 7)
    private val unitsOptions = listOf("metric" to R.string.units_metric, "imperial" to R.string.units_imperial)
    private val householdOptions = (1..10).toList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentProfileBinding.bind(view)
        binding.header.applyStatusBarPadding()
        binding.txtVersion.text = getString(R.string.profile_version, BuildConfig.VERSION_NAME)
        setupClicks()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { render(it) }
            }
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

    private fun setupClicks() = with(binding) {
        val profile = { viewModel.state.value.profile }

        rowNotifyDays.setOnClickListener {
            showChoice(
                R.string.profile_notify_days,
                daysOptions.map { daysText(it) },
                daysOptions.indexOf(profile().notifyDaysBefore)
            ) { viewModel.setNotifyDays(daysOptions[it]) }
        }

        rowUnits.setOnClickListener {
            showChoice(
                R.string.profile_units,
                unitsOptions.map { getString(it.second) },
                unitsOptions.indexOfFirst { it.first == profile().units }
            ) { viewModel.setUnits(unitsOptions[it].first) }
        }

        rowMembers.setOnClickListener {
            showChoice(
                R.string.profile_members,
                householdOptions.map { it.toString() },
                householdOptions.indexOf(profile().householdSize)
            ) { viewModel.setHousehold(householdOptions[it]) }
        }

        // TODO: abrir las pantallas de Categorías y Ubicaciones cuando existan
        rowCategories.setOnClickListener { comingSoon() }
        rowLocations.setOnClickListener { comingSoon() }

        rowLogout.setOnClickListener { (requireActivity() as MainNavigator).logout() }
    }

    private fun render(s: ProfileState) = with(binding) {
        val p = s.profile
        txtInitial.text = p.name.trim().take(1).uppercase().ifEmpty { "?" }
        txtName.text = p.name
        txtEmail.text = s.email
        txtHousehold.text = getString(R.string.profile_household_chip, p.householdSize)

        txtProducts.text = s.stats.products.toString()
        txtRecipes.text = s.stats.recipes.toString()
        txtFavorites.text = s.stats.favorites.toString()

        rowNotifyDays.value = daysText(p.notifyDaysBefore)
        rowUnits.value = getString(
            unitsOptions.firstOrNull { it.first == p.units }?.second ?: R.string.units_metric
        )
        rowMembers.value = p.householdSize.toString()

        if (s.showError) {
            Toast.makeText(requireContext(), R.string.profile_error, Toast.LENGTH_LONG).show()
            viewModel.errorShown()
        }
    }

    private fun daysText(days: Int) = resources.getQuantityString(R.plurals.days, days, days)

    private fun comingSoon() =
        Toast.makeText(requireContext(), R.string.coming_soon, Toast.LENGTH_SHORT).show()

    private fun showChoice(
        @StringRes title: Int,
        items: List<String>,
        selected: Int,
        onPick: (Int) -> Unit
    ) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setSingleChoiceItems(items.toTypedArray(), selected) { dialog, which ->
                onPick(which)
                dialog.dismiss()
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }
}