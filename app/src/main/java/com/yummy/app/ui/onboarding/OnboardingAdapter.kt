package com.yummy.app.ui.onboarding

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.yummy.app.databinding.ItemOnboardingBinding

class OnboardingAdapter(
    private val pages: List<OnboardingPage>
) : RecyclerView.Adapter<OnboardingAdapter.PageViewHolder>() {

    class PageViewHolder(val binding: ItemOnboardingBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = PageViewHolder(
        ItemOnboardingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun getItemCount() = pages.size

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        val page = pages[position]
        with(holder.binding) {
            imgIllustration.setImageResource(page.image)
            txtTitle.setText(page.title)
            txtDescription.setText(page.description)
            ringCircle.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(root.context, page.ringColor)
            )
        }
    }
}