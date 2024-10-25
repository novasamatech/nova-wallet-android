package io.novafoundation.nova.caip.slip44

import io.novafoundation.nova.caip.slip44.endpoint.Slip44CoinApi
import io.novafoundation.nova.caip.slip44.endpoint.Slip44CoinRemote
import io.novafoundation.nova.runtime.ext.normalizeSymbol
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface Slip44CoinRepository {

    suspend fun getCoinCode(chainAsset: Chain.Asset): Int?
}

internal class RealSlip44CoinRepository(
    private val slip44Api: Slip44CoinApi,
    private val slip44CoinsUrl: String
) : Slip44CoinRepository {

    private var slip44Coins: Map<String, Slip44CoinRemote> = emptyMap()
    private val mutex = Mutex()

    override suspend fun getCoinCode(chainAsset: Chain.Asset): Int? = mutex.withLock {
        if (slip44Coins.isEmpty()) {
            slip44Coins = slip44Api.getSlip44Coins(slip44CoinsUrl)
                .associateBy { it.symbol }
        }

        return slip44Coins[chainAsset.normalizeSymbol()]
            ?.index
            ?.toIntOrNull()
    }
}
