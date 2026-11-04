package com.yummy.app.data.local

import android.content.Context
object AppPrefs {
    private const val FILE = "yummy_prefs"
    private const val KEY_ONBOARDING_DONE = "onboarding_done"

    // true = siempre muestra el onboarding
    private const val ALWAYS_SHOW_ONBOARDING = true

    fun isOnboardingDone(context: Context): Boolean {
        if (ALWAYS_SHOW_ONBOARDING) return false
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getBoolean(KEY_ONBOARDING_DONE, false)
    }

    fun setOnboardingDone(context: Context) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ONBOARDING_DONE, true)
            .apply()
    }
}