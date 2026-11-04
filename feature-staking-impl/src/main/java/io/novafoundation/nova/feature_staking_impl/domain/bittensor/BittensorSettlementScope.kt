package io.novafoundation.nova.feature_staking_impl.domain.bittensor

/** Networks whose indexed cost basis may change after an own transaction. */
sealed interface BittensorSettlementScope {

    data object Account : BittensorSettlementScope

    data class Network(val netuid: BittensorNetuid) : BittensorSettlementScope
}

internal fun BittensorOutcomeTarget?.settlementScope(): BittensorSettlementScope = when {
    this == null -> BittensorSettlementScope.Account
    kind == BittensorOperationKind.CLAIM && netuid.isRoot() -> BittensorSettlementScope.Account
    else -> BittensorSettlementScope.Network(netuid)
}
