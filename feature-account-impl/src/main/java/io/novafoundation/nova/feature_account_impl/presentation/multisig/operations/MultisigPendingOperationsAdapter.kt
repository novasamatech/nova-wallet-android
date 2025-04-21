package io.novafoundation.nova.feature_account_impl.presentation.multisig.operations

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import io.novafoundation.nova.common.list.BaseListAdapter
import io.novafoundation.nova.common.list.BaseViewHolder
import io.novafoundation.nova.common.presentation.setColoredText
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.view.shape.addRipple
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.databinding.ItemMultisigPendingOperationBinding
import io.novafoundation.nova.feature_account_impl.presentation.multisig.operations.model.PendingMultisigOperationModel

class MultisigPendingOperationsAdapter(
    private val handler: ItemHandler
) : BaseListAdapter<PendingMultisigOperationModel, MultisigPendingOperationHolder>(PendingMultisigOperationDiffCallback()) {

    interface ItemHandler {

        fun itemClicked(model: PendingMultisigOperationModel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MultisigPendingOperationHolder {
        return MultisigPendingOperationHolder(
            viewBinding = ItemMultisigPendingOperationBinding.inflate(parent.inflater(), parent, false),
            itemHandler = handler,
        )
    }

    override fun onBindViewHolder(holder: MultisigPendingOperationHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class MultisigPendingOperationHolder(
    private val viewBinding: ItemMultisigPendingOperationBinding,
    private val itemHandler: MultisigPendingOperationsAdapter.ItemHandler,
) : BaseViewHolder(viewBinding.root) {

    init {
        with(viewBinding.root) {
            background = context.addRipple(context.getRoundedCornerDrawable(R.color.block_background))
        }
    }

    fun bind(model: PendingMultisigOperationModel) = with(viewBinding) {
        itemPendingOperationTitle.text = model.operationTitle
        itemPendingOperationChain.setChain(model.chain)
        itemPendingOperationProgress.text = model.progress
        itemPendingOperationAction.setColoredText(model.action)

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
