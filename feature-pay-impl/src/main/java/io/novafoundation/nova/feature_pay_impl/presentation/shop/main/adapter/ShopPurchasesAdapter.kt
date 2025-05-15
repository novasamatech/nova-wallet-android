package io.novafoundation.nova.feature_pay_impl.presentation.shop.main.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.list.SingleItemAdapter
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawableWithRipple
import io.novafoundation.nova.feature_pay_impl.R
import io.novafoundation.nova.feature_pay_impl.databinding.ItemBrandsPurchasesBinding

class ShopPurchasesAdapter(private val handler: Handler) : SingleItemAdapter<ShopPurchasesViewHolder>(isShownByDefault = false) {

    interface Handler {
        fun onPurchasesClick()
    }

    private var quantity: Int? = null

    fun setPurchasesQuantity(quantity: Int) {
        this.quantity = quantity
        notifyChangedIfShown()
    }

    override fun onBindViewHolder(holder: ShopPurchasesViewHolder, position: Int) {
        holder.bind(quantity)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ShopPurchasesViewHolder(
        ItemBrandsPurchasesBinding.inflate(parent.inflater(), parent, false),
        handler
    )
}

class ShopPurchasesViewHolder(
    private val binder: ItemBrandsPurchasesBinding,
    private val listener: ShopPurchasesAdapter.Handler
) : RecyclerView.ViewHolder(binder.root) {

    init {
        binder.root.background = itemView.context.getRoundedCornerDrawableWithRipple(fillColorRes = R.color.block_background, cornerSizeInDp = 10)
    }

    fun bind(quantity: Int?) {
        binder.root.setOnClickListener { listener.onPurchasesClick() }
        binder.shopPurchasesQuantity.text = quantity?.format()
    }
}
