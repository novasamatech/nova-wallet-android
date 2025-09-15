package io.novafoundation.nova.feature_wallet_api.presentation.mixin.assetSelector

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.data.model.DiscreetMode
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.WithCoroutineScopeExtensions
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListBottomSheet
import io.novafoundation.nova.feature_account_api.presenatation.chain.iconOrFallback
import io.novafoundation.nova.feature_wallet_api.data.mappers.mapAssetToAssetModel
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.SelectableAssetAndOption
import io.novafoundation.nova.feature_wallet_api.domain.SelectableAssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.maskable.MaskableValueFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.maskable.MaskableValueFormatterFactory
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.maskable.MaskableValueFormatterProvider
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch

class AssetSelectorFactory(
    private val assetIconProvider: AssetIconProvider,
    private val assetUseCase: SelectableAssetUseCase<*>,
    private val singleAssetSharedState: SingleAssetSharedState,
    private val resourceManager: ResourceManager,
    private val maskableValueFormatterProvider: MaskableValueFormatterProvider,
    private val maskableValueFormatterFactory: MaskableValueFormatterFactory,
) {

    fun create(
        scope: CoroutineScope,
        amountProvider: suspend (SelectableAssetAndOption) -> Balance
    ): AssetSelectorMixin.Presentation {
        return AssetSelectorProvider(
            assetIconProvider,
            assetUseCase,
            resourceManager,
            singleAssetSharedState,
            scope,
            amountProvider,
            maskableValueFormatterProvider,
            maskableValueFormatterFactory.create(DiscreetMode.DISABLED) // To format values without masking in asset list
        )
    }
}

private class AssetSelectorProvider(
    private val assetIconProvider: AssetIconProvider,
    private val assetUseCase: SelectableAssetUseCase<*>,
    private val resourceManager: ResourceManager,
    private val singleAssetSharedState: SingleAssetSharedState,
    private val scope: CoroutineScope,
    private val amountProvider: suspend (SelectableAssetAndOption) -> Balance,
    private val maskableValueFormatterProvider: MaskableValueFormatterProvider,
    private val noMaskingValueFormatter: MaskableValueFormatter, // To format values without masking in asset list
) : AssetSelectorMixin.Presentation, CoroutineScope by scope, WithCoroutineScopeExtensions by WithCoroutineScopeExtensions(scope) {

    private val maskableValueFormatterFlow = maskableValueFormatterProvider.provideFormatter()
        .shareInBackground()

    override val showAssetChooser = MutableLiveData<Event<DynamicListBottomSheet.Payload<AssetSelectorModel>>>()

    private val selectedAssetAndOptionFlow = assetUseCase.currentAssetAndOptionFlow()
        .shareInBackground(SharingStarted.Eagerly)

    override val selectedAssetFlow: Flow<Asset> = selectedAssetAndOptionFlow.map { it.asset }

    override val selectedAssetModelFlow: Flow<AssetSelectorModel> = selectedAssetAndOptionFlow
        .combine(maskableValueFormatterFlow) { option, formatter -> mapAssetAndOptionToSelectorModel(option, formatter) }
        .shareIn(this, SharingStarted.Eagerly, replay = 1)

    override fun assetSelectorClicked() {
        launch {
            val availableToSelect = assetUseCase.availableAssetsToSelect()

            val models = availableToSelect.map { mapAssetAndOptionToSelectorModel(it, noMaskingValueFormatter) }
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

    private suspend fun mapAssetAndOptionToSelectorModel(
        assetAndOption: SelectableAssetAndOption,
        maskableValueFormatter: MaskableValueFormatter
    ): AssetSelectorModel {
        val balance = amountProvider(assetAndOption)

        val assetModel = mapAssetToAssetModel(
            assetIconProvider,
            assetAndOption.asset,
            resourceManager,
            icon = assetAndOption.option.assetWithChain.chain.iconOrFallback(),
            patternId = null,
            maskableBalance = maskableValueFormatter.formatAny { balance }
        )
        val title = assetAndOption.formatTitle()

        return AssetSelectorModel(assetModel, title, assetAndOption.option.additional.identifier)
    }

    private fun SelectableAssetAndOption.formatTitle(): String {
        val formattedOptionLabel = option.additional.format(resourceManager)
        val tokenName = asset.token.configuration.name

        return if (formattedOptionLabel != null) {
            "$tokenName $formattedOptionLabel"
        } else {
            tokenName
        }
    }
}
