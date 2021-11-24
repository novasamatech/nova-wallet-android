package io.novafoundation.nova.feature_crowdloan_impl.domain.main.statefull

import io.novafoundation.nova.common.presentation.combineLoading
import io.novafoundation.nova.common.presentation.mapLoading
import io.novafoundation.nova.common.utils.WithCoroutineScopeExtensions
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.withLoading
import io.novafoundation.nova.feature_crowdloan_impl.domain.main.CrowdloanInteractor
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import io.novafoundation.nova.runtime.state.selectedChainFlow
import kotlinx.coroutines.CoroutineScope

class StatefulCrowdloanProviderFactory(
    private val singleAssetSharedState: SingleAssetSharedState,
    private val interactor: CrowdloanInteractor,
) : StatefulCrowdloanMixin.Factory {

    override fun create(scope: CoroutineScope): StatefulCrowdloanMixin {
        return StatefulCrowdloanProvider(
            singleAssetSharedState = singleAssetSharedState,
            interactor = interactor,
            coroutineScope = scope
        )
    }
}

class StatefulCrowdloanProvider(
    singleAssetSharedState: SingleAssetSharedState,
    private val interactor: CrowdloanInteractor,
    coroutineScope: CoroutineScope,
) : StatefulCrowdloanMixin,
    CoroutineScope by coroutineScope,
    WithCoroutineScopeExtensions by WithCoroutineScopeExtensions(coroutineScope) {

    private val selectedChain = singleAssetSharedState.selectedChainFlow()
        .inBackground()
        .share()

    private val crowdloansIntermediateState = selectedChain.withLoading(interactor::crowdloansFlow)
        .inBackground()
        .share()

    private val externalContributionsIntermediateState = selectedChain.withLoading(interactor::externalContributions)
        .inBackground()
        .share()

    override val groupedCrowdloansFlow = crowdloansIntermediateState.mapLoading {
        interactor.groupCrowdloans(it)
    }
        .inBackground()
        .share()

    override val allUserContributions = combineLoading(
        crowdloansIntermediateState,
        externalContributionsIntermediateState,
        interactor::allUserContributions
    )
        .inBackground()
        .share()
}
