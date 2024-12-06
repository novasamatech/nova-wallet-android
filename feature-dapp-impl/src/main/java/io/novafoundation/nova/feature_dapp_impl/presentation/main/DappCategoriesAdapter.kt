package io.novafoundation.nova.feature_dapp_impl.presentation.main

import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import io.novafoundation.nova.common.list.PayloadGenerator
import io.novafoundation.nova.common.list.resolvePayload
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.utils.loadOrHide
import io.novafoundation.nova.feature_dapp_impl.R
import io.novafoundation.nova.feature_dapp_impl.presentation.main.model.DAppCategoryModel
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_dapp_category.view.itemDappCategoryIcon
import kotlinx.android.synthetic.main.item_dapp_category.view.itemDappCategoryText

class DappCategoriesAdapter(
    private val imageLoader: ImageLoader,
    private val handler: Handler,
) : ListAdapter<DAppCategoryModel, DappCategoryViewHolder>(DappDiffCallback) {

    interface Handler {

        fun onCategoryClicked(id: String)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DappCategoryViewHolder {
        return DappCategoryViewHolder(parent.inflateChild(R.layout.item_dapp_category), imageLoader, handler)
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
    override val containerView: View,
    private val imageLoader: ImageLoader,
    private val itemHandler: DappCategoriesAdapter.Handler,
) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    fun bind(item: DAppCategoryModel) = with(containerView) {
        itemDappCategoryIcon.loadOrHide(item.iconUrl, imageLoader)
        itemDappCategoryText.text = item.name

        bindSelected(item.selected)

        containerView.setOnClickListener { itemHandler.onCategoryClicked(item.id) }
    }

    fun bindSelected(isSelected: Boolean) {
        itemView.isSelected = isSelected

        // We must set tint to image view programmatically since we can't specify the state for default color
        if (isSelected) {
            itemView.itemDappCategoryIcon.setColorFilter(ContextCompat.getColor(itemView.context, R.color.icon_primary_on_content))
        } else {
            itemView.itemDappCategoryIcon.clearColorFilter()
        }
    }
}
