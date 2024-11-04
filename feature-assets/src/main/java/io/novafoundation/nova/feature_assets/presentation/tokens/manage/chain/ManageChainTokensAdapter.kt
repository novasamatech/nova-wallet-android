package io.novafoundation.nova.feature_assets.presentation.tokens.manage.chain

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import coil.ImageLoader
import coil.clear
import io.novafoundation.nova.common.list.BaseListAdapter
import io.novafoundation.nova.common.list.BaseViewHolder
import io.novafoundation.nova.common.list.PayloadGenerator
import io.novafoundation.nova.common.list.resolvePayload
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.feature_account_api.presenatation.chain.loadChainIcon
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.databinding.ItemManageChainTokenBinding
import io.novafoundation.nova.feature_assets.presentation.tokens.manage.chain.model.ChainTokenInstanceModel

class ManageChainTokensAdapter(
    private val imageLoader: ImageLoader,
    private val handler: ItemHandler
) : BaseListAdapter<ChainTokenInstanceModel, ManageChainTokensViewHolder>(DiffCallback()) {

    interface ItemHandler {

        fun enableSwitched(position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ManageChainTokensViewHolder {
        return ManageChainTokensViewHolder(
            binder = ItemManageChainTokenBinding.inflate(parent.inflater(), parent, false),
            itemHandler = handler,
            imageLoader = imageLoader
        )
    }

    override fun onBindViewHolder(holder: ManageChainTokensViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(holder: ManageChainTokensViewHolder, position: Int, payloads: MutableList<Any>) {
        val item = getItem(position)
        resolvePayload(holder, position, payloads) {
            when (it) {
                ChainTokenInstanceModel::enabled -> holder.bindEnabled(item)
            }
        }
    }
}

class ManageChainTokensViewHolder(
    private val binder: ItemManageChainTokenBinding,
    private val itemHandler: ManageChainTokensAdapter.ItemHandler,
    private val imageLoader: ImageLoader,
) : BaseViewHolder(binder.root) {

    init {
        with(binder) {
            itemManageChainTokenEnabled.setOnClickListener { itemHandler.enableSwitched(bindingAdapterPosition) }
        }
    }

    fun bind(item: ChainTokenInstanceModel) = with(binder) {
        bindEnabled(item)
        itemManageChainTokenChainIcon.loadChainIcon(item.chainUi.icon, imageLoader)
        itemManageChainTokenChainName.text = item.chainUi.name
    }

    fun bindEnabled(item: ChainTokenInstanceModel) {
        with(binder.itemManageChainTokenEnabled) {
            isChecked = item.enabled
            isEnabled = item.switchable
        }

        with(binder) {
            val contentColorRes = if (item.enabled) R.color.text_primary else R.color.text_secondary
            itemManageChainTokenChainName.setTextColorRes(contentColorRes)
        }
    }

    override fun unbind() {
        binder.itemManageChainTokenChainIcon.clear()
    }
}

private class DiffCallback : DiffUtil.ItemCallback<ChainTokenInstanceModel>() {

    private val payloadGenerator = PayloadGenerator(ChainTokenInstanceModel::enabled)

    override fun areItemsTheSame(oldItem: ChainTokenInstanceModel, newItem: ChainTokenInstanceModel): Boolean {
        return oldItem.chainUi.id == newItem.chainUi.id
    }

    override fun areContentsTheSame(oldItem: ChainTokenInstanceModel, newItem: ChainTokenInstanceModel): Boolean {
        return oldItem == newItem
    }

    override fun getChangePayload(oldItem: ChainTokenInstanceModel, newItem: ChainTokenInstanceModel): Any? {
        return payloadGenerator.diff(oldItem, newItem)
    }
}
