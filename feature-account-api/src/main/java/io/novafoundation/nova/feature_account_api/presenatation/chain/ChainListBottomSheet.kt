package io.novafoundation.nova.feature_account_api.presenatation.chain

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.DiffUtil
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListBottomSheet
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListSheetAdapter
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.HolderCreator
import io.novafoundation.nova.feature_account_api.R
import kotlinx.android.synthetic.main.item_bottom_sheet_chain_list.view.itemChainListBottomSheet

class ChainListBottomSheet(
    context: Context,
    data: List<ChainUi>,
) : DynamicListBottomSheet<ChainUi>(context, Payload(data), AccountDiffCallback, onClicked = null) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.common_networks)
    }

    override fun holderCreator(): HolderCreator<ChainUi> = {
        ChainListHolder(it.inflateChild(R.layout.item_bottom_sheet_chain_list))
    }
}

class ChainListHolder(
    itemView: View
) : DynamicListSheetAdapter.Holder<ChainUi>(itemView) {

    override fun bind(item: ChainUi, isSelected: Boolean, handler: DynamicListSheetAdapter.Handler<ChainUi>) {
        itemView.itemChainListBottomSheet.setChain(item)
    }
}

private object AccountDiffCallback : DiffUtil.ItemCallback<ChainUi>() {
    override fun areItemsTheSame(oldItem: ChainUi, newItem: ChainUi): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: ChainUi, newItem: ChainUi): Boolean {
        return true
    }
}
