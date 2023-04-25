package io.novafoundation.nova.feature_wallet_connect_impl.domain.model

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class SessionChains(
    val required: ResolvedChains,
    val optional: ResolvedChains,
) {

    class ResolvedChains(val knownChains: Set<Chain>, val unknownChains: Set<String>)
}
