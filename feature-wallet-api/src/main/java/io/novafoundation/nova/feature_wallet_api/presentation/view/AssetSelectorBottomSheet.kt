package io.novafoundation.nova.feature_wallet_api.presentation.view

import android.content.Context
import android.os.Bundle
import android.view.View
import coil.ImageLoader
import coil.load
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.ClickHandler
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListBottomSheet
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListSheetAdapter
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.HolderCreator
import io.novafoundation.nova.feature_wallet_api.R
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetModel
import kotlinx.android.synthetic.main.item_asset_selector.view.itemAssetSelectorBalance
import kotlinx.android.synthetic.main.item_asset_selector.view.itemAssetSelectorCheckmark
import kotlinx.android.synthetic.main.item_asset_selector.view.itemAssetSelectorIcon
import kotlinx.android.synthetic.main.item_asset_selector.view.itemAssetSelectorTokenName

class AssetSelectorBottomSheet(
    private val imageLoader: ImageLoader,
    context: Context,
    payload: Payload<AssetModel>,
    onClicked: ClickHandler<AssetModel>
) : DynamicListBottomSheet<AssetModel>(
    context,
    payload,
    AssetModel.DIFF_CALLBACK,
    onClicked
) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.wallet_assets)
    }

    override fun holderCreator(): HolderCreator<AssetModel> = { parent ->
        AssetSelectorHolder(parent.inflateChild(R.layout.item_asset_selector), imageLoader)
    }
}

private class AssetSelectorHolder(
    parent: View,
    private val imageLoader: ImageLoader,
) : DynamicListSheetAdapter.Holder<AssetModel>(parent) {

    override fun bind(
        item: AssetModel,
        isSelected: Boolean,
        handler: DynamicListSheetAdapter.Handler<AssetModel>
    ) {
        super.bind(item, isSelected, handler)

        with(itemView) {
            itemAssetSelectorBalance.text = item.assetBalance
            itemAssetSelectorTokenName.text = item.tokenName
            itemAssetSelectorIcon.load(item.imageUrl, imageLoader)
            itemAssetSelectorCheckmark.setVisible(isSelected, falseState = View.INVISIBLE)
        }
    }
}
