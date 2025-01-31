package io.novafoundation.nova.feature_buy_impl.presentation.mixin

import android.content.Context
import android.os.Bundle
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.ClickHandler
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListBottomSheet
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListSheetAdapter
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.HolderCreator
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.ReferentialEqualityDiffCallBack
import io.novafoundation.nova.feature_buy_api.domain.BuyProvider
import io.novafoundation.nova.feature_buy_impl.R
import io.novafoundation.nova.feature_buy_impl.databinding.ItemSheetBuyProviderBinding

class BuyProviderChooserBottomSheet(
    context: Context,
    payload: Payload<BuyProvider>,
    onSelect: ClickHandler<BuyProvider>,
    onCancel: () -> Unit,
) : DynamicListBottomSheet<BuyProvider>(
    context = context,
    payload = payload,
    diffCallback = ReferentialEqualityDiffCallBack(),
    onClicked = onSelect,
    onCancel = onCancel
) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.wallet_asset_buy_with)
    }

    override fun holderCreator(): HolderCreator<BuyProvider> = {
        BuyProviderHolder(ItemSheetBuyProviderBinding.inflate(it.inflater(), it, false))
    }
}

private class BuyProviderHolder(
    private val binder: ItemSheetBuyProviderBinding,
) : DynamicListSheetAdapter.Holder<BuyProvider>(binder.root) {

    override fun bind(item: BuyProvider, isSelected: Boolean, handler: DynamicListSheetAdapter.Handler<BuyProvider>) {
        super.bind(item, isSelected, handler)

        with(itemView) {
            binder.itemSheetBuyProviderText.text = item.name
            binder.itemSheetBuyProviderImage.setImageResource(item.icon)
        }
    }
}
