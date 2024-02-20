package io.novafoundation.nova.feature_push_notifications.data.domain.interactor

import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_push_notifications.data.data.PushNotificationsService
import io.novafoundation.nova.feature_push_notifications.data.data.settings.PushSettingsProvider
import io.novafoundation.nova.runtime.ext.defaultComparatorFrom
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ChainWithGovTracks(val chain: Chain, val govVersion: Chain.Governance, val tracksQuantity: Int)

interface GovernancePushSettingsInteractor {

    fun governanceChainsFlow(): Flow<List<ChainWithGovTracks>>
}

class RealGovernancePushSettingsInteractor(
    private val pushNotificationsService: PushNotificationsService,
    private val pushSettingsProvider: PushSettingsProvider,
    private val chainRegistry: ChainRegistry,
    private val governanceSourceRegistry: GovernanceSourceRegistry
) : GovernancePushSettingsInteractor {

    override fun governanceChainsFlow(): Flow<List<ChainWithGovTracks>> {
        return chainRegistry.currentChains
            .map {
                it.filter { it.governance.isNotEmpty() }
                    .flatMap { chain -> chain.governance.map { chain to it } }
                    .map { (chain, govType) -> ChainWithGovTracks(chain, govType, getTracksQuantity(chain, govType)) }
                    .sortedWith(Chain.defaultComparatorFrom(ChainWithGovTracks::chain))
            }
    }

    private suspend fun getTracksQuantity(chain: Chain, governance: Chain.Governance): Int {
        return governanceSourceRegistry.sourceFor(governance)
            .referenda
            .getTracks(chain.id)
            .size
    }
}
