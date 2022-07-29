package io.novafoundation.nova.feature_crowdloan_impl.domain.main.statefull

import io.novafoundation.nova.common.presentation.combineLoading
import io.novafoundation.nova.common.presentation.mapLoading
import io.novafoundation.nova.common.utils.WithCoroutineScopeExtensions
import io.novafoundation.nova.common.utils.combineToPair
import io.novafoundation.nova.common.utils.withLoading
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_crowdloan_impl.domain.main.CrowdloanInteractor
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import io.novafoundation.nova.runtime.state.selectedChainFlow
import kotlinx.coroutines.CoroutineScope

class StatefulCrowdloanProviderFactory(
    private val singleAssetSharedState: SingleAssetSharedState,
    private val interactor: CrowdloanInteractor,
    private val selectedAccountUseCase: SelectedAccountUseCase,
) : StatefulCrowdloanMixin.Factory {

    override fun create(scope: CoroutineScope): StatefulCrowdloanMixin {
        return StatefulCrowdloanProvider(
            singleAssetSharedState = singleAssetSharedState,
            interactor = interactor,
            selectedAccountUseCase = selectedAccountUseCase,
            coroutineScope = scope
        )
    }
}

class StatefulCrowdloanProvider(
    singleAssetSharedState: SingleAssetSharedState,
    selectedAccountUseCase: SelectedAccountUseCase,
    private val interactor: CrowdloanInteractor,
    coroutineScope: CoroutineScope,
) : StatefulCrowdloanMixin,
    CoroutineScope by coroutineScope,
    WithCoroutineScopeExtensions by WithCoroutineScopeExtensions(coroutineScope) {

    private val selectedChain = singleAssetSharedState.selectedChainFlow()
        .shareInBackground()

    private val selectedAccount = selectedAccountUseCase.selectedMetaAccountFlow()
        .shareInBackground()

    private val chainAndAccount = combineToPair(selectedChain, selectedAccount)

    private val crowdloansIntermediateState = chainAndAccount.withLoading { (chain, account) ->
        interactor.crowdloansFlow(chain, account)
    }

    private val externalContributionsIntermediateState = chainAndAccount.withLoading { (chain, account) ->
        interactor.externalContributions(chain, account)
    }
        .shareInBackground()

    override val groupedCrowdloansFlow = crowdloansIntermediateState.mapLoading {
        interactor.groupCrowdloans(it)
    }
        .shareInBackground()

    override val allUserContributions = combineLoading(
        crowdloansIntermediateState,
        externalContributionsIntermediateState,
        interactor::allUserContributions
    )
        .shareInBackground()
}
