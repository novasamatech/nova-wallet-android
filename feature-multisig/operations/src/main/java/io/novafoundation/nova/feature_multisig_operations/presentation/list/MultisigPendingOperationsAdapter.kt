package io.novafoundation.nova.feature_multisig_operations.presentation.list

import android.view.ViewGroup
import coil.ImageLoader
import io.novafoundation.nova.common.list.BaseGroupedDiffCallback
import io.novafoundation.nova.common.list.GroupedListAdapter
import io.novafoundation.nova.common.list.GroupedListHolder
import io.novafoundation.nova.common.presentation.setColoredText
import io.novafoundation.nova.common.utils.formatting.formatDaysSinceEpoch
import io.novafoundation.nova.common.utils.images.setIcon
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.letOrHide
import io.novafoundation.nova.common.utils.setDrawableEnd
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.view.shape.addRipple
import io.novafoundation.nova.common.view.shape.getBottomRoundedCornerDrawable
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.feature_account_api.presenatation.chain.loadChainIcon
import io.novafoundation.nova.feature_multisig_operations.R
import io.novafoundation.nova.feature_multisig_operations.databinding.ItemMultisigPendingOperationBinding
import io.novafoundation.nova.feature_multisig_operations.databinding.ItemMultisigPendingOperationHeaderBinding
import io.novafoundation.nova.feature_multisig_operations.presentation.list.model.PendingMultisigOperationHeaderModel
import io.novafoundation.nova.feature_multisig_operations.presentation.list.model.PendingMultisigOperationModel

class MultisigPendingOperationsAdapter(
    private val handler: ItemHandler,
    private val imageLoader: ImageLoader,
) : GroupedListAdapter<PendingMultisigOperationHeaderModel, PendingMultisigOperationModel>(PendingMultisigOperationDiffCallback()) {

    interface ItemHandler {

        fun itemClicked(model: PendingMultisigOperationModel)
    }

    override fun createGroupViewHolder(parent: ViewGroup): GroupedListHolder {
        return MultisigPendingOperationHeaderHolder(
            viewBinding = ItemMultisigPendingOperationHeaderBinding.inflate(parent.inflater(), parent, false)
        )
    }

    override fun createChildViewHolder(parent: ViewGroup): GroupedListHolder {
       return MultisigPendingOperationHolder(
           viewBinding = ItemMultisigPendingOperationBinding.inflate(parent.inflater(), parent, false),
           itemHandler = handler,
           imageLoader = imageLoader
       )
    }

    override fun bindChild(holder: GroupedListHolder, child: PendingMultisigOperationModel) {
        (holder as MultisigPendingOperationHolder).bind(child)
    }

    override fun bindGroup(holder: GroupedListHolder, group: PendingMultisigOperationHeaderModel) {
        (holder as MultisigPendingOperationHeaderHolder).bind(group)
    }
}

class MultisigPendingOperationHeaderHolder(
    private val viewBinding: ItemMultisigPendingOperationHeaderBinding,
): GroupedListHolder(viewBinding.root) {

    fun bind(model: PendingMultisigOperationHeaderModel) {
        viewBinding.itemMultisigPendingOperationHeader.text = model.daysSinceEpoch.formatDaysSinceEpoch(viewBinding.root.context)
    }
}

class MultisigPendingOperationHolder(
    private val viewBinding: ItemMultisigPendingOperationBinding,
    private val itemHandler: MultisigPendingOperationsAdapter.ItemHandler,
    private val imageLoader: ImageLoader,
) : GroupedListHolder(viewBinding.root) {

    init {
        with(viewBinding.root) {
            background = context.addRipple(context.getRoundedCornerDrawable(R.color.block_background))
        }

        with(viewBinding.itemPendingOperationOnBehalfOfContainer) {
            background = context.getBottomRoundedCornerDrawable(R.color.block_background)
        }
    }

    fun bind(model: PendingMultisigOperationModel) = with(viewBinding) {
        itemPendingOperationTitle.text = model.call.title
        itemPendingOperationSubtitle.setTextOrHide(model.call.subtitle)

        itemPendingOperationChain.loadChainIcon(model.chain.icon, imageLoader)
        itemPendingOperationIcon.setIcon(model.call.icon, imageLoader)

        itemPendingOperationProgress.text = model.progress
        itemPendingOperationAction.letOrHide(model.action) { action ->
            itemPendingOperationAction.setColoredText(action.text)
            itemPendingOperationAction.setDrawableEnd(action.icon, widthInDp = 16, paddingInDp = 4)
        }

        itemPendingOperationPrimaryValue.setTextOrHide(model.call.primaryValue)
        itemPendingOperationTime.setTextOrHide(model.time)

        itemPendingOperationOnBehalfOfContainer.letOrHide(model.call.onBehalfOf) { onBehalfOf ->
            itemPendingOperationOnBehalfOfAddress.text = onBehalfOf.nameOrAddress
            itemPendingOperationOnBehalfOfIcon.setImageDrawable(onBehalfOf.image)
        }

        root.setOnClickListener { itemHandler.itemClicked(model) }
    }
}

private class PendingMultisigOperationDiffCallback : BaseGroupedDiffCallback<PendingMultisigOperationHeaderModel, PendingMultisigOperationModel>(
    PendingMultisigOperationHeaderModel::class.java
) {

    override fun areGroupItemsTheSame(oldItem: PendingMultisigOperationHeaderModel, newItem: PendingMultisigOperationHeaderModel): Boolean {
        return oldItem.daysSinceEpoch == newItem.daysSinceEpoch
    }

    override fun areGroupContentsTheSame(oldItem: PendingMultisigOperationHeaderModel, newItem: PendingMultisigOperationHeaderModel): Boolean {
        return true
    }

    override fun areChildItemsTheSame(oldItem: PendingMultisigOperationModel, newItem: PendingMultisigOperationModel): Boolean {
        return oldItem.id == newItem.id

    }

    override fun areChildContentsTheSame(oldItem: PendingMultisigOperationModel, newItem: PendingMultisigOperationModel): Boolean {
        return oldItem == newItem
    }
}
