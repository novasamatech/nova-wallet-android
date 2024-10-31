package io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2

import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_wallet_api.domain.fee.CustomFeeInteractor
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.amount.FeeBalanceExtractor
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.formatter.FeeFormatter
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

class FeeLoaderV2Factory(
    private val chainRegistry: ChainRegistry,
    private val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
    private val resourceManager: ResourceManager,
    private val interactor: CustomFeeInteractor,
): FeeLoaderMixinV2.Factory {

    override fun <F, D> create(
        scope: CoroutineScope,
        selectedChainAssetFlow: Flow<Chain.Asset>,
        feeFormatter: FeeFormatter<F, D>,
        feeBalanceExtractor: FeeBalanceExtractor<F>,
        configuration: FeeLoaderMixinV2.Configuration<F, D>
    ): FeeLoaderMixinV2.Presentation<F, D> {
        return FeeLoaderV2Provider(
            chainRegistry = chainRegistry,
            actionAwaitableMixinFactory = actionAwaitableMixinFactory,
            resourceManager = resourceManager,
            interactor = interactor,
            feeFormatter = feeFormatter,
            configuration = configuration,
            feeBalanceExtractor = feeBalanceExtractor,
            selectedChainAssetFlow = selectedChainAssetFlow,
            coroutineScope = scope
        )
    }
}
