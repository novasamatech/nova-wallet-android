package io.novafoundation.nova.feature_account_api.domain.updaters

import io.novafoundation.nova.core.updater.UpdateScope
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow

class ChainUpdateScope(
    private val chainFlow: Flow<Chain>
) : UpdateScope<Chain> {

    override fun invalidationFlow(): Flow<Chain> {
        return chainFlow
    }
}
