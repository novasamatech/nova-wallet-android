package io.novafoundation.nova.feature_push_notifications.data.domain.interactor

import io.novafoundation.nova.common.utils.mapToSet
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.runtime.ext.defaultComparatorFrom
import io.novafoundation.nova.runtime.ext.openGovIfSupported
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ChainWithGovTracks(
    val chain: Chain,
    val govVersion: Chain.Governance,
    val tracks: Set<TrackId>
)

interface GovernancePushSettingsInteractor {

    fun governanceChainsFlow(): Flow<List<ChainWithGovTracks>>
}

class RealGovernancePushSettingsInteractor(
    private val chainRegistry: ChainRegistry,
    private val governanceSourceRegistry: GovernanceSourceRegistry
) : GovernancePushSettingsInteractor {

    override fun governanceChainsFlow(): Flow<List<ChainWithGovTracks>> {
        return chainRegistry.currentChains
            .map {
                it.filter { it.pushSupport }
                    .flatMap { it.supportedGovTypes() }
                    .map { (chain, govType) -> ChainWithGovTracks(chain, govType, getTrackIds(chain, govType)) }
                    .sortedWith(Chain.defaultComparatorFrom(ChainWithGovTracks::chain))
            }
    }

    private fun Chain.supportedGovTypes(): List<Pair<Chain, Chain.Governance>> {
        return openGovIfSupported()?.let {
            listOf(this to it)
        }.orEmpty()
    }

    private suspend fun getTrackIds(chain: Chain, governance: Chain.Governance): Set<TrackId> {
        return governanceSourceRegistry.sourceFor(governance)
            .referenda
            .getTracks(chain.id)
            .mapToSet { it.id }
    }
}
