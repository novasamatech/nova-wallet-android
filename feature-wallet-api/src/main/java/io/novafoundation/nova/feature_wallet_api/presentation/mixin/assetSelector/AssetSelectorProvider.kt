package io.novafoundation.nova.feature_wallet_api.presentation.mixin.assetSelector

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.WithCoroutineScopeExtensions
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListBottomSheet
import io.novafoundation.nova.feature_wallet_api.data.mappers.mapAssetToAssetModel
import io.novafoundation.nova.feature_wallet_api.domain.AssetAndOption
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import java.math.BigDecimal

class AssetSelectorFactory(
    private val assetUseCase: AssetUseCase,
    private val singleAssetSharedState: SingleAssetSharedState,
    private val resourceManager: ResourceManager
) {

    fun create(
        scope: CoroutineScope,
        amountProvider: (Asset) -> BigDecimal
    ): AssetSelectorMixin.Presentation {
        return AssetSelectorProvider(assetUseCase, resourceManager, singleAssetSharedState, scope, amountProvider)
    }
}

private class AssetSelectorProvider(
    private val assetUseCase: AssetUseCase,
    private val resourceManager: ResourceManager,
    private val singleAssetSharedState: SingleAssetSharedState,
    private val scope: CoroutineScope,
    private val amountProvider: (Asset) -> BigDecimal
) : AssetSelectorMixin.Presentation, CoroutineScope by scope, WithCoroutineScopeExtensions by WithCoroutineScopeExtensions(scope) {

    override val showAssetChooser = MutableLiveData<Event<DynamicListBottomSheet.Payload<AssetSelectorModel>>>()

    private val selectedAssetAndOptionFlow = assetUseCase.currentAssetAndOptionFlow()
        .shareInBackground(SharingStarted.Eagerly)

    override val selectedAssetFlow: Flow<Asset> = selectedAssetAndOptionFlow.map { it.asset }

    override val selectedAssetModelFlow: Flow<AssetSelectorModel> = selectedAssetAndOptionFlow
        .map(::mapAssetAndOptionToSelectorModel)
        .shareIn(this, SharingStarted.Eagerly, replay = 1)

    override fun assetSelectorClicked() {
        launch {
            val availableToSelect = assetUseCase.availableAssetsToSelect()

            val models = availableToSelect.map(::mapAssetAndOptionToSelectorModel)
            val selectedOption = selectedAssetAndOptionFlow.first()
            val selectedChainAsset = selectedOption.asset.token.configuration

            val selectedModel = models.firstOrNull {
                it.assetModel.chainAssetId == selectedChainAsset.id &&
                    it.assetModel.chainId == selectedChainAsset.chainId &&
                    it.additionalIdentifier == selectedOption.option.additional.identifier
            }

            showAssetChooser.value = Event(DynamicListBottomSheet.Payload(models, selectedModel))
        }
    }

    override fun assetChosen(selectorModel: AssetSelectorModel) {
        singleAssetSharedState.update(
            chainId = selectorModel.assetModel.chainId,
            chainAssetId = selectorModel.assetModel.chainAssetId,
            optionIdentifier = selectorModel.additionalIdentifier
        )
    }

    private fun mapAssetAndOptionToSelectorModel(assetAndOption: AssetAndOption): AssetSelectorModel {
        val assetModel = mapAssetToAssetModel(assetAndOption.asset, resourceManager, patternId = null, retrieveAmount = amountProvider)
        val title = assetAndOption.formatTitle()

        return AssetSelectorModel(assetModel, title, assetAndOption.option.additional.identifier)
    }

    private fun AssetAndOption.formatTitle(): String {
        val formattedOptionLabel = option.additional.format(resourceManager)
        val tokenName = asset.token.configuration.name

        return if (formattedOptionLabel != null) {
            "$tokenName $formattedOptionLabel"
        } else {
            tokenName
        }
    }
}
