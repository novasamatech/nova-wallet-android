package io.novafoundation.nova.feature_crowdloan_impl.domain.main.statefull

import io.novafoundation.nova.common.presentation.mapLoading
import io.novafoundation.nova.common.utils.WithCoroutineScopeExtensions
import io.novafoundation.nova.common.utils.combineToPair
import io.novafoundation.nova.common.utils.withLoading
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_crowdloan_api.domain.contributions.ContributionsInteractor
import io.novafoundation.nova.feature_crowdloan_impl.domain.main.CrowdloanInteractor
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.getCurrentAsset
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.formatAmountToAmountModel
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import io.novafoundation.nova.runtime.state.selectedChainFlow
import kotlinx.coroutines.CoroutineScope

class StatefulCrowdloanProviderFactory(
    private val singleAssetSharedState: SingleAssetSharedState,
    private val crowdloanInteractor: CrowdloanInteractor,
    private val contributionsInteractor: ContributionsInteractor,
    private val selectedAccountUseCase: SelectedAccountUseCase,
    private val amountFormatter: AmountFormatter,
    private val assetUseCase: AssetUseCase,
) : StatefulCrowdloanMixin.Factory {

    override fun create(scope: CoroutineScope): StatefulCrowdloanMixin {
        return StatefulCrowdloanProvider(
            singleAssetSharedState = singleAssetSharedState,
            crowdloanInteractor = crowdloanInteractor,
            contributionsInteractor = contributionsInteractor,
            selectedAccountUseCase = selectedAccountUseCase,
            assetUseCase = assetUseCase,
            amountFormatter = amountFormatter,
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
    private val amountFormatter: AmountFormatter,
) : StatefulCrowdloanMixin,
    CoroutineScope by coroutineScope,
    WithCoroutineScopeExtensions by WithCoroutineScopeExtensions(coroutineScope) {

    override val selectedChain = singleAssetSharedState.selectedChainFlow()
        .shareInBackground()

    override val selectedAccount = selectedAccountUseCase.selectedMetaAccountFlow()
        .shareInBackground()

    private val chainAndAccount = combineToPair(selectedChain, selectedAccount)
        .shareInBackground()

    override val groupedCrowdloansFlow = chainAndAccount.withLoading { (chain, account) ->
        crowdloanInteractor.groupedCrowdloansFlow(chain, account)
    }
        .shareInBackground()

    override val contributionsInfoFlow = chainAndAccount.withLoading { (chain, account) ->
        contributionsInteractor.observeChainContributions(account, chain.id, chain.utilityAsset.id)
    }
        .mapLoading {
            val amountModel = amountFormatter.formatAmountToAmountModel(
                it.totalContributed,
                assetUseCase.getCurrentAsset()
            )

            StatefulCrowdloanMixin.ContributionsInfo(
                contributionsCount = it.contributions.size,
                isUserHasContributions = it.contributions.isNotEmpty(),
                totalContributed = amountModel
            )
        }
}
