package io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2

import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.data.model.SubmissionFee
import io.novafoundation.nova.feature_wallet_api.domain.fee.FeeInteractor
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.amount.DefaultFeeInspector
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.amount.FeeInspector
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.formatter.DefaultFeeFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.formatter.FeeFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeDisplay
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

class FeeLoaderV2Factory(
    private val chainRegistry: ChainRegistry,
    private val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
    private val resourceManager: ResourceManager,
    private val interactor: FeeInteractor,
    private val amountFormatter: AmountFormatter
) : FeeLoaderMixinV2.Factory {

    override fun <F, D> create(
        scope: CoroutineScope,
        feeContextFlow: Flow<FeeContext>,
        feeFormatter: FeeFormatter<F, D>,
        feeInspector: FeeInspector<F>,
        configuration: FeeLoaderMixinV2.Configuration<F, D>
    ): FeeLoaderMixinV2.Presentation<F, D> {
        return FeeLoaderV2Provider(
            chainRegistry = chainRegistry,
            actionAwaitableMixinFactory = actionAwaitableMixinFactory,
            resourceManager = resourceManager,
            interactor = interactor,
            feeFormatter = feeFormatter,
            configuration = configuration,
            feeInspector = feeInspector,
            feeContextFlow = feeContextFlow,
            coroutineScope = scope,
        )
    }

    override fun <F : SubmissionFee> createDefault(
        scope: CoroutineScope,
        feeContextFlow: Flow<FeeContext>,
        configuration: FeeLoaderMixinV2.Configuration<F, FeeDisplay>
    ): FeeLoaderMixinV2.Presentation<F, FeeDisplay> {
        return create(
            scope = scope,
            feeContextFlow = feeContextFlow,
            feeFormatter = DefaultFeeFormatter(amountFormatter),
            feeInspector = DefaultFeeInspector(),
            configuration = configuration
        )
    }
}
