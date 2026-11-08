package com.yummy.app.ui.main

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.yummy.app.R

class PlaceholderFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val ctx = requireContext()
        val title = TextView(ctx).apply {
            setText(requireArguments().getInt(ARG_TITLE))
            textSize = 22f
            gravity = Gravity.CENTER
            setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
        }
        val logout = Button(ctx).apply {
            setText(R.string.logout_temp)
            setOnClickListener { (requireActivity() as MainNavigator).logout() }
        }
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            addView(title)
            addView(logout)
        }
    }

    companion object {
        private const val ARG_TITLE = "title"
        fun newInstance(@StringRes title: Int) = PlaceholderFragment().apply {
            arguments = Bundle().apply { putInt(ARG_TITLE, title) }
        }
    }
}