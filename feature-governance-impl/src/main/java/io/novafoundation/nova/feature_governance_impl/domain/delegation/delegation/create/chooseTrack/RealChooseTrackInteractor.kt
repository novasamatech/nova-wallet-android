package io.novafoundation.nova.feature_governance_impl.domain.delegation.delegation.create.chooseTrack

import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.requireIdOfSelectedMetaAccountIn
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Voting
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.delegations
import io.novafoundation.nova.feature_governance_api.data.repository.getTracksById
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegation.common.chooseTrack.ChooseTrackInteractor
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegation.common.chooseTrack.model.ChooseTrackData
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegation.common.chooseTrack.model.TrackPartition
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegation.common.chooseTrack.model.TrackPreset
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegation.common.chooseTrack.model.all
import io.novafoundation.nova.feature_governance_api.domain.referendum.track.category.TrackCategory
import io.novafoundation.nova.feature_governance_api.domain.track.Track
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.data.repository.RemoveVotesSuggestionRepository
import io.novafoundation.nova.feature_governance_impl.domain.delegation.delegation.create.chooseTrack.TrackAvailability.ALREADY_DELEGATED
import io.novafoundation.nova.feature_governance_impl.domain.delegation.delegation.create.chooseTrack.TrackAvailability.ALREADY_VOTED
import io.novafoundation.nova.feature_governance_impl.domain.delegation.delegation.create.chooseTrack.TrackAvailability.AVAILABLE
import io.novafoundation.nova.feature_governance_impl.domain.track.category.TrackCategorizer
import io.novafoundation.nova.feature_governance_impl.domain.track.mapTrackInfoToTrack
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.state.selectedOption
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private enum class TrackAvailability {
    AVAILABLE, ALREADY_VOTED, ALREADY_DELEGATED
}

