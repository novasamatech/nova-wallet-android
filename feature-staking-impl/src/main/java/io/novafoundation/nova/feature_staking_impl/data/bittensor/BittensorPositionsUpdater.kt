package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.updaters.AccountUpdateScope
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.network.updaters.multiChain.SharedStateBasedUpdater
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class BittensorPositionsUpdater(
    override val scope: AccountUpdateScope,
    private val chainRegistry: ChainRegistry,
    private val positionRepository: BittensorPositionRepository
) : SharedStateBasedUpdater<MetaAccount> {

    override suspend fun listenForUpdates(
        storageSubscriptionBuilder: SharedRequestsBuilder,
        scopeValue: MetaAccount
    ): Flow<Updater.SideEffect> {
        val chain = chainRegistry.getChain(ChainGeneses.BITTENSOR)
        val coldKey = scopeValue.accountIdIn(chain) ?: return emptyFlow()

        return positionRepository.observePositions(chain.id, coldKey)
            .noSideAffects()
    }
}
