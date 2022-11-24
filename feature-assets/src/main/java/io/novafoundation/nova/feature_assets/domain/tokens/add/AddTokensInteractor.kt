package io.novafoundation.nova.feature_assets.domain.tokens.add

import io.novafoundation.nova.runtime.ext.defaultComparator
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface AddTokensInteractor {

    fun availableChainsToAddTokenFlow(): Flow<List<Chain>>
}

class RealAddTokensInteractor(
    private val chainRegistry: ChainRegistry
) : AddTokensInteractor {

    override fun availableChainsToAddTokenFlow(): Flow<List<Chain>> {
        return chainRegistry.currentChains.map { chains ->
            chains.filter { it.isEthereumBased }
                .sortedWith(Chain.defaultComparator())
        }
    }
}
