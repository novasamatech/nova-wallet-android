package io.novafoundation.nova.feature_assets.presentation.trade.provider

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import io.novafoundation.nova.common.list.GroupedListHolder
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.databinding.ItemTradeProviderBinding

class TradeProviderAdapter(
    private val itemHandler: ItemHandler,
) : ListAdapter<TradeProviderRvItem, TradeProviderViewHolder>(DiffCallback) {

    interface ItemHandler {
        fun providerClicked(item: TradeProviderRvItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TradeProviderViewHolder {
        val binder = ItemTradeProviderBinding.inflate(parent.inflater(), parent, false)
        return TradeProviderViewHolder(binder, itemHandler)
    }

    override fun onBindViewHolder(holder: TradeProviderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

private object DiffCallback : DiffUtil.ItemCallback<TradeProviderRvItem>() {

    override fun areItemsTheSame(oldItem: TradeProviderRvItem, newItem: TradeProviderRvItem): Boolean {
        return oldItem.providerLogoRes == newItem.providerLogoRes
    }

    override fun areContentsTheSame(oldItem: TradeProviderRvItem, newItem: TradeProviderRvItem): Boolean {
        return oldItem == newItem
    }
}

class TradeProviderViewHolder(
    private val binder: ItemTradeProviderBinding,
    private val itemHandler: TradeProviderAdapter.ItemHandler,
) : GroupedListHolder(binder.root) {

    fun bind(item: TradeProviderRvItem) = with(containerView) {
        containerView.setOnClickListener { itemHandler.providerClicked(item) }

        binder.itemTradeProviderLogo.setImageResource(item.providerLogoRes)
        binder.itemTradeProviderDescription.text = item.description

        fillPaymentMethods(item)
    }

    private fun fillPaymentMethods(item: TradeProviderRvItem) = with(containerView) {
        binder.itemTradeProviderPaymentMethods.removeAllViews()

        item.paymentMethods.forEach {
            val view = when (it) {
                is TradeProviderRvItem.PaymentMethod.ByResId -> createImage(it.resId)
                is TradeProviderRvItem.PaymentMethod.ByText -> createText(it.text)
            }

            binder.itemTradeProviderPaymentMethods.addView(view)
        }
    }

    private fun createImage(resId: Int): View {
        return binder.itemTradeProviderPaymentMethods.inflateChild(R.layout.layout_payment_method_image, false)
            .apply {
                require(this is ImageView)
                this.setImageResource(resId)
            }
    }

    private fun createText(text: String): View {
        return binder.itemTradeProviderPaymentMethods.inflateChild(R.layout.layout_payment_method_text, false)
            .apply {
                require(this is TextView)
                this.text = text
            }
    }
}
