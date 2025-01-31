package io.novafoundation.nova.feature_dapp_impl.presentation.common

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import io.novafoundation.nova.common.list.BaseListAdapter
import io.novafoundation.nova.common.list.BaseViewHolder
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.feature_dapp_impl.databinding.ItemDappGroupBinding

class DappCategoryListAdapter(
    private val handler: DAppClickHandler
) : BaseListAdapter<DappCategoryModel, DappCategoryViewHolder>(DappCategoryDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DappCategoryViewHolder {
        return DappCategoryViewHolder(ItemDappGroupBinding.inflate(parent.inflater(), parent, false), handler)
    }

    override fun onBindViewHolder(holder: DappCategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

private object DappCategoryDiffCallback : DiffUtil.ItemCallback<DappCategoryModel>() {

    override fun areItemsTheSame(oldItem: DappCategoryModel, newItem: DappCategoryModel): Boolean {
        return oldItem.categoryName == newItem.categoryName
    }

    override fun areContentsTheSame(oldItem: DappCategoryModel, newItem: DappCategoryModel): Boolean {
        return oldItem == newItem
    }
}

class DappCategoryViewHolder(
    private val binder: ItemDappGroupBinding,
    itemHandler: DAppClickHandler,
) : BaseViewHolder(binder.root) {

    private val adapter = DappListAdapter(itemHandler)

    init {
        binder.dappRecyclerView.layoutManager = GridLayoutManager(itemView.context, 3, GridLayoutManager.HORIZONTAL, false)
        binder.dappRecyclerView.adapter = adapter
        binder.dappRecyclerView.itemAnimator = null
        val snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(binder.dappRecyclerView)
    }

    fun bind(item: DappCategoryModel) = with(binder) {
        itemDAppCategoryTitle.text = item.categoryName
        adapter.submitList(item.items)
    }

    override fun unbind() {}
}
