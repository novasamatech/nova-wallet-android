package io.novafoundation.nova.feature_assets.presentation.manageTokens

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import coil.ImageLoader
import coil.clear
import io.novafoundation.nova.common.list.BaseListAdapter
import io.novafoundation.nova.common.list.BaseViewHolder
import io.novafoundation.nova.common.list.PayloadGenerator
import io.novafoundation.nova.common.list.resolvePayload
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.utils.setImageTintRes
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.feature_account_api.presenatation.chain.loadTokenIcon
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.presentation.manageTokens.model.MultiChainTokenModel
import kotlinx.android.synthetic.main.item_manage_token_multichain.view.itemManageTokenMultichainEnabled
import kotlinx.android.synthetic.main.item_manage_token_multichain.view.itemManageTokenMultichainIcon
import kotlinx.android.synthetic.main.item_manage_token_multichain.view.itemManageTokenMultichainNetworks
import kotlinx.android.synthetic.main.item_manage_token_multichain.view.itemManageTokenMultichainSymbol
import kotlinx.android.synthetic.main.item_manage_token_multichain.view.itemManageTokensEdit

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
            containerView = parent.inflateChild(R.layout.item_manage_token_multichain),
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
                MultiChainTokenModel::networks -> holder.bindNetworks(item)
            }
        }
    }
}

class ManageTokensViewHolder(
    containerView: View,
    private val itemHandler: ManageTokensAdapter.ItemHandler,
    private val imageLoader: ImageLoader,
) : BaseViewHolder(containerView) {

    init {
        containerView.itemManageTokensEdit.setOnClickListener {
            itemHandler.editClocked(bindingAdapterPosition)
        }
    }

    fun bind(item: MultiChainTokenModel) = with(containerView) {
        bindNetworks(item)

        bindEnabled(item)

        itemManageTokenMultichainIcon.loadTokenIcon(item.icon, imageLoader)
        itemManageTokenMultichainSymbol.text = item.symbol
    }

    fun bindNetworks(item: MultiChainTokenModel) {
        containerView.itemManageTokenMultichainNetworks.text = item.networks
    }

    fun bindEnabled(item: MultiChainTokenModel) = with(containerView) {
        itemManageTokenMultichainEnabled.setOnCheckedChangeListener(null)
        itemManageTokenMultichainEnabled.isChecked = item.enabled
        itemManageTokenMultichainEnabled.setOnCheckedChangeListener { _, _ ->
            itemHandler.enableSwitched(bindingAdapterPosition)
        }

        val contentColorRes = if (item.enabled) R.color.white else R.color.white_48
        itemManageTokenMultichainIcon.setImageTintRes(contentColorRes)
        itemManageTokenMultichainSymbol.setTextColorRes(contentColorRes)
    }

    override fun unbind() {
        containerView.itemManageTokenMultichainIcon.clear()
    }
}

private class DiffCallback : DiffUtil.ItemCallback<MultiChainTokenModel>() {

    private val payloadGenerator = PayloadGenerator(MultiChainTokenModel::enabled, MultiChainTokenModel::networks)

    override fun areItemsTheSame(oldItem: MultiChainTokenModel, newItem: MultiChainTokenModel): Boolean {
        return oldItem.symbol == newItem.symbol
    }

    override fun areContentsTheSame(oldItem: MultiChainTokenModel, newItem: MultiChainTokenModel): Boolean {
        return oldItem == newItem
    }

    override fun getChangePayload(oldItem: MultiChainTokenModel, newItem: MultiChainTokenModel): Any? {
        return payloadGenerator.diff(oldItem, newItem)
    }
}
