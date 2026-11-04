package com.yummy.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.yummy.app.R
import com.yummy.app.databinding.ActivityRegisterBinding

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
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupChips()
        setupClicks()
    }

    private fun setupChips() {
        chips = listOf(
            binding.chip1, binding.chip2, binding.chip3,
            binding.chip4, binding.chip5, binding.chip6
        )
        chips.forEachIndexed { index, chip ->
            chip.setOnClickListener { selectChip(index + 1) }
        }
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
                ContextCompat.getColor(
                    this,
                    if (selected) R.color.white else R.color.icon_gray
                )
            )
        }
    }

    private fun setupClicks() = with(binding) {
        btnBack.setOnClickListener { goToLogin() }
        btnGoLogin.setOnClickListener { goToLogin() }

        btnRegister.setOnClickListener {
            // TODO: lógica de registro (más adelante)
        }
    }

    private fun goToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}