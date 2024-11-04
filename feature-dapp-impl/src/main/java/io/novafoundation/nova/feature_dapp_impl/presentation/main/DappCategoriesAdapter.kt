package io.novafoundation.nova.feature_dapp_impl.presentation.main

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.list.PayloadGenerator
import io.novafoundation.nova.common.list.resolvePayload
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.feature_dapp_impl.databinding.ItemDappCategoryBinding
import io.novafoundation.nova.feature_dapp_impl.presentation.main.model.DAppCategoryModel

class DappCategoriesAdapter(
    private val handler: Handler,
) : ListAdapter<DAppCategoryModel, DappCategoryViewHolder>(DappDiffCallback) {

    interface Handler {

        fun onCategoryClicked(id: String)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DappCategoryViewHolder {
        return DappCategoryViewHolder(ItemDappCategoryBinding.inflate(parent.inflater(), parent, false), handler)
    }

    override fun onBindViewHolder(holder: DappCategoryViewHolder, position: Int, payloads: MutableList<Any>) {
        val item = getItem(position)

        resolvePayload(holder, position, payloads) {
            when (it) {
                DAppCategoryModel::selected -> holder.bindSelected(item.selected)
            }
        }
    }

    override fun onBindViewHolder(holder: DappCategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

private val dAppCategoryPayloadGenerator = PayloadGenerator(DAppCategoryModel::selected)

private object DappDiffCallback : DiffUtil.ItemCallback<DAppCategoryModel>() {

    override fun areItemsTheSame(oldItem: DAppCategoryModel, newItem: DAppCategoryModel): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: DAppCategoryModel, newItem: DAppCategoryModel): Boolean {
        return oldItem == newItem
    }

    override fun getChangePayload(oldItem: DAppCategoryModel, newItem: DAppCategoryModel): Any? {
        return dAppCategoryPayloadGenerator.diff(oldItem, newItem)
    }
}

class DappCategoryViewHolder(
    private val binder: ItemDappCategoryBinding,
    private val itemHandler: DappCategoriesAdapter.Handler,
) : RecyclerView.ViewHolder(binder.root) {

    fun bind(item: DAppCategoryModel) = with(binder) {
        itemDappCategory.text = item.name

        bindSelected(item.selected)

        binder.root.setOnClickListener { itemHandler.onCategoryClicked(item.id) }
    }

    fun bindSelected(isSelected: Boolean) = with(binder) {
        itemDappCategory.isSelected = isSelected
    }
}
