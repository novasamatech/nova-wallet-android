package io.novafoundation.nova.feature_crowdloan_impl.domain.main.statefull

import io.novafoundation.nova.common.presentation.firstNonEmptyLoading
import io.novafoundation.nova.common.presentation.mapLoading
import io.novafoundation.nova.common.utils.WithCoroutineScopeExtensions
import io.novafoundation.nova.common.utils.combineToPair
import io.novafoundation.nova.common.utils.withLoading
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_crowdloan_impl.domain.contributions.ContributionsInteractor
import io.novafoundation.nova.feature_crowdloan_impl.domain.main.CrowdloanInteractor
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.getCurrentAsset
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import io.novafoundation.nova.runtime.state.selectedChainFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi

class StatefulCrowdloanProviderFactory(
    private val singleAssetSharedState: SingleAssetSharedState,
    private val crowdloanInteractor: CrowdloanInteractor,
    private val contributionsInteractor: ContributionsInteractor,
    private val selectedAccountUseCase: SelectedAccountUseCase,
    private val assetUseCase: AssetUseCase,
) : StatefulCrowdloanMixin.Factory {

    override fun create(scope: CoroutineScope): StatefulCrowdloanMixin {
        return StatefulCrowdloanProvider(
            singleAssetSharedState = singleAssetSharedState,
            crowdloanInteractor = crowdloanInteractor,
            contributionsInteractor = contributionsInteractor,
            selectedAccountUseCase = selectedAccountUseCase,
            assetUseCase = assetUseCase,
            coroutineScope = scope
        )
    }
}

class StatefulCrowdloanProvider(
    singleAssetSharedState: SingleAssetSharedState,
    selectedAccountUseCase: SelectedAccountUseCase,
    private val crowdloanInteractor: CrowdloanInteractor,
    private val contributionsInteractor: ContributionsInteractor,
    private val assetUseCase: AssetUseCase,
    coroutineScope: CoroutineScope,
) : StatefulCrowdloanMixin,
    CoroutineScope by coroutineScope,
    WithCoroutineScopeExtensions by WithCoroutineScopeExtensions(coroutineScope) {

    private val selectedChain = singleAssetSharedState.selectedChainFlow()
        .shareInBackground()

    private val selectedAccount = selectedAccountUseCase.selectedMetaAccountFlow()
        .shareInBackground()

    private val chainAndAccount = combineToPair(selectedChain, selectedAccount)
        .shareInBackground()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val crowdloansIntermediateState = chainAndAccount.withLoading { (chain, account) ->
        crowdloanInteractor.crowdloansFlow(chain, account)
    }
        .shareInBackground()

    private val directContributionsIntermediateState = crowdloansIntermediateState
        .mapLoading { crowdloan ->
            crowdloan.mapNotNull { it.myContribution }
        }
        .shareInBackground()

    private val externalContributionsIntermediateState = chainAndAccount
        .withLoading { (chain, account) ->
            contributionsInteractor.externalContributionsFlow(chain, account)
        }
        .shareInBackground()

    override val groupedCrowdloansFlow = crowdloansIntermediateState
        .mapLoading {
            crowdloanInteractor.groupCrowdloans(it)
        }

    override val contributionsInfoFlow = firstNonEmptyLoading(
        directContributionsIntermediateState,
        externalContributionsIntermediateState
    )
        .mapLoading {
            val amountModel = mapAmountToAmountModel(
                contributionsInteractor.getTotalAmountOfContributions(it),
                assetUseCase.getCurrentAsset()
            )

            StatefulCrowdloanMixin.ContributionsInfo(
                contributionsCount = it.size,
                isUserHasContributions = it.isNotEmpty(),
                totalContributed = amountModel
            )
        }
}
