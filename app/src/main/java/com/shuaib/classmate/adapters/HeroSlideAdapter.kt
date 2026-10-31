package com.shuaib.classmate.adapters

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.shuaib.classmate.databinding.ItemHomeHeroSlideBinding
import com.shuaib.classmate.models.HeroSlide

class HeroSlideAdapter(
    private var items: List<HeroSlide>
) : RecyclerView.Adapter<HeroSlideAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemHomeHeroSlideBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHomeHeroSlideBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        binding.root.layoutParams = RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (items.isEmpty()) return
        val realIndex = if (items.size > 1) position % items.size else position
        val slide = items[realIndex]
        val b = holder.binding

        b.layoutSlideBg.setBackgroundResource(slide.bgDrawableRes)
        b.ivCategoryIcon.setImageResource(slide.categoryIconRes)
        b.ivCategoryIcon.imageTintList = ColorStateList.valueOf(slide.categoryColor)
        b.tvCategoryTitle.text = slide.categoryTitle
        b.tvCategoryTitle.setTextColor(slide.categoryColor)
        b.tvActionText.text = slide.actionText
        b.tvStatusBadge.text = slide.statusBadge
        b.tvContextBadge.text = slide.contextBadge
        b.tvMainTitle.text = slide.title
        b.tvDetail1.text = slide.detail1
        b.tvDetail2.text = slide.detail2
        b.tvDetail3.text = slide.detail3

        if (slide.secondaryTitle.isNotBlank()) {
            b.layoutSecondaryClass.visibility = android.view.View.VISIBLE
            b.tvSecondaryClassTitle.text = slide.secondaryTitle
            b.tvSecondaryClassDetail.text = slide.secondaryDetail
        } else {
            b.layoutSecondaryClass.visibility = android.view.View.GONE
        }

        b.cardSlideRoot.setOnClickListener {
            slide.onClick()
        }
        b.tvActionText.setOnClickListener {
            slide.onClick()
        }
    }

    override fun getItemCount(): Int {
        return if (items.size > 1) items.size * 1000 else items.size
    }

    fun getRealCount(): Int = items.size

    fun updateList(newItems: List<HeroSlide>) {
        items = newItems
        notifyDataSetChanged()
    }
}
