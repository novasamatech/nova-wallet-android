package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.utils.isNonNegative
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorGates
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorDirectoryStatus
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorDirectoryStatus.Availability
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorDirectoryStatus.Completeness
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorDirectoryStatus.Component
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorDirectoryStatus.Freshness
import java.math.BigDecimal
import java.math.BigInteger
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

internal data class BittensorRecommendationGeneration(val asOf: BigDecimal, val sourceBlockNumber: BigInteger) :
    Comparable<BittensorRecommendationGeneration> {

    override fun compareTo(other: BittensorRecommendationGeneration): Int =
        asOf.compareTo(other.asOf).takeIf { it != 0 } ?: sourceBlockNumber.compareTo(other.sourceBlockNumber)
}

internal fun mapBittensorRecommendationGeneration(meta: BittensorRecommendationsRemote.Meta?): BittensorRecommendationGeneration {
    val metadata = requireNotNull(meta) { "Missing recommendation metadata" }
    val sourceBlock = requireNotNull(metadata.generation?.sourceBlockNumber) { "Missing recommendation generation" }
    require(sourceBlock.isNonNegative && sourceBlock <= ULong.MAX_VALUE.toString().toBigInteger()) { "Invalid recommendation block" }
    val asOf = requireNotNull(metadata.components?.recommendations?.asOf) { "Missing recommendation timestamp" }
    return BittensorRecommendationGeneration(parseBittensorTimestamp(asOf), sourceBlock)
}

// Parse ISO instants without java.time (older Android), retaining sub-millisecond generation ordering.
internal fun parseBittensorTimestamp(value: String): BigDecimal {
    val match = requireNotNull(BITTENSOR_TIMESTAMP.matchEntire(value)) { "Invalid Bittensor timestamp" }
    val zone = match.groupValues[3].let { if (it == "Z") "+0000" else it.replace(":", "") }
    val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ROOT)
    formatter.timeZone = TimeZone.getTimeZone("UTC")
    formatter.isLenient = false
    val text = match.groupValues[1] + zone
    val position = ParsePosition(0)
    val date = requireNotNull(formatter.parse(text, position)) { "Invalid Bittensor timestamp" }
    require(position.index == text.length) { "Invalid Bittensor timestamp" }
    val fraction = match.groupValues[2].takeIf { it.isNotEmpty() }?.let { "0$it".toBigDecimal() } ?: BigDecimal.ZERO
    return date.time.toBigDecimal().movePointLeft(3) + fraction
}

private val BITTENSOR_TIMESTAMP = Regex("(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2})(\\.\\d{1,9})?(Z|[+-]\\d{2}:\\d{2})")

/** A STALE stakes component is usable data (response-metadata contract); only an unavailable or malformed one withholds the stake. */
internal fun hasUsableBittensorDirectoryStake(status: BittensorValidatorDirectoryStatus): Boolean = with(status.stakes) {
    availability == Availability.AVAILABLE && freshness != Freshness.UNKNOWN && asOfEpochSeconds != null
}

internal fun mapBittensorDirectoryStatus(meta: BittensorValidatorsRemote.Meta?): BittensorValidatorDirectoryStatus {
    val completeness = when (meta?.completeness) {
        "COMPLETE" -> Completeness.COMPLETE
        "PARTIAL" -> Completeness.PARTIAL
        else -> Completeness.UNKNOWN
    }
    return BittensorValidatorDirectoryStatus(
        completeness,
        mapDirectoryComponent(meta?.components?.validatorStakes),
        mapDirectoryComponent(meta?.components?.validatorMetagraph),
        mapDirectoryComponent(meta?.components?.validatorIdentities)
    )
}

/** The root list is never partial; its stakes and names both come from the root validators component. */
internal fun mapBittensorRootDirectoryStatus(meta: BittensorRootValidatorRemote.Meta?): BittensorValidatorDirectoryStatus {
    val rootValidators = mapDirectoryComponent(meta?.components?.rootValidators)
    return BittensorValidatorDirectoryStatus(Completeness.COMPLETE, rootValidators, mapDirectoryComponent(null), rootValidators)
}

private fun mapDirectoryComponent(remote: BittensorRecommendationsRemote.Component?): Component {
    val availability = when (remote?.availability) {
        "AVAILABLE" -> Availability.AVAILABLE
        "UNAVAILABLE" -> Availability.UNAVAILABLE
        else -> Availability.UNKNOWN
    }
    val freshness = when (remote?.freshness) {
        "FRESH" -> Freshness.FRESH
        "STALE" -> Freshness.STALE
        else -> Freshness.UNKNOWN
    }
    val timestamp = remote?.asOf?.let { runCatching { parseBittensorTimestamp(it) }.getOrNull() }
    return Component(availability, freshness, timestamp, remote?.availabilityReason)
}

/**
 * A STALE policy still applies: its take/permit/activity limits are configuration. A STALE generation still recommends too,
 * and the validator list shows it as outdated - see [hasFreshBittensorRecommendations].
 */
internal fun requireUsableBittensorRecommendations(response: BittensorRecommendationsRemote) {
    val meta = requireNotNull(response.meta) { "Missing validator policy" }
    requireAvailableBittensorComponent(meta.components?.recommendations, "Validator policy")
    val gates = requireNotNull(meta.clientGates)
    val maxCommissionRate = requireNotNull(gates.maxTake).toBigDecimal()
    require(maxCommissionRate.isNonNegative && maxCommissionRate <= BigDecimal.ONE) { "Invalid validator take limit" }
    requireNotNull(gates.requirePermit)
    requireNotNull(gates.requireActiveWithinCutoff)
}

internal fun mapBittensorValidatorGates(response: BittensorRecommendationsRemote): BittensorValidatorGates {
    requireUsableBittensorRecommendations(response)
    val gates = requireNotNull(response.meta?.clientGates)
    return BittensorValidatorGates(
        maxCommissionRate = requireNotNull(gates.maxTake).toBigDecimal(),
        requirePermit = requireNotNull(gates.requirePermit),
        requireActive = requireNotNull(gates.requireActiveWithinCutoff)
    )
}
