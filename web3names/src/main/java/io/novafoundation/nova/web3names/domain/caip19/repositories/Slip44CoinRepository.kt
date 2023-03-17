package io.novafoundation.nova.web3names.domain.caip19.repositories

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

interface Slip44CoinRepository {
    fun getCoinCode(chainAsset: Chain.Asset): Int
}
