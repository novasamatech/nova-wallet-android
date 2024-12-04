package io.novafoundation.nova.feature_swap_impl.presentation.main.input

import androidx.annotation.StringRes
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.FieldValidator
import io.novafoundation.nova.feature_account_api.presenatation.chain.getAssetIconOrFallback
import io.novafoundation.nova.feature_account_api.presenatation.chain.iconOrFallback
import io.novafoundation.nova.feature_swap_impl.R
import io.novafoundation.nova.feature_swap_impl.presentation.main.input.SwapAmountInputMixin.SwapInputAssetModel
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixinBase
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.BaseAmountChooserProvider
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.DefaultFiatFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction.MaxActionProvider
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SwapAmountInputMixinFactory(
    private val chainRegistry: ChainRegistry,
    private val resourceManager: ResourceManager,
    private val assetIconProvider: AssetIconProvider
) {

    fun create(
        coroutineScope: CoroutineScope,
        tokenFlow: Flow<Token?>,
        @StringRes emptyAssetTitle: Int,
        maxActionProvider: MaxActionProvider? = null,
        fiatFormatter: AmountChooserMixinBase.FiatFormatter = DefaultFiatFormatter(),
        fieldValidator: FieldValidator
    ): SwapAmountInputMixin.Presentation {
        return RealSwapAmountInputMixin(
            coroutineScope = coroutineScope,
            tokenFlow = tokenFlow,
            emptyAssetTitle = emptyAssetTitle,
            chainRegistry = chainRegistry,
            resourceManager = resourceManager,
            maxActionProvider = maxActionProvider,
            fiatFormatter = fiatFormatter,
            fieldValidator = fieldValidator,
            assetIconProvider = assetIconProvider
        )
    }
}

private class RealSwapAmountInputMixin(
    coroutineScope: CoroutineScope,
    tokenFlow: Flow<Token?>,
    @StringRes private val emptyAssetTitle: Int,
    private val chainRegistry: ChainRegistry,
    private val resourceManager: ResourceManager,
    maxActionProvider: MaxActionProvider?,
    fiatFormatter: AmountChooserMixinBase.FiatFormatter,
    fieldValidator: FieldValidator,
    private val assetIconProvider: AssetIconProvider
) : BaseAmountChooserProvider(
    coroutineScope = coroutineScope,
    tokenFlow = tokenFlow,
    maxActionProvider = maxActionProvider,
    fiatFormatter = fiatFormatter,
    allowMaxAction = true,
    fieldValidator = fieldValidator
),
    SwapAmountInputMixin.Presentation {

    override val assetModel: Flow<SwapInputAssetModel> = tokenFlow.map {
        val chainAsset = it?.configuration

        if (chainAsset != null) {
            formatInputAsset(chainAsset)
        } else {
            defaultInputModel()
        }
    }

    private suspend fun formatInputAsset(chainAsset: Chain.Asset): SwapInputAssetModel {
        val chain = chainRegistry.getChain(chainAsset.chainId)

        return SwapInputAssetModel(
            assetIcon = SwapInputAssetModel.SwapAssetIcon.Chosen(assetIconProvider.getAssetIconOrFallback(chainAsset)),
            title = chainAsset.symbol.value,
            subtitleIcon = chain.iconOrFallback(),
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
