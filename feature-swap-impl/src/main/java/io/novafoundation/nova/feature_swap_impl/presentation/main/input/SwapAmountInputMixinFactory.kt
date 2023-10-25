package io.novafoundation.nova.feature_swap_impl.presentation.main.input

import androidx.annotation.StringRes
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.feature_currency_api.presentation.formatters.formatAsCurrency
import io.novafoundation.nova.feature_swap_impl.R
import io.novafoundation.nova.feature_swap_impl.presentation.main.input.SwapAmountInputMixin.SwapInputAssetModel
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.BaseAmountChooserProvider
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

typealias MaxAvailableExtractor = (Asset) -> Balance?

class SwapAmountInputMixinFactory(
    private val chainRegistry: ChainRegistry,
    private val resourceManager: ResourceManager
) {

    fun create(
        coroutineScope: CoroutineScope,
        assetFlow: Flow<Asset?>,
        maxAvailable: MaxAvailableExtractor,
        @StringRes emptyAssetTitle: Int
    ): SwapAmountInputMixin.Presentation {
        return RealSwapAmountInputMixin(
            coroutineScope = coroutineScope,
            assetFlow = assetFlow,
            maxAvailableExtractor = maxAvailable,
            emptyAssetTitle = emptyAssetTitle,
            chainRegistry = chainRegistry,
            resourceManager = resourceManager
        )
    }
}

private class RealSwapAmountInputMixin(
    coroutineScope: CoroutineScope,
    assetFlow: Flow<Asset?>,
    private val maxAvailableExtractor: MaxAvailableExtractor,
    @StringRes private val emptyAssetTitle: Int,
    private val chainRegistry: ChainRegistry,
    private val resourceManager: ResourceManager,
) : BaseAmountChooserProvider(coroutineScope), SwapAmountInputMixin.Presentation {

    override val fiatAmount: Flow<String> = combine(assetFlow.filterNotNull(), amount) { asset, amount ->
        asset.token.amountToFiat(amount).formatAsCurrency(asset.token.currency)
    }
        .shareInBackground()

    override val maxAvailable: Flow<String?> = assetFlow.map { asset ->
        if (asset == null) return@map null
        val maxAvailableBalance = maxAvailableExtractor.invoke(asset)

        maxAvailableBalance?.formatPlanks(asset.token.configuration)
    }.shareInBackground()

    override val assetModel: Flow<SwapInputAssetModel> = assetFlow.map {
        val chainAsset = it?.token?.configuration

        if (chainAsset != null) {
            formatInputAsset(chainAsset)
        } else {
            defaultInputModel()
        }
    }

    private suspend fun formatInputAsset(chainAsset: Chain.Asset): SwapInputAssetModel {
        val chain = chainRegistry.getChain(chainAsset.chainId)

        return SwapInputAssetModel(
            assetIcon = SwapInputAssetModel.SwapAssetIcon.Chosen(chainAsset.iconUrl),
            title = chainAsset.symbol,
            subtitleIcon = Icon.FromLink(chain.icon),
            subtitle = chain.name,
            showInput = true,
        )
    }

    private fun defaultInputModel(): SwapInputAssetModel {
        return SwapInputAssetModel(
            assetIcon = SwapInputAssetModel.SwapAssetIcon.NotChosen,
            title = resourceManager.getString(emptyAssetTitle),
            subtitleIcon = null,
            subtitle = resourceManager.getString(R.string.fragment_swap_main_settings_select_token),
            showInput = false,
        )
    }
}