class RealChooseTrackInteractor(
    private val governanceSharedState: GovernanceSharedState,
    private val governanceSourceRegistry: GovernanceSourceRegistry,
    private val chainStateRepository: ChainStateRepository,
    private val accountRepository: AccountRepository,
    private val trackCategorizer: TrackCategorizer,
    private val removeVotesSuggestionRepository: RemoveVotesSuggestionRepository,
    private val chainRegistry: ChainRegistry
) : ChooseTrackInteractor {

    override suspend fun isAllowedToShowRemoveVotesSuggestion(): Boolean {
        return removeVotesSuggestionRepository.isAllowedToShowRemoveVotesSuggestion()
    }

    override suspend fun disallowShowRemoveVotesSuggestion() {
        removeVotesSuggestionRepository.disallowShowRemoveVotesSuggestion()
    }

    override fun observeTracksByChain(chainId: ChainId): Flow<ChooseTrackData> = flowOf {
        val chain = chainRegistry.getChain(chainId)
        val govType = chain.governance.firstOrNull { it == Chain.Governance.V2 } ?: return@flowOf ChooseTrackData.empty()

        val governanceSource = governanceSourceRegistry.sourceFor(govType)
        val allTracks = governanceSource.referenda.getTracksById(chain.id)
            .mapValues { (_, track) -> mapTrackInfoToTrack(track) }
            .values
            .toList()

        ChooseTrackData(
            trackPartition = TrackPartition(
                available = allTracks,
                alreadyVoted = emptyList(),
                alreadyDelegated = emptyList(),
                preCheckedTrackIds = emptySet()
            ),
            presets = buildPresets(allTracks)
        )
    }

    override fun observeNewDelegationTrackData(): Flow<ChooseTrackData> {
        return observeChooseTrackData { voting, allTracks ->
            val tracksByAvailability = allTracks.values.groupBy { voting.newDelegationAvailabilityOf(it.id) }

            TrackPartition(
                available = tracksByAvailability.tracksThatAre(AVAILABLE),
                alreadyVoted = tracksByAvailability.tracksThatAre(ALREADY_VOTED),
                alreadyDelegated = tracksByAvailability.tracksThatAre(ALREADY_DELEGATED),
                preCheckedTrackIds = emptySet()
            )
        }
    }

    override fun observeEditDelegationTrackData(delegateId: AccountId): Flow<ChooseTrackData> {
        return observeChooseTrackData { voting, allTracks ->
            val tracksByAvailability = allTracks.values.groupBy { voting.editDelegationAvailabilityOf(it.id, delegateId) }

            TrackPartition(
                available = tracksByAvailability.tracksThatAre(AVAILABLE),
                alreadyVoted = tracksByAvailability.tracksThatAre(ALREADY_VOTED),
                alreadyDelegated = tracksByAvailability.tracksThatAre(ALREADY_DELEGATED),
                preCheckedTrackIds = voting.delegations(to = delegateId).keys
            )
        }
    }

    override fun observeRevokeDelegationTrackData(delegateId: AccountId): Flow<ChooseTrackData> {
        return observeChooseTrackData { voting, allTracks ->
            val revokableTrackIds = voting.delegations(to = delegateId).keys

            TrackPartition(
                available = revokableTrackIds.map(allTracks::getValue),
                alreadyVoted = emptyList(),
                alreadyDelegated = emptyList(),
                preCheckedTrackIds = emptySet()
            )
        }
    }

    private fun observeChooseTrackData(
        partitionConstructor: suspend (voting: Map<TrackId, Voting>, allTracks: Map<TrackId, Track>) -> TrackPartition,
    ): Flow<ChooseTrackData> {
        return flowOfAll {
            val governanceOption = governanceSharedState.selectedOption()
            val chain = governanceOption.assetWithChain.chain
            val governanceSource = governanceSourceRegistry.sourceFor(governanceOption)

            val allTracks = governanceSource.referenda.getTracksById(chain.id)
                .mapValues { (_, track) -> mapTrackInfoToTrack(track) }

            val userAccountId = accountRepository.requireIdOfSelectedMetaAccountIn(chain)

            chainStateRepository.currentBlockNumberFlow(chain.id).map {
                val userVotings = governanceSource.convictionVoting.votingFor(userAccountId, chain.id)

                val partition = partitionConstructor(userVotings, allTracks)

                val presets = buildPresets(partition.available)

                ChooseTrackData(partition, presets)
            }
        }
    }

    private fun buildPresets(tracks: List<Track>): List<TrackPreset> {
        val all = if (tracks.isNotEmpty()) TrackPreset.all(tracks) else null

        val categorized = tracks.groupBy { trackCategorizer.categoryOf(it.name) }
            .mapNotNull { (trackCategory, tracks) ->
                val presetType = mapTrackCategoryToPresetType(trackCategory)

                presetType?.let {
                    TrackPreset(
                        type = it,
                        trackIds = tracks.map(Track::id)
                    )
                }
            }

        return listOfNotNull(all) + categorized
    }

    private fun mapTrackCategoryToPresetType(trackCategory: TrackCategory): TrackPreset.Type? {
        return when (trackCategory) {
            TrackCategory.TREASURY -> TrackPreset.Type.TREASURY
            TrackCategory.GOVERNANCE -> TrackPreset.Type.GOVERNANCE
            TrackCategory.FELLOWSHIP -> TrackPreset.Type.FELLOWSHIP
            TrackCategory.OTHER -> null
        }
    }

    private fun Map<TrackId, Voting>.newDelegationAvailabilityOf(trackId: TrackId): TrackAvailability {
        return when (val voting = get(trackId)) {
            is Voting.Casting -> if (voting.votes.isEmpty()) AVAILABLE else ALREADY_VOTED
            is Voting.Delegating -> ALREADY_DELEGATED
            null -> AVAILABLE
        }
    }

    private fun Map<TrackAvailability, List<Track>>.tracksThatAre(availability: TrackAvailability): List<Track> {
        return get(availability).orEmpty()
    }

    private fun Map<TrackId, Voting>.editDelegationAvailabilityOf(trackId: TrackId, delegateId: AccountId): TrackAvailability {
        return when (val voting = get(trackId)) {
            is Voting.Casting -> if (voting.votes.isEmpty()) AVAILABLE else ALREADY_VOTED
            is Voting.Delegating -> if (voting.target.contentEquals(delegateId)) AVAILABLE else ALREADY_DELEGATED
            null -> AVAILABLE
        }
    }
}
