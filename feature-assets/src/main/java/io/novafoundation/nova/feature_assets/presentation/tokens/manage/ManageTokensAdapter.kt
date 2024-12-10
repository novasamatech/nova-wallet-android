package io.novafoundation.nova.feature_assets.presentation.tokens.manage

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import coil.ImageLoader
import coil.clear
import io.novafoundation.nova.common.list.BaseListAdapter
import io.novafoundation.nova.common.list.BaseViewHolder
import io.novafoundation.nova.common.list.PayloadGenerator
import io.novafoundation.nova.common.list.resolvePayload
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.images.setIcon
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.databinding.ItemManageTokenMultichainBinding
import io.novafoundation.nova.feature_assets.presentation.tokens.manage.model.MultiChainTokenModel

private val subtitleExtractor = { model: MultiChainTokenModel -> model.header.networks }

class ManageTokensAdapter(
    private val imageLoader: ImageLoader,
    private val handler: ItemHandler
) : BaseListAdapter<MultiChainTokenModel, ManageTokensViewHolder>(DiffCallback()) {

    interface ItemHandler {

        fun enableSwitched(position: Int)

        fun editClocked(position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ManageTokensViewHolder {
        return ManageTokensViewHolder(
            binder = ItemManageTokenMultichainBinding.inflate(parent.inflater(), parent, false),
            itemHandler = handler,
            imageLoader = imageLoader
        )
    }

    override fun onBindViewHolder(holder: ManageTokensViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(holder: ManageTokensViewHolder, position: Int, payloads: MutableList<Any>) {
        val item = getItem(position)
        resolvePayload(holder, position, payloads) {
            when (it) {
                MultiChainTokenModel::enabled -> holder.bindEnabled(item)
                subtitleExtractor -> holder.bindNetworks(item)
            }
        }
    }
}

class ManageTokensViewHolder(
    private val binder: ItemManageTokenMultichainBinding,
    private val itemHandler: ManageTokensAdapter.ItemHandler,
    private val imageLoader: ImageLoader,
) : BaseViewHolder(binder.root) {

    init {
        with(binder) {
            binder.root.setOnClickListener {
                itemHandler.editClocked(bindingAdapterPosition)
            }
            itemManageTokenMultichainEnabled.setOnClickListener {
                itemHandler.enableSwitched(bindingAdapterPosition)
            }
        }
    }

    fun bind(item: MultiChainTokenModel) = with(binder) {
        bindNetworks(item)

        bindEnabled(item)

        itemManageTokenMultichainIcon.setIcon(item.header.icon, imageLoader)
        itemManageTokenMultichainSymbol.text = item.header.symbol
    }

    fun bindNetworks(item: MultiChainTokenModel) {
        binder.itemManageTokenMultichainNetworks.text = item.header.networks
    }

    fun bindEnabled(item: MultiChainTokenModel) = with(binder) {
        itemManageTokenMultichainEnabled.isChecked = item.enabled
        itemManageTokenMultichainEnabled.isEnabled = item.switchable

        itemManageTokenMultichainIcon.alpha = if (item.enabled) 1f else 0.48f

        val contentColorRes = if (item.enabled) R.color.text_primary else R.color.text_secondary
        itemManageTokenMultichainSymbol.setTextColorRes(contentColorRes)
    }

    override fun unbind() {
        binder.itemManageTokenMultichainIcon.clear()
    }
}

private class DiffCallback : DiffUtil.ItemCallback<MultiChainTokenModel>() {

    private val payloadGenerator = PayloadGenerator(MultiChainTokenModel::enabled, subtitleExtractor)

    override fun areItemsTheSame(oldItem: MultiChainTokenModel, newItem: MultiChainTokenModel): Boolean {
        return oldItem.header.symbol == newItem.header.symbol
    }

    override fun areContentsTheSame(oldItem: MultiChainTokenModel, newItem: MultiChainTokenModel): Boolean {
        return oldItem == newItem
    }

    override fun getChangePayload(oldItem: MultiChainTokenModel, newItem: MultiChainTokenModel): Any? {
        return payloadGenerator.diff(oldItem, newItem)
    }
}
