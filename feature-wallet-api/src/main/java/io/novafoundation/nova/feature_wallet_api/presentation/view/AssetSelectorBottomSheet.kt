package io.novafoundation.nova.feature_wallet_api.presentation.view

import android.content.Context
import android.os.Bundle
import androidx.recyclerview.widget.DiffUtil
import coil.ImageLoader
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.images.setIcon
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.ClickHandler
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListBottomSheet
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListSheetAdapter
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.HolderCreator
import io.novafoundation.nova.feature_wallet_api.R
import io.novafoundation.nova.feature_wallet_api.databinding.ItemAssetSelectorBinding
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.assetSelector.AssetSelectorModel

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

        setTitle(R.string.select_network_title)
        setSubtitle(null)
    }

    override fun holderCreator(): HolderCreator<AssetSelectorModel> = { parent ->
        AssetSelectorHolder(ItemAssetSelectorBinding.inflate(parent.inflater(), parent, false), imageLoader)
    }
}

private class AssetSelectorHolder(
    private val binder: ItemAssetSelectorBinding,
    private val imageLoader: ImageLoader,
) : DynamicListSheetAdapter.Holder<AssetSelectorModel>(binder.root) {

    override fun bind(
        item: AssetSelectorModel,
        isSelected: Boolean,
        handler: DynamicListSheetAdapter.Handler<AssetSelectorModel>
    ) {
        super.bind(item, isSelected, handler)

        with(itemView) {
            binder.itemAssetSelectorBalance.text = item.assetModel.assetBalance
            binder.itemAssetSelectorTokenName.text = item.title
            binder.itemAssetSelectorIcon.setIcon(item.assetModel.icon, imageLoader)
            binder.itemAssetSelectorRadioButton.isChecked = isSelected
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
