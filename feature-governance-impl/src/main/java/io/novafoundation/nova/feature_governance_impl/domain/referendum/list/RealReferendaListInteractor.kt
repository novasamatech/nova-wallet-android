package io.novafoundation.nova.feature_governance_impl.domain.referendum.list

import io.novafoundation.nova.common.utils.SearchComparator
import io.novafoundation.nova.common.utils.SearchFilter
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.common.utils.filterWith
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Voting
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_governance_api.data.source.SupportedGovernanceOption
import io.novafoundation.nova.feature_governance_api.data.source.trackLocksFlowOrEmpty
import io.novafoundation.nova.feature_governance_api.domain.locks.ClaimScheduleCalculator
import io.novafoundation.nova.feature_governance_api.domain.locks.RealClaimScheduleCalculator
import io.novafoundation.nova.feature_governance_api.domain.locks.hasClaimableLocks
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.DelegatedState
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.GovernanceLocksOverview
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendaListInteractor
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendaListState
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumGroup
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumPreview
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumStatus
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.Voter
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.getName
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.user
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.domain.referendum.list.repository.ReferendaCommonRepository
import io.novafoundation.nova.feature_governance_impl.domain.referendum.list.sorting.ReferendaSortingProvider
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.state.selectedOption
import jp.co.soramitsu.fearless_utils.hash.isPositive
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class RealReferendaListInteractor(
    private val governanceSharedState: GovernanceSharedState,
    private val referendaCommonRepository: ReferendaCommonRepository,
    private val referendaSharedComputation: ReferendaSharedComputation,
    private val governanceSourceRegistry: GovernanceSourceRegistry,
    private val referendaSortingProvider: ReferendaSortingProvider,
) : ReferendaListInteractor {

    override fun searchReferendaListStateFlow(
        queryFlow: Flow<String>,
        voterAccountId: AccountId?,
        selectedGovernanceOption: SupportedGovernanceOption,
        coroutineScope: CoroutineScope
    ): Flow<List<ReferendumPreview>> {
        return flowOfAll {
            combine(
                queryFlow,
                referendaSharedComputation.referenda(voterAccountId?.let(Voter.Companion::user), selectedGovernanceOption, coroutineScope)
            ) { query, referendaState ->
                val referenda = referendaState.referenda

                if (query.isEmpty()) {
                    val sorting = referendaSortingProvider.getReferendumSorting()

                    return@combine referenda.sortedWith(sorting)
                }

                val lowercaseQuery = query.lowercase()

                val searchFilter = SearchFilter.Builder<ReferendumPreview>(lowercaseQuery) { it.getName()?.lowercase() }
                    .separatedWordSearch(true)
                    .and { it.id.toString() }
                    .build()

                val searchComparator = SearchComparator.Builder<ReferendumPreview>(lowercaseQuery) { it.getName()?.lowercase() }
                    .separatedWordSearch(true)
                    .and { it.id.toString() }
                    .build()

                referenda.filterWith(searchFilter)
                    .sortedWith(searchComparator)
            }
        }
    }

    override fun referendaListStateFlow(
        voterAccountId: AccountId?,
        selectedGovernanceOption: SupportedGovernanceOption,
        coroutineScope: CoroutineScope
    ): Flow<ReferendaListState> {
        return flowOfAll {
            val voter = voterAccountId?.let(Voter.Companion::user)
            val referendaStateFlow = referendaSharedComputation.referenda(
                voter,
                selectedGovernanceOption,
                coroutineScope
            )

            val chain = selectedGovernanceOption.assetWithChain.chain
            val asset = selectedGovernanceOption.assetWithChain.asset
            val governanceSource = governanceSourceRegistry.sourceFor(selectedGovernanceOption)
            val undecidingTimeout = governanceSource.referenda.undecidingTimeout(chain.id)
            val voteLockingPeriod = governanceSource.convictionVoting.voteLockingPeriod(chain.id)
            val delegationSupported = governanceSource.delegationsRepository.isDelegationSupported(chain)

            val trackLocksFlow = governanceSource.convictionVoting.trackLocksFlowOrEmpty(voter?.accountId, asset.fullId)

            combine(referendaStateFlow, trackLocksFlow) { intermediateData, trackLocks ->
                val claimScheduleCalculator = with(intermediateData) {
                    RealClaimScheduleCalculator(voting, currentBlockNumber, onChainReferenda, tracksById, undecidingTimeout, voteLockingPeriod, trackLocks)
                }
                val locksOverview = claimScheduleCalculator.governanceLocksOverview()

                ReferendaListState(
                    groupedReferenda = sortReferendaPreviews(intermediateData.referenda),
                    locksOverview = locksOverview,
                    delegated = determineDelegatedState(intermediateData.voting, delegationSupported),
                )
            }
        }
    }

    private fun ReferendumPreview.group(): ReferendumGroup {
        return when (status) {
            is ReferendumStatus.Executed,
            is ReferendumStatus.Approved,
            is ReferendumStatus.NotExecuted -> ReferendumGroup.COMPLETED

            else -> ReferendumGroup.ONGOING
        }
    }

    private suspend fun sortReferendaPreviews(referenda: List<ReferendumPreview>) =
        referenda.groupBy { it.group() }
            .mapValues { (group, referenda) ->
                val sorting = referendaSortingProvider.getReferendumSorting(group)

                referenda.sortedWith(sorting)
            }.toSortedMap(referendaSortingProvider.getGroupSorting())

    private fun ClaimScheduleCalculator.governanceLocksOverview(): GovernanceLocksOverview? {
        val totalLock = totalGovernanceLock()

        return if (totalLock.isPositive()) {
            val claimableSchedule = estimateClaimSchedule()

            GovernanceLocksOverview(
                locked = totalLock,
                hasClaimableLocks = claimableSchedule.hasClaimableLocks()
            )
        } else {
            null
        }
    }

    private fun determineDelegatedState(voting: Map<TrackId, Voting>, delegationsSupported: Boolean): DelegatedState {
        if (!delegationsSupported) return DelegatedState.DelegationNotSupported

        val delegatedAmount = voting.values.filterIsInstance<Voting.Delegating>()
            .maxOfOrNull { it.amount }

        return if (delegatedAmount != null) {
            DelegatedState.Delegated(delegatedAmount)
        } else {
            DelegatedState.NotDelegated
        }
    }

    override fun votedReferendaListFlow(voter: Voter, onlyRecentVotes: Boolean): Flow<List<ReferendumPreview>> {
        return flowOfAll {
            val selectedGovernanceOption = governanceSharedState.selectedOption()

            referendaCommonRepository.referendaListFlow(voter, selectedGovernanceOption, onlyRecentVotes)
        }
    }
}
