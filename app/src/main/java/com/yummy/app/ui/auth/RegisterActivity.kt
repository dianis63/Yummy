package com.yummy.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.yummy.app.R
import com.yummy.app.data.AuthRepository
import com.yummy.app.databinding.ActivityRegisterBinding
import com.yummy.app.MainActivity
import kotlinx.coroutines.launch
import com.yummy.app.ui.components.bindPasswordToggle

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var chips: List<TextView>
    private var selectedPeople = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        setupChips()
        setupClicks()
    }

    private fun setupChips() {
        chips = listOf(binding.chip1, binding.chip2, binding.chip3,
            binding.chip4, binding.chip5, binding.chip6)
        chips.forEachIndexed { i, chip -> chip.setOnClickListener { selectChip(i + 1) } }
        selectChip(selectedPeople)
    }

    private fun selectChip(people: Int) {
        selectedPeople = people
        chips.forEachIndexed { index, chip ->
            val selected = index + 1 == people
            chip.setBackgroundResource(
                if (selected) R.drawable.bg_chip_selected else R.drawable.bg_person_chip
            )
            chip.setTextColor(
                ContextCompat.getColor(this, if (selected) R.color.white else R.color.icon_gray)
            )
        }
    }

    private fun setupClicks() = with(binding) {
        btnBack.setOnClickListener { goToLogin() }
        btnGoLogin.setOnClickListener { goToLogin() }
        btnRegister.setOnClickListener { doRegister() }
        etPassword.bindPasswordToggle(btnTogglePassword)
    }
    private fun doRegister() {
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()

        if (name.isEmpty()) { binding.etName.error = "Escribe tu nombre"; return }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Correo no válido"; return
        }
        if (password.length < 6) { binding.etPassword.error = "Mínimo 6 caracteres"; return }

        setLoading(true)
        lifecycleScope.launch {
            try {
                AuthRepository.signUp(name, email, password, selectedPeople)
                Toast.makeText(
                    this@RegisterActivity,
                    "¡Cuenta creada! Ahora inicia sesión",
                    Toast.LENGTH_LONG
                ).show()
                goToLogin()
            } catch (e: Exception) {
                Toast.makeText(this@RegisterActivity, AuthRepository.friendlyError(e), Toast.LENGTH_LONG).show()
                setLoading(false)
            }
        }
    }

    private fun setLoading(loading: Boolean) = with(binding) {
        btnRegister.isEnabled = !loading
        btnRegister.alpha = if (loading) 0.6f else 1f
        btnRegister.text = if (loading) "Creando..." else getString(R.string.register_button)
    }

    private fun goToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}