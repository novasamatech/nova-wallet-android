package io.novafoundation.nova.feature_wallet_api.presentation.view

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.DiffUtil
import coil.ImageLoader
import io.novafoundation.nova.common.utils.images.setIcon
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.ClickHandler
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListBottomSheet
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListSheetAdapter
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.HolderCreator
import io.novafoundation.nova.feature_wallet_api.R
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.assetSelector.AssetSelectorModel
import kotlinx.android.synthetic.main.item_asset_selector.view.itemAssetSelectorBalance
import kotlinx.android.synthetic.main.item_asset_selector.view.itemAssetSelectorIcon
import kotlinx.android.synthetic.main.item_asset_selector.view.itemAssetSelectorRadioButton
import kotlinx.android.synthetic.main.item_asset_selector.view.itemAssetSelectorTokenName

class AssetSelectorBottomSheet(
    private val imageLoader: ImageLoader,
    context: Context,
    payload: Payload<AssetSelectorModel>,
    onClicked: ClickHandler<AssetSelectorModel>
) : DynamicListBottomSheet<AssetSelectorModel>(
    context,
    payload,
    DiffCallback(),
    onClicked
) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.wallet_assets)
        setSubtitle(null)
    }

    override fun holderCreator(): HolderCreator<AssetSelectorModel> = { parent ->
        AssetSelectorHolder(parent.inflateChild(R.layout.item_asset_selector), imageLoader)
    }
}

private class AssetSelectorHolder(
    parent: View,
    private val imageLoader: ImageLoader,
) : DynamicListSheetAdapter.Holder<AssetSelectorModel>(parent) {

    override fun bind(
        item: AssetSelectorModel,
        isSelected: Boolean,
        handler: DynamicListSheetAdapter.Handler<AssetSelectorModel>
    ) {
        super.bind(item, isSelected, handler)

        with(itemView) {
            itemAssetSelectorBalance.text = item.assetModel.assetBalance
            itemAssetSelectorTokenName.text = item.title
            itemAssetSelectorIcon.setIcon(item.assetModel.icon, imageLoader)
            itemAssetSelectorRadioButton.isChecked = isSelected
        }
    }
}

private class DiffCallback : DiffUtil.ItemCallback<AssetSelectorModel>() {

    override fun areItemsTheSame(oldItem: AssetSelectorModel, newItem: AssetSelectorModel): Boolean {
        return oldItem.assetModel.chainId == newItem.assetModel.chainId &&
            oldItem.assetModel.chainAssetId == newItem.assetModel.chainAssetId &&
            oldItem.additionalIdentifier == newItem.additionalIdentifier
    }

    override fun areContentsTheSame(oldItem: AssetSelectorModel, newItem: AssetSelectorModel): Boolean {
        return oldItem == newItem
    }
}
