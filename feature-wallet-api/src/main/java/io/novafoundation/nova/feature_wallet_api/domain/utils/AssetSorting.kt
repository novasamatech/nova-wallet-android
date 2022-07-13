package io.novafoundation.nova.feature_wallet_api.domain.utils

import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.ext.genesisHash
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

val Chain.relaychainsFirstAscendingOrder
    get() = when (genesisHash) {
        Chain.Geneses.POLKADOT -> 0
        Chain.Geneses.KUSAMA -> 1
        else -> 2
    }

val Chain.testnetsLastAscendingOrder
    get() = if (isTestNet) {
        1
    } else {
        0
    }

val Chain.alpabeticalOrder
    get() = name
