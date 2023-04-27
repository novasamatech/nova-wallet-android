package io.novafoundation.nova.feature_wallet_connect_impl.domain.model

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class SessionChains(
    val required: ResolvedChains,
    val optional: ResolvedChains,
) {

    class ResolvedChains(val knownChains: Set<Chain>, val unknownChains: Set<String>)
}

fun SessionChains.allKnownChains(): Set<Chain> {
    return required.knownChains + optional.knownChains
}

fun SessionChains.allUnknownChains(): Set<String> {
    return required.unknownChains + optional.unknownChains
}

fun SessionChains.ResolvedChains.hasUnknown(): Boolean {
    return unknownChains.isNotEmpty()
}

fun SessionChains.ResolvedChains.hasKnown(): Boolean {
    return knownChains.isNotEmpty()
}

fun SessionChains.ResolvedChains.hasAny(): Boolean {
    return hasUnknown() || hasKnown()
}
