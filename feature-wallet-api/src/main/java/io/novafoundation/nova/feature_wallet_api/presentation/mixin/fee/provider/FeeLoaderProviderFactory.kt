package io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.provider

import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_wallet_api.domain.fee.CustomFeeInteractor
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.GenericFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.GenericFeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.SimpleFee
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

class FeeLoaderProviderFactory(
    private val customFeeInteractor: CustomFeeInteractor,
    private val chainRegistry: ChainRegistry,
    private val resourceManager: ResourceManager,
    private val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
) : FeeLoaderMixin.Factory {

    override fun create(
        tokenFlow: Flow<Token?>,
        configuration: GenericFeeLoaderMixin.Configuration<SimpleFee>
    ): FeeLoaderMixin.Presentation {
        return GenericFeeLoaderProviderPresentation(resourceManager, configuration, tokenFlow)
    }

    override fun <F : GenericFee> createGeneric(
        tokenFlow: Flow<Token?>,
        configuration: GenericFeeLoaderMixin.Configuration<F>
    ): GenericFeeLoaderMixin.Presentation<F> {
        return GenericFeeLoaderProvider(resourceManager, configuration, tokenFlow)
    }

    override fun createChangeableFee(
        tokenFlow: Flow<Token?>,
        coroutineScope: CoroutineScope,
        configuration: GenericFeeLoaderMixin.Configuration<SimpleFee>
    ): FeeLoaderMixin.Presentation {
        return ChangeableFeeLoaderProviderPresentation(
            customFeeInteractor,
            chainRegistry,
            resourceManager,
            configuration,
            actionAwaitableMixinFactory,
            tokenFlow,
            coroutineScope
        )
    }

    override fun <F : GenericFee> createChangeableFeeGeneric(
        tokenFlow: Flow<Token?>,
        coroutineScope: CoroutineScope,
        configuration: GenericFeeLoaderMixin.Configuration<F>
    ): GenericFeeLoaderMixin.Presentation<F> {
        return ChangeableFeeLoaderProvider(
            customFeeInteractor,
            chainRegistry,
            resourceManager,
            configuration,
            actionAwaitableMixinFactory,
            tokenFlow,
            coroutineScope
        )
    }
}
