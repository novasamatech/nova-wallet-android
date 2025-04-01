package io.novafoundation.nova.feature_buy_impl.presentation.mixin

import android.content.Context
import android.os.Bundle
import android.view.View
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.ClickHandler
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListBottomSheet
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListSheetAdapter
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.HolderCreator
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.ReferentialEqualityDiffCallBack
import io.novafoundation.nova.feature_buy_api.domain.TradeProvider
import io.novafoundation.nova.feature_buy_impl.R
import kotlinx.android.synthetic.main.item_sheet_buy_provider.view.itemSheetBuyProviderImage
import kotlinx.android.synthetic.main.item_sheet_buy_provider.view.itemSheetBuyProviderText

class BuyProviderChooserBottomSheet(
    context: Context,
    payload: Payload<TradeProvider>,
    onSelect: ClickHandler<TradeProvider>,
    onCancel: () -> Unit,
) : DynamicListBottomSheet<TradeProvider>(
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

    override fun holderCreator(): HolderCreator<TradeProvider> = {
        BuyProviderHolder(it.inflateChild(R.layout.item_sheet_buy_provider))
    }
}

private class BuyProviderHolder(
    itemView: View,
) : DynamicListSheetAdapter.Holder<TradeProvider>(itemView) {

    override fun bind(item: TradeProvider, isSelected: Boolean, handler: DynamicListSheetAdapter.Handler<TradeProvider>) {
        super.bind(item, isSelected, handler)

        with(itemView) {
            itemSheetBuyProviderText.text = item.name
            itemSheetBuyProviderImage.setImageResource(item.logoRes)
        }
    }
}
