package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.utils.isNonNegative
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorGates
import java.math.BigDecimal

internal fun requireUsableBittensorRecommendations(response: BittensorRecommendationsRemote) {
    val meta = requireNotNull(response.meta) { "Missing validator policy" }
    val component = requireNotNull(meta.components?.recommendations)
    require(component.availability == "AVAILABLE" && component.freshness == "FRESH") { "Validator policy is unavailable or stale" }
    val gates = requireNotNull(meta.clientGates)
    val maxTake = requireNotNull(gates.maxTake).toBigDecimal()
    require(maxTake.isNonNegative && maxTake <= BigDecimal.ONE) { "Invalid validator take limit" }
    requireNotNull(gates.requirePermit)
    requireNotNull(gates.requireActiveWithinCutoff)
}

internal fun bindBittensorValidatorGates(response: BittensorRecommendationsRemote): BittensorValidatorGates {
    requireUsableBittensorRecommendations(response)
    val gates = requireNotNull(response.meta?.clientGates)
    return BittensorValidatorGates(
        maxTake = requireNotNull(gates.maxTake).toBigDecimal(),
        requirePermit = requireNotNull(gates.requirePermit),
        requireActive = requireNotNull(gates.requireActiveWithinCutoff)
    )
}
