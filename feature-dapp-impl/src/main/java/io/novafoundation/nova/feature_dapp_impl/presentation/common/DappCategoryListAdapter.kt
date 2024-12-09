package io.novafoundation.nova.feature_dapp_impl.presentation.common

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import io.novafoundation.nova.common.list.BaseListAdapter
import io.novafoundation.nova.common.list.BaseViewHolder
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.feature_dapp_impl.R
import kotlinx.android.synthetic.main.item_dapp_group.view.dappRecyclerView
import kotlinx.android.synthetic.main.item_dapp_group.view.itemDAppCategoryTitle

class DappCategoryListAdapter(
    private val handler: DAppClickHandler
) : BaseListAdapter<DappCategoryModel, DappCategoryViewHolder>(DappCategoryDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DappCategoryViewHolder {
        return DappCategoryViewHolder(parent.inflateChild(R.layout.item_dapp_group), handler)
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
    view: View,
    itemHandler: DAppClickHandler,
) : BaseViewHolder(view) {

    private val adapter = DappListAdapter(itemHandler)

    init {
        itemView.dappRecyclerView.layoutManager = GridLayoutManager(itemView.context, 3, GridLayoutManager.HORIZONTAL, false)
        itemView.dappRecyclerView.adapter = adapter
        itemView.dappRecyclerView.itemAnimator = null
        val snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(itemView.dappRecyclerView)
    }

    fun bind(item: DappCategoryModel) = with(itemView) {
        itemDAppCategoryTitle.text = item.categoryName
        adapter.submitList(item.items)
    }

    override fun unbind() {}
}
