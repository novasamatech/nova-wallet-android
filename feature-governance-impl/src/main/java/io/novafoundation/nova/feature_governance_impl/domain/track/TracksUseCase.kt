package io.novafoundation.nova.feature_governance_impl.domain.track

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_governance_api.domain.track.Track
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.runtime.state.selectedOption

interface TracksUseCase {

    suspend fun tracksOf(trackIds: Collection<TrackId>): List<Track>
}

suspend fun TracksUseCase.tracksByIdOf(trackIds: Collection<TrackId>) = tracksOf(trackIds).associateBy { it.id }

class RealTracksUseCase(
    private val governanceSharedState: GovernanceSharedState,
    private val governanceSourceRegistry: GovernanceSourceRegistry,
) : TracksUseCase {

    override suspend fun tracksOf(trackIds: Collection<TrackId>): List<Track> {
        val option = governanceSharedState.selectedOption()
        val source = governanceSourceRegistry.sourceFor(option)
        val chain = option.assetWithChain.chain

        val trackIdsSet = trackIds.toSet()

        return source.referenda.getTracks(chain.id)
            .filter { it.id in trackIdsSet }
            .map(::mapTrackInfoToTrack)
    }
}
