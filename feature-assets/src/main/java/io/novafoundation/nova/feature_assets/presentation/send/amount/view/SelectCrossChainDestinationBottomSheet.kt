package io.novafoundation.nova.feature_assets.presentation.send.amount.view

import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import io.novafoundation.nova.common.list.BaseGroupedDiffCallback
import io.novafoundation.nova.common.list.GroupedListAdapter
import io.novafoundation.nova.common.list.GroupedListHolder
import io.novafoundation.nova.common.list.headers.TextHeader
import io.novafoundation.nova.common.list.toListWithHeaders
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.BaseDynamicListBottomSheet
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.ClickHandler
import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainUi
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.databinding.ItemChainChooserBinding
import io.novafoundation.nova.feature_assets.databinding.ItemChainChooserGroupBinding
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import io.novafoundation.nova.runtime.multiNetwork.ChainWithAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class SelectCrossChainDestinationBottomSheet(
    context: Context,
    private val payload: Payload,
    private val onSelected: ClickHandler<ChainWithAsset>,
    private val onCancelled: () -> Unit
) : BaseDynamicListBottomSheet(context), CrossChainDestinationAdapter.Handler {

    class Payload(
        val destinations: Map<TextHeader, List<CrossChainDestinationModel>>,
        val selectedChain: Chain
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.wallet_send_recipient_network)

        setOnCancelListener { onCancelled() }

        val adapter = CrossChainDestinationAdapter(this, payload.selectedChain)
        recyclerView.adapter = adapter

        adapter.submitList(payload.destinations.toListWithHeaders())
    }

    override fun itemClicked(chainWithAsset: ChainWithAsset) {
        onSelected(this, chainWithAsset)
        dismiss()
    }
}

class CrossChainDestinationModel(
    val chainWithAsset: ChainWithAsset,
    val chainUi: ChainUi,
    val balance: AmountModel?
)

class CrossChainDestinationAdapter(
    private val handler: Handler,
    private val selectedChain: Chain,
) : GroupedListAdapter<TextHeader, CrossChainDestinationModel>(DiffCallback()) {

    interface Handler {

        fun itemClicked(chainWithAsset: ChainWithAsset)
    }

    override fun createGroupViewHolder(parent: ViewGroup): GroupedListHolder {
        return GroupHolder(ItemChainChooserGroupBinding.inflate(parent.inflater(), parent, false))
    }

    override fun createChildViewHolder(parent: ViewGroup): GroupedListHolder {
        return ItemHolder(ItemChainChooserBinding.inflate(parent.inflater(), parent, false))
    }

    override fun bindGroup(holder: GroupedListHolder, group: TextHeader) {
        (holder as GroupHolder).bind(group)
    }

    override fun bindChild(holder: GroupedListHolder, child: CrossChainDestinationModel) {
        val isSelected = child.chainWithAsset.chain.id == selectedChain.id

        (holder as ItemHolder).bind(child, isSelected, handler)
    }
}

private class GroupHolder(private val binder: ItemChainChooserGroupBinding) : GroupedListHolder(binder.root) {

    fun bind(item: TextHeader) {
        binder.itemChainChooserGroup.text = item.content
    }
}

private class ItemHolder(private val binder: ItemChainChooserBinding) : GroupedListHolder(binder.root) {

    fun bind(
        item: CrossChainDestinationModel,
        isSelected: Boolean,
        handler: CrossChainDestinationAdapter.Handler
    ) = with(binder) {
        itemChainChooserChain.setChain(item.chainUi)
        itemChainChooserCheck.isChecked = isSelected

        itemChainChooserAmountToken.setTextOrHide(item.balance?.token)
        itemChainChooserAmountFiat.setTextOrHide(item.balance?.fiat)

        binder.root.setOnClickListener { handler.itemClicked(item.chainWithAsset) }
    }
}

private class DiffCallback : BaseGroupedDiffCallback<TextHeader, CrossChainDestinationModel>(TextHeader::class.java) {

    override fun areGroupItemsTheSame(oldItem: TextHeader, newItem: TextHeader): Boolean {
        return TextHeader.DIFF_CALLBACK.areItemsTheSame(oldItem, newItem)
    }

    override fun areGroupContentsTheSame(oldItem: TextHeader, newItem: TextHeader): Boolean {
        return TextHeader.DIFF_CALLBACK.areContentsTheSame(oldItem, newItem)
    }

    override fun areChildItemsTheSame(oldItem: CrossChainDestinationModel, newItem: CrossChainDestinationModel): Boolean {
        return oldItem.chainUi.id == newItem.chainUi.id
    }

    override fun areChildContentsTheSame(oldItem: CrossChainDestinationModel, newItem: CrossChainDestinationModel): Boolean {
        return oldItem.chainUi == newItem.chainUi
    }
}
