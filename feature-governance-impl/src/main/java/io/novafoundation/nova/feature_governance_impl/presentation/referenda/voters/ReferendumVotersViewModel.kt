package io.novafoundation.nova.feature_governance_impl.presentation.referenda.voters

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.utils.withLoading
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createIdentityAddressModel
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VoteType
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.votes
import io.novafoundation.nova.feature_governance_api.domain.referendum.voters.ReferendumVoter
import io.novafoundation.nova.feature_governance_api.domain.referendum.voters.ReferendumVotersInteractor
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.domain.identity.GovernanceIdentityProviderFactory
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.voters.model.VoterModel
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatTokenAmount
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.state.chain
import io.novafoundation.nova.runtime.state.chainAsset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ReferendumVotersViewModel(
    private val payload: ReferendumVotersPayload,
    private val router: GovernanceRouter,
    private val governanceSharedState: GovernanceSharedState,
    private val externalActions: ExternalActions.Presentation,
    private val addressIconGenerator: AddressIconGenerator,
    private val identityProviderFactory: GovernanceIdentityProviderFactory,
    private val referendumVotersInteractor: ReferendumVotersInteractor,
    private val resourceManager: ResourceManager
) : BaseViewModel(), ExternalActions by externalActions {

    private val chainFlow = flowOf { governanceSharedState.chain() }
    private val chainAssetFlow = flowOf { governanceSharedState.chainAsset() }

    private val voterList = chainFlow.flatMapLatest {
        val referendumId = ReferendumId(payload.referendumId)
        referendumVotersInteractor.votersFlow(referendumId, it, payload.voteType)
    }

    private val defaultIdentityProvider = identityProviderFactory.defaultProvider()

    val title: String = mapTypeToString(payload.voteType)

    val voterModels = voterList.map { voters ->
        val chain = chainFlow.first()
        val chainAsset = chainAssetFlow.first()
        mapVotersToVoterModels(chain, chainAsset, voters)
    }
        .withLoading()
        .shareInBackground()

    fun backClicked() {
        router.back()
    }

    fun voterClicked(voter: VoterModel) = launch {
        val chain = chainFlow.first()
        val type = ExternalActions.Type.Address(voter.addressModel.address)
        externalActions.showExternalActions(type, chain)
    }

    private suspend fun mapVotersToVoterModels(chain: Chain, chainAsset: Chain.Asset, voters: List<ReferendumVoter>): List<VoterModel> {
        return voters.map { mapVoterToVoterModel(chain, chainAsset, it) }
    }

    private suspend fun mapVoterToVoterModel(chain: Chain, chainAsset: Chain.Asset, voter: ReferendumVoter): VoterModel {
        val addressModel = addressIconGenerator.createIdentityAddressModel(
            chain,
            voter.accountId,
            defaultIdentityProvider
        )
        val votesAmount = voter.vote.votes(chainAsset)!!
        val amount = votesAmount.amount.formatTokenAmount(chainAsset)
        val multiplier = votesAmount.multiplier
        val total = votesAmount.total.format()
        return VoterModel(
            addressModel,
            resourceManager.getString(R.string.referendum_voter_vote, total),
            resourceManager.getString(R.string.referendum_voter_vote_details, amount, multiplier)
        )
    }

    private fun mapTypeToString(voteType: VoteType): String {
        return if (voteType == VoteType.AYE) {
            resourceManager.getString(R.string.referendum_positive_voters_title)
        } else {
            resourceManager.getString(R.string.referendum_negative_voters_title)
        }
    }
}
