package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import java.math.BigDecimal

data class BittensorValidatorDirectory(
    val validators: List<BittensorSubnetValidator>,
    val status: BittensorValidatorDirectoryStatus,
    val recommendationFailure: Throwable?
)

data class BittensorValidatorDirectoryStatus(
    val completeness: Completeness,
    val stakes: Component,
    val metagraph: Component,
    val identities: Component
) {

    enum class Completeness { COMPLETE, PARTIAL, UNKNOWN }

    enum class Availability { AVAILABLE, UNAVAILABLE, UNKNOWN }

    enum class Freshness { FRESH, STALE, UNKNOWN }

    data class Component(
        val availability: Availability,
        val freshness: Freshness,
        val asOfEpochSeconds: BigDecimal?,
        val availabilityReason: String?
    )
}
