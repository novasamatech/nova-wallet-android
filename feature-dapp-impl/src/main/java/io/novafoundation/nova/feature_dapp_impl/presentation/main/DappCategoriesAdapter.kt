package io.novafoundation.nova.feature_dapp_impl.presentation.main

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.feature_dapp_api.data.model.DappCategory
import io.novafoundation.nova.feature_dapp_impl.R
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_dapp_category.view.itemDappCategory

class DappCategoriesAdapter(
    private val handler: Handler,
) : ListAdapter<DappCategory, DappCategoryViewHolder>(DappDiffCallback) {

    private var selectedPosition = 0

    interface Handler {

        fun onItemClicked(position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DappCategoryViewHolder {
        return DappCategoryViewHolder(parent.inflateChild(R.layout.item_dapp_category), handler)
    }

    override fun onBindViewHolder(holder: DappCategoryViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
            return
        }

        payloads.onEach {
            when (it) {
                is SelectionChanged -> holder.bindSelected(isItemSelected(position))
            }
        }
    }

    override fun onBindViewHolder(holder: DappCategoryViewHolder, position: Int) {
        holder.bind(getItem(position), isItemSelected(position))
    }

    fun setSelectedCategory(newSelected: Int) {
        val previousSelected = selectedPosition
        selectedPosition = newSelected

        notifyItemChanged(previousSelected, SelectionChanged)
        notifyItemChanged(newSelected, SelectionChanged)
    }

    private fun isItemSelected(position: Int) = position == selectedPosition
}

private object DappDiffCallback : DiffUtil.ItemCallback<DappCategory>() {

    override fun areItemsTheSame(oldItem: DappCategory, newItem: DappCategory): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: DappCategory, newItem: DappCategory): Boolean {
        return oldItem == newItem
    }
}

private object SelectionChanged

class DappCategoryViewHolder(
    override val containerView: View,
    private val itemHandler: DappCategoriesAdapter.Handler,
) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    init {
        containerView.setOnClickListener { itemHandler.onItemClicked(adapterPosition) }
    }

    fun bind(
        item: DappCategory,
        selected: Boolean
    ) = with(containerView) {
        itemDappCategory.text = item.name

        bindSelected(selected)
    }

    fun bindSelected(isSelected: Boolean) = with(containerView) {
        itemDappCategory.isSelected = isSelected
    }
}
