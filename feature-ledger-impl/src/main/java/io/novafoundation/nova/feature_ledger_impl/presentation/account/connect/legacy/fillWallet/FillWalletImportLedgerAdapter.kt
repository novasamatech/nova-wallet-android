package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.fillWallet

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import coil.ImageLoader
import coil.clear
import io.novafoundation.nova.common.list.BaseListAdapter
import io.novafoundation.nova.common.list.BaseViewHolder
import io.novafoundation.nova.common.list.PayloadGenerator
import io.novafoundation.nova.common.list.resolvePayload
import io.novafoundation.nova.common.utils.getAccentColor
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.setImageTint
import io.novafoundation.nova.common.utils.setImageTintRes
import io.novafoundation.nova.feature_account_api.presenatation.chain.loadChainIcon
import io.novafoundation.nova.feature_account_api.view.ItemChainAccount
import io.novafoundation.nova.feature_ledger_impl.R
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.fillWallet.model.FillableChainAccountModel

class FillWalletImportLedgerAdapter(
    private val handler: Handler,
    private val imageLoader: ImageLoader
) : BaseListAdapter<FillableChainAccountModel, FillWalletViewHolder>(DiffCallback()) {

    interface Handler {

        fun onItemClicked(item: FillableChainAccountModel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FillWalletViewHolder {
        val view = ItemChainAccount(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        return FillWalletViewHolder(view, imageLoader, handler)
    }

    override fun onBindViewHolder(holder: FillWalletViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(holder: FillWalletViewHolder, position: Int, payloads: MutableList<Any>) {
        val item = getItem(position)

        resolvePayload(holder, position, payloads) {
            when (it) {
                addressExtractor -> holder.bindAddress(item)
            }
        }
    }
}

private val addressExtractor = { item: FillableChainAccountModel -> item.filledAddressModel?.address }

private val AssetPayloadGenerator = PayloadGenerator(addressExtractor)

private class DiffCallback : DiffUtil.ItemCallback<FillableChainAccountModel>() {
    override fun areItemsTheSame(oldItem: FillableChainAccountModel, newItem: FillableChainAccountModel): Boolean {
        return oldItem.chainUi.id == newItem.chainUi.id
    }

    override fun areContentsTheSame(oldItem: FillableChainAccountModel, newItem: FillableChainAccountModel): Boolean {
        return oldItem.filledAddressModel?.address == newItem.filledAddressModel?.address
    }

    override fun getChangePayload(oldItem: FillableChainAccountModel, newItem: FillableChainAccountModel): Any? {
        return AssetPayloadGenerator.diff(oldItem, newItem)
    }
}

class FillWalletViewHolder(
    override val containerView: ItemChainAccount,
    private val imageLoader: ImageLoader,
    private val eventHandler: FillWalletImportLedgerAdapter.Handler
) : BaseViewHolder(containerView) {

    fun bind(
        item: FillableChainAccountModel,
    ) = with(containerView) {
        chainIcon.loadChainIcon(item.chainUi.icon, imageLoader)
        chainName.text = item.chainUi.name

        bindAddress(item)
    }

    fun bindAddress(
        item: FillableChainAccountModel,
    ) = with(containerView) {
        if (item.filledAddressModel != null) {
            accountIcon.makeVisible()
            accountAddress.makeVisible()
            action.setImageResource(R.drawable.ic_checkmark_circle_16)
            action.setImageTintRes(R.color.icon_positive)

            accountIcon.setImageDrawable(item.filledAddressModel.image)
            accountAddress.text = item.filledAddressModel.nameOrAddress

            background = null
            setOnClickListener(null)
        } else {
            accountIcon.makeGone()
            accountAddress.makeGone()

            action.setImageResource(R.drawable.ic_add_circle)
            action.setImageTint(context.getAccentColor())

            setBackgroundResource(R.drawable.bg_primary_list_item)
            setOnClickListener { eventHandler.onItemClicked(item) }
        }
    }

    override fun unbind() {
        containerView.chainIcon.clear()
    }
}
