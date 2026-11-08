package com.yummy.app.ui.components

import android.content.Context
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import com.yummy.app.R

enum class ExpiryUi(
    @ColorRes val bg: Int,
    @ColorRes val fg: Int,
    @StringRes val label: Int
) {
    EXPIRED(R.color.status_expired_bg, R.color.status_expired_fg, R.string.status_expired),
    EXPIRING(R.color.status_expiring_bg, R.color.status_expiring_fg, R.string.status_expiring),
    FRESH(R.color.status_fresh_bg, R.color.status_fresh_fg, R.string.status_fresh);

    companion object {
        fun from(status: String) = when (status) {
            "expired" -> EXPIRED
            "expiring" -> EXPIRING
            else -> FRESH
        }

        fun daysText(context: Context, days: Int?): String = when {
            days == null -> ""
            days < 0 -> context.getString(R.string.expiry_ago, -days)
            days == 0 -> context.getString(R.string.expiry_today)
            else -> context.getString(R.string.expiry_in, days)
        }
    }
}