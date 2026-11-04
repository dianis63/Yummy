package com.yummy.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.yummy.app.R
import com.yummy.app.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private var passwordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupClicks()
    }

    private fun setupClicks() = with(binding) {
        btnTogglePassword.setOnClickListener { togglePassword() }

        btnGoRegister.setOnClickListener {
            startActivity(Intent(this@LoginActivity, RegisterActivity::class.java))
            finish()
        }

        btnLogin.setOnClickListener {
            // TODO: lógica de inicio de sesión (más adelante)
        }
    }

    private fun togglePassword() = with(binding) {
        passwordVisible = !passwordVisible
        etPassword.inputType = InputType.TYPE_CLASS_TEXT or
                if (passwordVisible) InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                else InputType.TYPE_TEXT_VARIATION_PASSWORD
        // Al cambiar inputType se pierde la fuente, la volvemos a poner
        etPassword.typeface = ResourcesCompat.getFont(this@LoginActivity, R.font.segoe_ui)
        etPassword.setSelection(etPassword.text.length)
        btnTogglePassword.setImageResource(
            if (passwordVisible) R.drawable.ic_eye_off else R.drawable.ic_eye
        )
    }
}