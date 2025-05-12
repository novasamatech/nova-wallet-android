package io.novafoundation.nova.feature_pay_impl.presentation.shop.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.list.SingleItemAdapter
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.feature_pay_impl.databinding.ItemBrandsSearchBinding

class ShopSearchAdapter(private val handler: Handler) : SingleItemAdapter<ShopSearchViewHolder>() {

    interface Handler {
        fun onSearchClick()
    }

    override fun onBindViewHolder(holder: ShopSearchViewHolder, position: Int) {
        holder.bind()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ShopSearchViewHolder(
        ItemBrandsSearchBinding.inflate(parent.inflater(), parent, false),
        handler
    )
}


class ShopSearchViewHolder(
    private val binder: ItemBrandsSearchBinding,
    private val listener: ShopSearchAdapter.Handler
) : RecyclerView.ViewHolder(binder.root) {

    fun bind() {
        binder.shopSearchIcon.setOnClickListener { listener.onSearchClick() }
    }
}



