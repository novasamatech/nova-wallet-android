package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import io.novafoundation.nova.common.list.BaseListAdapter
import io.novafoundation.nova.common.list.BaseViewHolder
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.view.shape.addRipple
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.feature_ledger_impl.R
import io.novafoundation.nova.feature_ledger_impl.databinding.ItemLedgerBinding
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.model.SelectLedgerModel

class SelectLedgerAdapter(
    private val handler: Handler
) : BaseListAdapter<SelectLedgerModel, SelectLedgerHolder>(DiffCallback()) {

    interface Handler {

        fun itemClicked(item: SelectLedgerModel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectLedgerHolder {
        return SelectLedgerHolder(ItemLedgerBinding.inflate(parent.inflater(), parent, false), handler)
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
    private val binder: ItemLedgerBinding,
    private val eventHandler: SelectLedgerAdapter.Handler
) : BaseViewHolder(binder.root) {
    init {
        binder.itemLedger.background = with(containerView.context) {
            addRipple(getRoundedCornerDrawable(R.color.block_background))
        }

        binder.itemLedger.setProgressTint(R.color.icon_secondary)
    }

    fun bind(model: SelectLedgerModel) = with(binder) {
        itemLedger.title.text = model.name

        bindConnecting(model)
    }

    fun bindConnecting(model: SelectLedgerModel) = with(binder) {
        itemLedger.setInProgress(model.isConnecting)

        if (model.isConnecting) {
            root.setOnClickListener(null)
        } else {
            root.setOnClickListener { eventHandler.itemClicked(model) }
        }
    }

    override fun unbind() {}
}
