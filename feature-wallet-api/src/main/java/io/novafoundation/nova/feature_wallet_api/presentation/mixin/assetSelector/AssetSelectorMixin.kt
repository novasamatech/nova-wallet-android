package io.novafoundation.nova.feature_wallet_api.presentation.mixin.assetSelector

import androidx.lifecycle.LiveData
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListBottomSheet
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetModel
import kotlinx.coroutines.flow.Flow

interface AssetSelectorMixin {

    val showAssetChooser: LiveData<Event<DynamicListBottomSheet.Payload<AssetSelectorModel>>>

    fun assetSelectorClicked()

    fun assetChosen(selectorModel: AssetSelectorModel)

    val selectedAssetModelFlow: Flow<AssetSelectorModel>

    interface Presentation : AssetSelectorMixin {

        val selectedAssetFlow: Flow<Asset>
    }
}

data class AssetSelectorModel(
    val assetModel: AssetModel,
    val title: String,
    val additionalIdentifier: String,
)
