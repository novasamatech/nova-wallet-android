package io.novafoundation.nova.feature_governance_impl.domain.delegation.delegation.create.chooseTrack

import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.requireIdOfSelectedMetaAccountIn
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Voting
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_governance_api.data.source.SupportedGovernanceOption
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegation.create.chooseTrack.NewDelegationChooseTrackInteractor
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegation.create.chooseTrack.model.ChooseTrackData
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegation.create.chooseTrack.model.TrackPartition
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegation.create.chooseTrack.model.TrackPreset
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegation.create.chooseTrack.model.all
import io.novafoundation.nova.feature_governance_api.domain.referendum.track.category.TrackCategory
import io.novafoundation.nova.feature_governance_api.domain.track.Track
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.domain.delegation.delegation.create.chooseTrack.TrackAvailability.ALREADY_DELEGATED
import io.novafoundation.nova.feature_governance_impl.domain.delegation.delegation.create.chooseTrack.TrackAvailability.ALREADY_VOTED
import io.novafoundation.nova.feature_governance_impl.domain.delegation.delegation.create.chooseTrack.TrackAvailability.AVAILABLE
import io.novafoundation.nova.feature_governance_impl.domain.track.category.TrackCategorizer
import io.novafoundation.nova.feature_governance_impl.domain.track.mapTrackInfoToTrack
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.state.selectedOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private enum class TrackAvailability {
    AVAILABLE, ALREADY_VOTED, ALREADY_DELEGATED
}

class RealNewDelegationChooseTrackInteractor(
    private val governanceSharedState: GovernanceSharedState,
    private val governanceSourceRegistry: GovernanceSourceRegistry,
    private val chainStateRepository: ChainStateRepository,
    private val accountRepository: AccountRepository,
    private val trackCategorizer: TrackCategorizer,
) : NewDelegationChooseTrackInteractor {

    override fun observeChooseTrackData(): Flow<ChooseTrackData> {
        return flowOfAll {
            val selectedGovernanceOption = governanceSharedState.selectedOption()

            observeChooseTrackData(selectedGovernanceOption)
        }
    }

    private suspend fun observeChooseTrackData(governanceOption: SupportedGovernanceOption): Flow<ChooseTrackData> {
        val chain = governanceOption.assetWithChain.chain
        val governanceSource = governanceSourceRegistry.sourceFor(governanceOption)
        val allTracks = governanceSource.referenda.getTracks(chain.id)
            .map { mapTrackInfoToTrack(it) }
        val userAccountId = accountRepository.requireIdOfSelectedMetaAccountIn(chain)

        return chainStateRepository.currentBlockNumberFlow(chain.id).map {
            val userVotings = governanceSource.convictionVoting.votingFor(userAccountId, chain.id)
            val tracksByAvailability = allTracks.groupBy { userVotings.availabilityOf(it.id) }

            val availableTracks = tracksByAvailability.tracksThatAre(AVAILABLE)
            val presets = buildPresets(availableTracks)

            ChooseTrackData(
                trackPartition = TrackPartition(
                    available = availableTracks,
                    alreadyVoted = tracksByAvailability.tracksThatAre(ALREADY_VOTED),
                    alreadyDelegated = tracksByAvailability.tracksThatAre(ALREADY_DELEGATED)
                ),
                presets = presets
            )
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

    private fun Map<TrackId, Voting>.availabilityOf(trackId: TrackId): TrackAvailability {
        return when (val voting = get(trackId)) {
            is Voting.Casting -> if (voting.votes.isEmpty()) AVAILABLE else ALREADY_VOTED
            is Voting.Delegating -> ALREADY_DELEGATED
            null -> AVAILABLE
        }
    }

    private fun Map<TrackAvailability, List<Track>>.tracksThatAre(availability: TrackAvailability): List<Track> {
        return get(availability).orEmpty()
    }
}
