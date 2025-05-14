package io.novafoundation.nova.feature_pay_impl.presentation.shop.adapter

import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.list.SingleItemAdapter
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.feature_pay_impl.databinding.ItemShopHeaderBinding

class ShopHeaderAdapter : SingleItemAdapter<ShopHeaderViewHolder>() {

    private var headerText: String? = null
    private var showShimmering: Boolean = false

    fun setHeaderText(text: String) {
        headerText = text
        notifyChangedIfShown()
    }

    fun showShimmering(showShimmering: Boolean) {
        this.showShimmering = showShimmering
        notifyChangedIfShown()
    }

    override fun onBindViewHolder(holder: ShopHeaderViewHolder, position: Int) {
        holder.bind(headerText, showShimmering)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShopHeaderViewHolder {
        return ShopHeaderViewHolder(ItemShopHeaderBinding.inflate(parent.inflater(), parent, false))
    }
}

class ShopHeaderViewHolder(private val binding: ItemShopHeaderBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(text: String?, showShimmering: Boolean) {
        binding.brandsHeaderTitleShimmering.isVisible = showShimmering
        binding.brandsHeaderTitle.text = text
    }
}
