package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.delegators

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.domain.mapLoading
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.utils.withSafeLoading
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.delegators.DelegateDelegatorsInteractor
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.delegators.model.Delegator
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.DelegateMappers
import io.novafoundation.nova.feature_governance_impl.presentation.common.voters.VoterModel
import io.novafoundation.nova.feature_governance_impl.presentation.common.voters.VotersFormatter
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.state.chain
import io.novafoundation.nova.runtime.state.chainAsset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DelegateDelegatorsViewModel(
    payload: DelegateDelegatorsPayload,
    interactor: DelegateDelegatorsInteractor,
    private val router: GovernanceRouter,
    private val governanceSharedState: GovernanceSharedState,
    private val externalActions: ExternalActions.Presentation,
    private val votersFormatter: VotersFormatter,
    private val delegateMappers: DelegateMappers,
) : BaseViewModel(), ExternalActions by externalActions {

    private val chainFlow = flowOf { governanceSharedState.chain() }
    private val chainAssetFlow = flowOf { governanceSharedState.chainAsset() }

    private val delegatorsList = interactor.delegatorsFlow(payload.delegateId)
        .withSafeLoading()
        .shareInBackground()

    val delegatorsCount = delegatorsList.mapLoading {
        it.size.format()
    }.shareInBackground()

    val delegatorModels = delegatorsList.mapLoading { delegators ->
        val chain = chainFlow.first()
        val chainAsset = chainAssetFlow.first()
        mapDelegatorsToDelegatorModels(chain, chainAsset, delegators)
    }
        .shareInBackground()

    fun backClicked() {
        router.back()
    }

    fun delegatorClicked(voter: VoterModel) = launch {
        val chain = chainFlow.first()
        externalActions.showAddressActions(voter.addressModel.address, chain)
    }

    private suspend fun mapDelegatorsToDelegatorModels(chain: Chain, chainAsset: Chain.Asset, voters: List<Delegator>): List<VoterModel> {
        return voters.map { delegator ->
            val delegationModel = delegateMappers.formatDelegationsOverview(delegator.vote, chainAsset)
            votersFormatter.formatVoter(delegator, chain, chainAsset, delegationModel)
        }
    }
}
