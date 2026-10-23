package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import java.math.BigDecimal

/** [staleRecommendations]: the recommendations come from a STALE generation, which is usable but shown as outdated. */
data class BittensorValidatorDirectory(
    val validators: List<BittensorSubnetValidator>,
    val status: BittensorValidatorDirectoryStatus,
    val recommendationFailure: Throwable?,
    val staleRecommendations: Boolean = false
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
