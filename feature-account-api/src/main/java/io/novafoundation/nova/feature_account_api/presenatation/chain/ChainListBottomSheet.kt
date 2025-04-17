package io.novafoundation.nova.feature_account_api.presenatation.chain

import android.content.Context
import android.os.Bundle
import androidx.recyclerview.widget.DiffUtil
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListBottomSheet
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListSheetAdapter
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.HolderCreator
import io.novafoundation.nova.feature_account_api.R
import io.novafoundation.nova.feature_account_api.databinding.ItemBottomSheetChainListBinding

class ChainListBottomSheet(
    context: Context,
    data: List<ChainUi>,
) : DynamicListBottomSheet<ChainUi>(context, Payload(data), AccountDiffCallback, onClicked = null) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.common_networks)
    }

    override fun holderCreator(): HolderCreator<ChainUi> = {
        ChainListHolder(ItemBottomSheetChainListBinding.inflate(it.inflater(), it, false))
    }
}

class ChainListHolder(
    private val binder: ItemBottomSheetChainListBinding
) : DynamicListSheetAdapter.Holder<ChainUi>(binder.root) {

    override fun bind(item: ChainUi, isSelected: Boolean, handler: DynamicListSheetAdapter.Handler<ChainUi>) {
        binder.itemChainListBottomSheet.setChain(item)
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
