package io.novafoundation.nova.feature_pay_impl.presentation.shop.common.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import coil.clear
import coil.load
import coil.transform.CircleCropTransformation
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawableWithRipple
import io.novafoundation.nova.feature_pay_impl.R
import io.novafoundation.nova.feature_pay_impl.databinding.ItemBrandBinding
import io.novafoundation.nova.feature_pay_impl.presentation.shop.main.adapter.items.ShopBrandRVItem

class ShopBrandsAdapter(
    private val imageLoader: ImageLoader,
    private val handler: Handler,
) : ListAdapter<ShopBrandRVItem, ShopPayBrandsHolder>(SendRecipientCallback()) {

    interface Handler {

        fun onBrandClick(brandModel: ShopBrandRVItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShopPayBrandsHolder {
        val binder = ItemBrandBinding.inflate(parent.inflater(), parent, false)
        return ShopPayBrandsHolder(binder, imageLoader, handler)
    }

    override fun onBindViewHolder(holder: ShopPayBrandsHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onViewRecycled(holder: ShopPayBrandsHolder) {
        holder.unbind()
    }
}

class ShopPayBrandsHolder(
    private val binder: ItemBrandBinding,
    private val imageLoader: ImageLoader,
    private val handler: ShopBrandsAdapter.Handler,
) : RecyclerView.ViewHolder(binder.root) {

    init {
        binder.itemBrandContainer.background = itemView.context.getRoundedCornerDrawableWithRipple(R.color.block_background)
    }

    fun bind(model: ShopBrandRVItem) = with(binder) {
        val cashbackVisible = model.cashback > 0
        itemBrandCashback.setVisible(cashbackVisible)
        itemBrandCashbackText.setVisible(cashbackVisible)
        itemBrandCashback.text = model.cashbackFormatted
        itemBrandName.text = model.name
        itemBrandIcon.load(model.iconUrl, imageLoader) {
            transformations(CircleCropTransformation())
        }

        root.setOnClickListener { handler.onBrandClick(model) }
    }

    fun unbind() {
        binder.itemBrandIcon.clear()
    }
}

private class SendRecipientCallback : DiffUtil.ItemCallback<ShopBrandRVItem>() {

    override fun areItemsTheSame(oldItem: ShopBrandRVItem, newItem: ShopBrandRVItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: ShopBrandRVItem, newItem: ShopBrandRVItem): Boolean {
        return oldItem == newItem
    }
}
