package com.yummy.app.ui.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.yummy.app.MainActivity
import com.yummy.app.R
import com.yummy.app.data.AuthRepository
import com.yummy.app.data.local.AppPrefs
import com.yummy.app.databinding.ActivityOnboardingBinding
import com.yummy.app.ui.auth.LoginActivity
import com.yummy.app.ui.auth.RegisterActivity
import kotlinx.coroutines.launch

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding

    private val pages = listOf(
        OnboardingPage(
            R.drawable.ic_onboarding_broccoli,
            R.string.onboarding_1_title,
            R.string.onboarding_1_desc,
            R.color.ring_green
        ),
        OnboardingPage(
            R.drawable.ic_onboarding_carrot,
            R.string.onboarding_2_title,
            R.string.onboarding_2_desc,
            R.color.ring_yellow
        ),
        OnboardingPage(
            R.drawable.ic_onboarding_strawberry,
            R.string.onboarding_3_title,
            R.string.onboarding_3_desc,
            R.color.ring_pink
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            // 1. Con sesión activa: directo a la pantalla principal
            if (AuthRepository.hasSession()) {
                startActivity(Intent(this@OnboardingActivity, MainActivity::class.java))
                finish()
                return@launch
            }

            // 2. Sin sesión pero ya vio el onboarding: al login
            if (AppPrefs.isOnboardingDone(this@OnboardingActivity)) {
                startActivity(Intent(this@OnboardingActivity, LoginActivity::class.java))
                finish()
                return@launch
            }

            // 3. Mostrar el onboarding
            binding = ActivityOnboardingBinding.inflate(layoutInflater)
            setContentView(binding.root)
            setupPager()
            setupClicks()
        }
    }

    private fun setupPager() = with(binding) {
        pager.adapter = OnboardingAdapter(pages)
        indicator.count = pages.size
        updateUi(0)

        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) = updateUi(position)
        })
    }

    private fun setupClicks() = with(binding) {
        btnNext.setOnClickListener {
            if (pager.currentItem == pages.lastIndex) {
                finishOnboarding(RegisterActivity::class.java)
            } else {
                pager.currentItem += 1
            }
        }
        btnSkip.setOnClickListener { finishOnboarding(LoginActivity::class.java) }
        btnLogin.setOnClickListener { finishOnboarding(LoginActivity::class.java) }
    }

    private fun updateUi(position: Int) = with(binding) {
        indicator.selectedIndex = position
        btnNext.setText(
            if (position == pages.lastIndex) R.string.onboarding_start
            else R.string.onboarding_next
        )
    }

    private fun finishOnboarding(target: Class<*>) {
        AppPrefs.setOnboardingDone(this)
        startActivity(Intent(this, target))
        finish()
    }
}