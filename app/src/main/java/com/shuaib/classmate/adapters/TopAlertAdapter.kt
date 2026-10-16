package com.shuaib.classmate.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.shuaib.classmate.databinding.ItemTopAlertBannerBinding
import com.shuaib.classmate.models.TopAlertItem

class TopAlertAdapter(
    private var items: List<TopAlertItem>
) : RecyclerView.Adapter<TopAlertAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemTopAlertBannerBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTopAlertBannerBinding.inflate(
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
        val item = items[realIndex]
        val b = holder.binding

        b.layoutAlertBg.setBackgroundResource(item.bgDrawableRes)
        b.ivAlertIcon.setImageResource(item.iconRes)
        b.tvAlertTitle.text = item.title
        b.tvAlertSubtitle.text = item.subtitle
        b.tvAlertBadgeTime.text = item.timeTag

        b.cardAlertRoot.setOnClickListener {
            item.onClick()
        }

        b.btnDismissAlert.setOnClickListener {
            item.onDismiss()
        }
    }

    override fun getItemCount(): Int {
        return if (items.size > 1) items.size * 1000 else items.size
    }

    fun getRealCount(): Int = items.size

    fun updateList(newItems: List<TopAlertItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
