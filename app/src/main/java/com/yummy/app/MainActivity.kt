package com.yummy.app
import com.yummy.app.ui.profile.ProfileFragment
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.yummy.app.data.AuthRepository
import com.yummy.app.databinding.ActivityMainBinding
import com.yummy.app.ui.auth.LoginActivity
import com.yummy.app.ui.components.SpeedDialAction
import com.yummy.app.ui.components.dp
import com.yummy.app.ui.home.HomeFragment
import com.yummy.app.ui.main.MainNavigator
import com.yummy.app.ui.main.PlaceholderFragment
import com.yummy.app.ui.main.Tab
import com.yummy.app.ui.inventory.InventoryFragment
import com.yummy.app.ui.addproduct.AddProductActivity
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), MainNavigator {

    private lateinit var binding: ActivityMainBinding
    private var currentTab = Tab.HOME

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Barra de estado transparente con íconos claros (los encabezados son rojos)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentTab = Tab.entries[savedInstanceState?.getInt(KEY_TAB) ?: 0]

        setupInsets()
        setupNav()
        setupSpeedDial()
        setupBack()

        binding.bottomNav.select(currentTab)
        binding.speedDial.setFabVisible(currentTab.showFab)
        if (savedInstanceState == null) showTab(currentTab)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_TAB, currentTab.ordinal)
    }

    // ---------- MainNavigator ----------
    override fun navigateTo(tab: Tab) {
        if (tab == currentTab) return
        binding.bottomNav.select(tab)
        showTab(tab)
    }

    override fun openAddMenu() = binding.speedDial.open()

    override fun logout() {
        lifecycleScope.launch {
            AuthRepository.signOut()
            startActivity(
                Intent(this@MainActivity, LoginActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
        }
    }

    // ---------- Configuración ----------
    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            binding.bottomNav.updateLayoutParams<FrameLayout.LayoutParams> {
                bottomMargin = bottom + dp(16)
            }
            // 16 (margen) + 64 (alto aprox. de la barra)
            binding.speedDial.setBottomInset(bottom + dp(16) + dp(64))
            insets
        }
    }

    private fun setupNav() {
        binding.bottomNav.onTabSelected = { navigateTo(it) }
    }

    private fun setupSpeedDial() {
        binding.speedDial.setActions(
            listOf(
                SpeedDialAction(ACTION_NEW_RECIPE, R.string.fab_new_recipe, R.drawable.ic_nav_recipes),
                SpeedDialAction(ACTION_ADD_SHOPPING, R.string.fab_add_shopping, R.drawable.ic_nav_cart),
                SpeedDialAction(ACTION_ADD_PRODUCT, R.string.fab_add_product, R.drawable.ic_nav_inventory)
            )
        )
        binding.speedDial.onActionClick = { action ->
            // TODO compañeros: abrir aquí la pantalla de cada acción (action.id)
            if (action.id == ACTION_ADD_PRODUCT) {
                startActivity(AddProductActivity.newIntent(this))
            } else {
                Toast.makeText(this, R.string.coming_soon, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupBack() {
        onBackPressedDispatcher.addCallback(this) {
            if (binding.speedDial.isOpen) binding.speedDial.close() else finish()
        }
    }

    private fun showTab(tab: Tab) {
        currentTab = tab
        binding.speedDial.setFabVisible(tab.showFab)
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.container, fragmentFor(tab))
        }
    }

    // TODO compañeros: reemplazar cada PlaceholderFragment por su pantalla real
    private fun fragmentFor(tab: Tab): Fragment = when (tab) {
        Tab.HOME -> HomeFragment()
        Tab.PROFILE -> ProfileFragment()
        Tab.INVENTORY -> InventoryFragment()
        else -> PlaceholderFragment.newInstance(tab.label)
    }

    companion object {
        private const val KEY_TAB = "tab"
        const val ACTION_NEW_RECIPE = 1
        const val ACTION_ADD_SHOPPING = 2
        const val ACTION_ADD_PRODUCT = 3
    }
}