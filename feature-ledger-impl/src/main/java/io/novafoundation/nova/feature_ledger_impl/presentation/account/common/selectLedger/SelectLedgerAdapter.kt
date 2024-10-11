package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import io.novafoundation.nova.common.list.BaseListAdapter
import io.novafoundation.nova.common.list.BaseViewHolder
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.view.shape.addRipple
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.feature_ledger_impl.R
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.model.SelectLedgerModel

class SelectLedgerAdapter(
    private val handler: Handler
) : BaseListAdapter<SelectLedgerModel, SelectLedgerHolder>(DiffCallback()) {

    interface Handler {

        fun itemClicked(item: SelectLedgerModel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectLedgerHolder {
        return SelectLedgerHolder(parent.inflateChild(R.layout.item_ledger), handler)
    }

    override fun onBindViewHolder(holder: SelectLedgerHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

private class DiffCallback : DiffUtil.ItemCallback<SelectLedgerModel>() {
    override fun areItemsTheSame(oldItem: SelectLedgerModel, newItem: SelectLedgerModel): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: SelectLedgerModel, newItem: SelectLedgerModel): Boolean {
        return oldItem == newItem
    }
}

class SelectLedgerHolder(
    containerView: View,
    private val eventHandler: SelectLedgerAdapter.Handler
) : BaseViewHolder(containerView) {
    init {
        containerView.itemLedger.background = with(containerView.context) {
            addRipple(getRoundedCornerDrawable(R.color.block_background))
        }

        containerView.itemLedger.setProgressTint(R.color.icon_secondary)
    }

    fun bind(model: SelectLedgerModel) = with(containerView) {
        itemLedger.title.text = model.name

        bindConnecting(model)
    }

    fun bindConnecting(model: SelectLedgerModel) = with(containerView) {
        itemLedger.setInProgress(model.isConnecting)

        if (model.isConnecting) {
            setOnClickListener(null)
        } else {
            setOnClickListener { eventHandler.itemClicked(model) }
        }
    }

    override fun unbind() {}
}
