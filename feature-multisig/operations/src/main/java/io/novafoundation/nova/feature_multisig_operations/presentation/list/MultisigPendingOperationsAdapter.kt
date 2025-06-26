package io.novafoundation.nova.feature_multisig_operations.presentation.list

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import coil.ImageLoader
import io.novafoundation.nova.common.list.BaseListAdapter
import io.novafoundation.nova.common.list.BaseViewHolder
import io.novafoundation.nova.common.presentation.setColoredText
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
import io.novafoundation.nova.feature_multisig_operations.presentation.list.model.PendingMultisigOperationModel

class MultisigPendingOperationsAdapter(
    private val handler: ItemHandler,
    private val imageLoader: ImageLoader,
) : BaseListAdapter<PendingMultisigOperationModel, MultisigPendingOperationHolder>(
    PendingMultisigOperationDiffCallback()
) {

    interface ItemHandler {

        fun itemClicked(model: PendingMultisigOperationModel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MultisigPendingOperationHolder {
        return MultisigPendingOperationHolder(
            viewBinding = ItemMultisigPendingOperationBinding.inflate(parent.inflater(), parent, false),
            itemHandler = handler,
            imageLoader = imageLoader
        )
    }

    override fun onBindViewHolder(holder: MultisigPendingOperationHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class MultisigPendingOperationHolder(
    private val viewBinding: ItemMultisigPendingOperationBinding,
    private val itemHandler: MultisigPendingOperationsAdapter.ItemHandler,
    private val imageLoader: ImageLoader,
) : BaseViewHolder(viewBinding.root) {

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

    override fun unbind() {}
}

private class PendingMultisigOperationDiffCallback : DiffUtil.ItemCallback<PendingMultisigOperationModel>() {

    override fun areItemsTheSame(oldItem: PendingMultisigOperationModel, newItem: PendingMultisigOperationModel): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: PendingMultisigOperationModel, newItem: PendingMultisigOperationModel): Boolean {
        return oldItem == newItem
    }
}
