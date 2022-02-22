package io.novafoundation.nova.feature_wallet_api.presentation.mixin.assetSelector

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.mixin.MixinFactory
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListBottomSheet
import io.novafoundation.nova.feature_wallet_api.data.mappers.mapAssetToAssetModel
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetSelectorModel
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch

class AssetSelectorFactory(
    private val assetUseCase: AssetUseCase,
    private val singleAssetSharedState: SingleAssetSharedState,
    private val resourceManager: ResourceManager
) : MixinFactory<AssetSelectorMixin.Presentation> {

    override fun create(scope: CoroutineScope): AssetSelectorMixin.Presentation {
        return AssetSelectorProvider(assetUseCase, resourceManager, singleAssetSharedState, scope)
    }
}

private class AssetSelectorProvider(
    private val assetUseCase: AssetUseCase,
    private val resourceManager: ResourceManager,
    private val singleAssetSharedState: SingleAssetSharedState,
    private val scope: CoroutineScope,
) : AssetSelectorMixin.Presentation, CoroutineScope by scope {

    override val showAssetChooser = MutableLiveData<Event<DynamicListBottomSheet.Payload<AssetSelectorModel>>>()

    override val selectedAssetFlow: Flow<Asset> = assetUseCase.currentAssetFlow()
        .inBackground()
        .shareIn(this, SharingStarted.Eagerly, replay = 1)

    override val selectedAssetModelFlow: Flow<AssetSelectorModel> = selectedAssetFlow
        .map {
            mapAssetToAssetModel(it, resourceManager, patternId = null)
        }
        .shareIn(this, SharingStarted.Eagerly, replay = 1)

    override fun assetSelectorClicked() {
        launch {
            val availableToSelect = assetUseCase.availableAssetsToSelect()

            val models = availableToSelect.map { mapAssetToAssetModel(it, resourceManager, patternId = null) }

            val selectedChainAsset = selectedAssetFlow.first().token.configuration

            val selectedModel = models.firstOrNull { it.chainAssetId == selectedChainAsset.id && it.chainId == selectedChainAsset.chainId }

            showAssetChooser.value = Event(DynamicListBottomSheet.Payload(models, selectedModel))
        }
    }

    override fun assetChosen(assetModel: AssetSelectorModel) {
        singleAssetSharedState.update(assetModel.chainId, assetModel.chainAssetId)
    }
}
