package io.novafoundation.nova.feature_swap_impl.presentation.mixin.maxAction

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction.MaxActionProvider
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction.MaxActionProviderDsl.deductFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction.MaxActionProviderDsl.providingMaxOf
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction.MaxAvailableDeduction
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.GenericFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.GenericFeeLoaderMixin
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import kotlinx.coroutines.flow.Flow

class MaxActionProviderFactory(
    private val assetSourceRegistry: AssetSourceRegistry,
    private val chainRegistry: ChainRegistry,
) {

    fun <F> create(
        assetInFlow: Flow<Asset?>,
        assetOutFlow: Flow<Asset?>,
        field: (Asset) -> Balance,
        feeLoaderMixin: GenericFeeLoaderMixin<F>,
        allowMaxAction: Boolean = true
    ): MaxActionProvider where F : GenericFee, F : MaxAvailableDeduction {
        return assetInFlow.providingMaxOf(field, allowMaxAction)
            .deductFee(feeLoaderMixin)
            .disallowReapingIfHasDependents(assetOutFlow, assetSourceRegistry, chainRegistry)
    }
}

private fun MaxActionProvider.disallowReapingIfHasDependents(
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
