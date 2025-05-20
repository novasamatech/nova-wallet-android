package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectAddress.list

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.address.format.AddressScheme
import io.novafoundation.nova.common.list.BaseListAdapter
import io.novafoundation.nova.common.list.BaseViewHolder
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.view.setExtraInfoAvailable
import io.novafoundation.nova.common.view.shape.addRipple
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.feature_account_api.view.showAddress
import io.novafoundation.nova.feature_ledger_impl.R
import io.novafoundation.nova.feature_ledger_impl.databinding.ItemLedgerAccountBinding
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectAddress.model.LedgerAccountModel

class LedgerAccountAdapter(
    private val handler: Handler
) : BaseListAdapter<LedgerAccountModel, SelectLedgerHolder>(DiffCallback()) {

    interface Handler {

        fun itemClicked(item: LedgerAccountModel)

        fun addressInfoClicked(addressModel: AddressModel, addressScheme: AddressScheme)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectLedgerHolder {
        return SelectLedgerHolder(ItemLedgerAccountBinding.inflate(parent.inflater(), parent, false), handler)
    }

    override fun onBindViewHolder(holder: SelectLedgerHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

private class DiffCallback : DiffUtil.ItemCallback<LedgerAccountModel>() {
    override fun areItemsTheSame(oldItem: LedgerAccountModel, newItem: LedgerAccountModel): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: LedgerAccountModel, newItem: LedgerAccountModel): Boolean {
        return oldItem == newItem
    }
}

class SelectLedgerHolder(
    private val viewBinding: ItemLedgerAccountBinding,
    private val eventHandler: LedgerAccountAdapter.Handler
) : BaseViewHolder(viewBinding.root) {
    init {
        viewBinding.root.background = with(containerView.context) {
            addRipple(getRoundedCornerDrawable(R.color.block_background))
        }

        viewBinding.itemLedgerAccountSubstrate.setExtraInfoAvailable(true)
    }

    fun bind(model: LedgerAccountModel) = with(viewBinding) {
        viewBinding.root.setOnClickListener { eventHandler.itemClicked(model) }

        itemLedgerAccountLabel.text = model.label
        itemLedgerAccountIcon.setImageDrawable(model.substrate.image)

        itemLedgerAccountSubstrate.showAddress(model.substrate)
        itemLedgerAccountSubstrate.setOnClickListener { eventHandler.addressInfoClicked(model.substrate, AddressScheme.SUBSTRATE) }

        if (model.evm != null) {
            itemLedgerAccountEvm.valuePrimary.setTextColorRes(R.color.text_primary)
            itemLedgerAccountEvm.setPrimaryValueStartIcon(null)
            itemLedgerAccountEvm.showAddress(model.evm)

            itemLedgerAccountEvm.setOnClickListener { eventHandler.addressInfoClicked(model.evm, AddressScheme.EVM) }
            itemLedgerAccountEvm.setExtraInfoAvailable(true)
        } else {
            itemLedgerAccountEvm.valuePrimary.setTextColorRes(R.color.text_secondary)
            itemLedgerAccountEvm.setPrimaryValueStartIcon(R.drawable.ic_warning_filled)
            itemLedgerAccountEvm.showValue(context.getString(R.string.ledger_select_address_not_found))

            itemLedgerAccountEvm.setOnClickListener(null)
            itemLedgerAccountEvm.setExtraInfoAvailable(false)
        }
    }

    override fun unbind() {}
}
