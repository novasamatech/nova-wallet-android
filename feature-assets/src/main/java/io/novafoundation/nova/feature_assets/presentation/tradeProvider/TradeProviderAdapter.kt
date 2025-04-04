package io.novafoundation.nova.feature_assets.presentation.tradeProvider

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import io.novafoundation.nova.common.list.GroupedListHolder
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.feature_assets.R
import kotlinx.android.synthetic.main.item_trade_provider.view.itemTradeProviderDescription
import kotlinx.android.synthetic.main.item_trade_provider.view.itemTradeProviderLogo
import kotlinx.android.synthetic.main.item_trade_provider.view.itemTradeProviderPaymentMethods
import kotlinx.android.synthetic.main.layout_payment_method_image.view.paymentMethodImage
import kotlinx.android.synthetic.main.layout_payment_method_text.view.paymentMethodText

class TradeProviderAdapter(
    private val itemHandler: ItemHandler,
) : ListAdapter<TradeProviderRvItem, TradeProviderViewHolder>(DiffCallback) {

    interface ItemHandler {
        fun providerClicked(item: TradeProviderRvItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TradeProviderViewHolder {
        return TradeProviderViewHolder(parent.inflateChild(R.layout.item_trade_provider), itemHandler)
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
    containerView: View,
    private val itemHandler: TradeProviderAdapter.ItemHandler,
) : GroupedListHolder(containerView) {

    fun bind(item: TradeProviderRvItem) = with(containerView) {
        containerView.setOnClickListener { itemHandler.providerClicked(item) }

        itemTradeProviderLogo.setImageResource(item.providerLogoRes)
        itemTradeProviderDescription.text = item.description

        fillPaymentMethods(item)
    }

    private fun fillPaymentMethods(item: TradeProviderRvItem) = with(containerView) {
        itemTradeProviderPaymentMethods.removeAllViews()

        item.paymentMethods.forEach {
            val view = when (it) {
                is TradeProviderRvItem.PaymentMethod.ByResId -> createImage(it.resId)
                is TradeProviderRvItem.PaymentMethod.ByText -> createText(it.text)
            }

            itemTradeProviderPaymentMethods.addView(view)
        }
    }

    private fun createImage(resId: Int): View {
        return containerView.itemTradeProviderPaymentMethods.inflateChild(R.layout.layout_payment_method_image, false)
            .apply {
                paymentMethodImage.setImageResource(resId)
            }
    }

    private fun createText(text: String): View {
        return containerView.itemTradeProviderPaymentMethods.inflateChild(R.layout.layout_payment_method_text, false)
            .apply {
                paymentMethodText.text = text
            }
    }
}
