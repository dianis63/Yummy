package com.yummy.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.yummy.app.R
import com.yummy.app.data.AuthRepository
import com.yummy.app.databinding.ActivityLoginBinding
import com.yummy.app.MainActivity // <-- ajusta a tu pantalla principal
import com.yummy.app.ui.components.bindPasswordToggle
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        setupClicks()
    }

    private fun setupClicks() = with(binding) {
        etPassword.bindPasswordToggle(btnTogglePassword)
        btnGoRegister.setOnClickListener {
            startActivity(Intent(this@LoginActivity, RegisterActivity::class.java))
            finish()
        }

        btnLogin.setOnClickListener { doLogin() }
    }

    private fun doLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Correo no válido"; return
        }
        if (password.length < 6) {
            binding.etPassword.error = "Mínimo 6 caracteres"; return
        }

        setLoading(true)
        lifecycleScope.launch {
            try {
                AuthRepository.signIn(email, password)
                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, AuthRepository.friendlyError(e), Toast.LENGTH_LONG).show()
                setLoading(false)
            }
        }
    }

    private fun setLoading(loading: Boolean) = with(binding) {
        btnLogin.isEnabled = !loading
        btnLogin.alpha = if (loading) 0.6f else 1f
        btnLogin.text = if (loading) "Entrando..." else getString(R.string.login_button)
    }

}