package io.novafoundation.nova.feature_pay_impl.presentation.shop.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.list.SingleItemAdapter
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.feature_pay_impl.databinding.ItemPopularBrandBinding
import io.novafoundation.nova.feature_pay_impl.databinding.ItemPopularBrandsBinding
import io.novafoundation.nova.feature_pay_impl.presentation.shop.adapter.items.ShopBrandRVItem
import io.novafoundation.nova.feature_pay_impl.presentation.shop.adapter.items.ShopPopularBrandRVItem

class ShopPopularBrandsAdapter(private val handler: Handler) : SingleItemAdapter<ShopPopularBrandsViewHolder>() {

    interface Handler {

        fun onPopularBrandClick(brandModel: ShopBrandRVItem)
    }

    private val popularBrands = mutableListOf<ShopPopularBrandRVItem>()

    fun setPopularBrands(brands: List<ShopPopularBrandRVItem>) {
        popularBrands.clear()
        popularBrands.addAll(brands)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        if (!showItem) return 0

        return if (popularBrands.isNotEmpty()) 1 else 0
    }

    override fun onBindViewHolder(holder: ShopPopularBrandsViewHolder, position: Int) {
        holder.bind(popularBrands)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ShopPopularBrandsViewHolder(
        ItemPopularBrandsBinding.inflate(parent.inflater(), parent, false),
        handler
    )
}


class ShopPopularBrandsViewHolder(binding: ItemPopularBrandsBinding, private val handler: ShopPopularBrandsAdapter.Handler) :
    RecyclerView.ViewHolder(binding.root), ShopPopularBrandAdapter.Handler {

    private val adapter = ShopPopularBrandAdapter(this)

    init {
        val recyclerView = binding.brandsRecyclerView
        LinearSnapHelper().attachToRecyclerView(recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(recyclerView.context, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.adapter = adapter
    }

    fun bind(brands: List<ShopPopularBrandRVItem>) {
        adapter.submitList(brands)
    }

    override fun onRaiseBrandClicked(brandModel: ShopBrandRVItem) {
        handler.onPopularBrandClick(brandModel)
    }
}

class ShopPopularBrandAdapter(private val handler: Handler) : ListAdapter<ShopPopularBrandRVItem, ShopPopularBrandViewHolder>(RaisePopularBrandCallback()) {

    interface Handler {

        fun onRaiseBrandClicked(brandModel: ShopBrandRVItem)
    }

    override fun onBindViewHolder(holder: ShopPopularBrandViewHolder, position: Int) {
        holder.bindItem(getItem(position))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShopPopularBrandViewHolder {
        return ShopPopularBrandViewHolder(
            ItemPopularBrandBinding.inflate(parent.inflater(), parent, false),
            handler
        )
    }
}

class ShopPopularBrandViewHolder(private val binding: ItemPopularBrandBinding, private val handler: ShopPopularBrandAdapter.Handler) :
    RecyclerView.ViewHolder(binding.root) {

    fun bindItem(item: ShopPopularBrandRVItem) {
        with(binding) {
            item.imageRes?.let(root::setBackgroundResource)
            root.setOnClickListener { handler.onRaiseBrandClicked(item.raiseBrand) }

            brandCashback.text = item.raiseBrand.cashbackFormatted

            brandTitle.text = item.name
            brandTitle.setTextColor(item.textColor)

            brandCashback.setTextColor(item.textColor)
        }
    }
}

private class RaisePopularBrandCallback : DiffUtil.ItemCallback<ShopPopularBrandRVItem>() {

    override fun areItemsTheSame(oldItem: ShopPopularBrandRVItem, newItem: ShopPopularBrandRVItem): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: ShopPopularBrandRVItem, newItem: ShopPopularBrandRVItem): Boolean {
        return oldItem == newItem
    }
}

