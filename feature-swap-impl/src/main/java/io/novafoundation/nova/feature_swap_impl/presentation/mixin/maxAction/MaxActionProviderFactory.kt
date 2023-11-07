package io.novafoundation.nova.feature_swap_impl.presentation.mixin.maxAction

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction.MaxActionProvider
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction.MaxActionProviderDsl.deductFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction.MaxActionProviderDsl.providingMaxOf
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.GenericFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.GenericFeeLoaderMixin
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import kotlinx.coroutines.flow.Flow

class MaxActionProviderFactory(
    private val assetSourceRegistry: AssetSourceRegistry,
    private val chainRegistry: ChainRegistry,
) {

    fun <F : GenericFee> create(
        assetInFlow: Flow<Asset?>,
        assetOutFlow: Flow<Asset?>,
        field: (Asset) -> Balance,
        feeLoaderMixin: GenericFeeLoaderMixin<F>,
        extractTotalFee: (F) -> Balance,
        allowMaxAction: Boolean = true
    ): MaxActionProvider {
        return assetInFlow.providingMaxOf(field, allowMaxAction)
            .deductFee(feeLoaderMixin, extractTotalFee)
            .considerConsumers(assetOutFlow, assetSourceRegistry, chainRegistry)
    }
}

private fun MaxActionProvider.considerConsumers(
    assetOutFlow: Flow<Asset?>,
    assetSourceRegistry: AssetSourceRegistry,
    chainRegistry: ChainRegistry
): MaxActionProvider {
    return SwapExistentialDepositAwareMaxActionProvider(
        assetOutFlow,
        assetSourceRegistry,
        this,
        chainRegistry
    )
}
