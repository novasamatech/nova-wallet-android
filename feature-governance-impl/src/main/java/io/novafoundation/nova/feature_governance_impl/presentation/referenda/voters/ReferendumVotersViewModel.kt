package io.novafoundation.nova.feature_governance_impl.presentation.referenda.voters

import android.text.TextUtils
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.firstOnLoad
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.utils.toggle
import io.novafoundation.nova.common.utils.withLoading
import io.novafoundation.nova.feature_account_api.domain.account.identity.Identity
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VoteType
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.label.DelegateLabel
import io.novafoundation.nova.feature_governance_api.domain.referendum.voters.ReferendumVoter
import io.novafoundation.nova.feature_governance_api.domain.referendum.voters.ReferendumVotersInteractor
import io.novafoundation.nova.feature_governance_api.presentation.referenda.voters.ReferendumVotersPayload
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.common.voters.VoteModel
import io.novafoundation.nova.feature_governance_impl.presentation.common.voters.VotersFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.common.voters.formatConvictionVote
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.DelegateMappers
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.voters.list.DelegatorVoterRVItem
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.voters.list.ExpandableVoterRVItem
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.voters.list.VoterRvItem
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.state.chain
import io.novafoundation.nova.runtime.state.chainAsset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ReferendumVotersViewModel(
    private val payload: ReferendumVotersPayload,
    private val router: GovernanceRouter,
    private val governanceSharedState: GovernanceSharedState,
    private val externalActions: ExternalActions.Presentation,
    private val referendumVotersInteractor: ReferendumVotersInteractor,
    private val resourceManager: ResourceManager,
    private val votersFormatter: VotersFormatter,
    private val delegateMappers: DelegateMappers
) : BaseViewModel(), ExternalActions by externalActions {

    private val chainFlow = flowOf { governanceSharedState.chain() }
        .shareInBackground()
    private val chainAssetFlow = flowOf { governanceSharedState.chainAsset() }
        .shareInBackground()

    private val voterList = flowOfAll {
        val referendumId = ReferendumId(payload.referendumId)
        referendumVotersInteractor.votersFlow(referendumId, payload.voteType)
    }.shareInBackground()

    val title: String = mapTypeToString(payload.voteType)

    private val expandedVotersFlow: MutableStateFlow<Set<Int>> = MutableStateFlow(setOf())

    val voterModels = combine(voterList, expandedVotersFlow) { voters, expandedVoters ->
        val chain = chainFlow.first()
        val chainAsset = chainAssetFlow.first()
        mapVotersToVoterModels(chain, chainAsset, voters, expandedVoters)
    }
        .withLoading()
        .shareInBackground()

    val votersCount = voterList.map { it.size.format() }
        .shareInBackground()

    fun backClicked() {
        router.back()
    }

    fun expandVoterClicked(position: Int) = launch {
        val voters = voterModels.firstOnLoad()
        val voterItem = voters[position] as? ExpandableVoterRVItem ?: return@launch
        expandedVotersFlow.value = expandedVotersFlow.value.toggle(voterItem.primaryIndex)
    }

    fun voterClicked(position: Int) = launch {
        val voters = voterModels.firstOnLoad()
        val voterItem = voters[position]
        val chain = chainFlow.first()
        val type = ExternalActions.Type.Address(voterItem.metadata.address)
        externalActions.showExternalActions(type, chain)
    }

    private suspend fun mapVotersToVoterModels(
        chain: Chain,
        chainAsset: Chain.Asset,
        voters: List<ReferendumVoter>,
        expandedVoters: Set<Int>
    ): List<VoterRvItem> {
        return buildList {
            voters.forEachIndexed { index, referendumVoter ->
                val isExpandable = referendumVoter.vote is ReferendumVoter.Vote.WithDelegators
                val isExpanded = index in expandedVoters

                add(mapReferendumVoterToExpandableRvItem(index, referendumVoter, chain, chainAsset, isExpandable, isExpanded))

                if (isExpandable && isExpanded) {
                    addAll(mapVoterDelegatorsToRvItem(referendumVoter, chain, chainAsset))
                }
            }
        }
    }

    private suspend fun mapReferendumVoterToExpandableRvItem(
        index: Int,
        referendumVoter: ReferendumVoter,
        chain: Chain,
        chainAsset: Chain.Asset,
        isExpandable: Boolean,
        isExpanded: Boolean
    ): ExpandableVoterRVItem {
        val voteModel = when (val vote = referendumVoter.vote) {
            is ReferendumVoter.Vote.OnlySelf -> votersFormatter.formatConvictionVote(vote.selfVote, chainAsset)
            is ReferendumVoter.Vote.WithDelegators -> VoteModel(
                votesCount = votersFormatter.formatTotalVotes(vote),
                votesCountDetails = null
            )
        }

        return ExpandableVoterRVItem(
            primaryIndex = index,
            vote = voteModel,
            metadata = delegateMappers.formatDelegateLabel(
                accountId = referendumVoter.accountId,
                metadata = referendumVoter.metadata,
                identityName = referendumVoter.identity?.name,
                chain = chain
            ),
            isExpandable = isExpandable,
            isExpanded = isExpanded,
            addressEllipsize = mapAddressEllipsize(referendumVoter.metadata, referendumVoter.identity)
        )
    }

    private suspend fun mapVoterDelegatorsToRvItem(referendumVoter: ReferendumVoter, chain: Chain, chainAsset: Chain.Asset): List<DelegatorVoterRVItem> {
        val vote = referendumVoter.vote
        if (vote !is ReferendumVoter.Vote.WithDelegators) return emptyList()

        return vote.delegators.map {
            DelegatorVoterRVItem(
                vote = votersFormatter.formatConvictionVote(it.vote, chainAsset),
                metadata = delegateMappers.formatDelegateLabel(
                    accountId = it.accountId,
                    metadata = it.metadata,
                    identityName = it.identity?.name,
                    chain = chain
                ),
                addressEllipsize = mapAddressEllipsize(it.metadata, it.identity)
            )
        }
    }

    private fun mapAddressEllipsize(metadata: DelegateLabel.Metadata?, identity: Identity?): TextUtils.TruncateAt {
        return if (metadata?.name != null || identity?.name != null) {
            TextUtils.TruncateAt.END
        } else {
            TextUtils.TruncateAt.MIDDLE
        }
    }

    private fun mapTypeToString(voteType: VoteType): String {
        return if (voteType == VoteType.AYE) {
            resourceManager.getString(R.string.referendum_positive_voters_title)
        } else {
            resourceManager.getString(R.string.referendum_negative_voters_title)
        }
    }
}
