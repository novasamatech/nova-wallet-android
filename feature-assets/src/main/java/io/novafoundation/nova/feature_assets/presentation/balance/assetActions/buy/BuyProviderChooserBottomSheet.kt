package io.novafoundation.nova.feature_assets.presentation.balance.assetActions.buy

import android.content.Context
import android.os.Bundle
import android.view.View
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.ClickHandler
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListBottomSheet
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListSheetAdapter
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.HolderCreator
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.ReferentialEqualityDiffCallBack
import io.novafoundation.nova.common.view.dialog.infoDialog
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.data.buyToken.BuyTokenRegistry
import kotlinx.android.synthetic.main.item_sheet_buy_provider.view.itemSheetBuyProviderImage
import kotlinx.android.synthetic.main.item_sheet_buy_provider.view.itemSheetBuyProviderText

typealias BuyProvider = BuyTokenRegistry.Provider<*>

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
        BuyProviderHolder(it.inflateChild(R.layout.item_sheet_buy_provider))
    }
}

private class BuyProviderHolder(
    itemView: View,
) : DynamicListSheetAdapter.Holder<BuyProvider>(itemView) {

    override fun bind(item: BuyProvider, isSelected: Boolean, handler: DynamicListSheetAdapter.Handler<BuyProvider>) {
        super.bind(item, isSelected, handler)

        with(itemView) {
            itemSheetBuyProviderText.text = item.name
            itemSheetBuyProviderImage.setImageResource(item.icon)
        }
    }
}
