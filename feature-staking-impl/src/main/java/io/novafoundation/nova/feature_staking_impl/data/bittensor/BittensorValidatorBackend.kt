package io.novafoundation.nova.feature_staking_impl.data.bittensor

import kotlinx.coroutines.flow.Flow

import io.novafoundation.nova.common.utils.isNonNegative
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorGates
import kotlinx.coroutines.flow.map
import java.math.BigDecimal

/** Validated backend documents share the persistent HTTP cache; no network call holds a global lock. */
class BittensorValidatorBackend(
    private val api: BittensorValidatorsApi,
    private val cache: BittensorResponseCache,
    private val readExpiredCache: Boolean = true
) {

    suspend fun validators(netuid: Int): BittensorValidatorsRemote = cache.get(
        path = "subnets/$netuid/validators",
        allowStale = readExpiredCache,
        type = BittensorValidatorsRemote::class.java,
        validate = { requireNotNull(it.items) { "Missing validator directory" } },
        fetch = api::validators
    ).directoryForDisplay()

    suspend fun rootValidators(): BittensorRootValidatorRemote = cache.get(
        path = "root/validators",
        allowStale = readExpiredCache,
        type = BittensorRootValidatorRemote::class.java,
        validate = ::validateRootValidators,
        fetch = api::rootValidators
    ).rootForDisplay()

    fun observeRootValidators(): Flow<BittensorRootValidatorRemote> = cache.observe(
        path = "root/validators",
        type = BittensorRootValidatorRemote::class.java,
        validate = ::validateRootValidators,
        fetch = api::rootValidators
    ).map { it.rootForDisplay() }

    private fun validateRootValidators(response: BittensorRootValidatorRemote) {
        requireAvailableRootComponent(response.meta?.components?.rootValidators)
        requireNotNull(response.items).forEach { row ->
            row.rootStakeTao?.toBigDecimal()
            row.stakeYield?.d30?.let { requireNotNull(it.apy).toBigDecimal() }
        }
    }

    /** Annual fraction (8.5 reported percent becomes 0.085). */
    suspend fun networkRootRate(): BigDecimal? {
        val response = cache.get(
            path = "root/overview",
            allowStale = readExpiredCache,
            type = BittensorRootOverviewRemote::class.java,
            validate = {
                requireAvailableRootComponent(it.meta?.components?.indexes)
                it.stakeIndex?.stakeYield?.d30?.let { requireNotNull(it.apy).toBigDecimal() }
            },
            fetch = api::rootOverview
        ).value
        return response.stakeIndex?.stakeYield?.d30?.apy?.toBigDecimal()?.takeIf { it.isNonNegative }?.movePointLeft(2)
    }

    suspend fun rankedValidators(netuid: Int): BittensorRankedValidatorsRemote {
        BittensorNetuid.from(netuid)
        return cache.get(
            path = "recommendations/subnets/$netuid/validators",
            allowStale = readExpiredCache,
            type = BittensorRankedValidatorsRemote::class.java,
            validate = {
                mapBittensorValidatorGates(BittensorRecommendationsRemote(it.meta))
                requireNotNull(it.items)
            },
            generation = { mapBittensorRecommendationGeneration(it.meta) },
            fetch = api::rankedValidators
        ).rankingForDisplay()
    }

    suspend fun gates(): BittensorValidatorGates = mapBittensorValidatorGates(subnetRecommendations())

    suspend fun rankedSubnet(subnet: BittensorNetuid.Subnet, registrationBlock: java.math.BigInteger?): BittensorSubnetRankingRemote? {
        val document = subnetRecommendations()
        requireUsableBittensorRecommendations(document)
        val sourceBlock = requireNotNull(document.meta?.generation?.sourceBlockNumber)
        require(registrationBlock == null || sourceBlock >= registrationBlock) { "Subnet ranking predates registration" }
        val items = requireNotNull(document.items) { "Missing subnet rankings" }
        require(items.map { it.netuid }.distinct().size == items.size) { "Duplicate subnet rankings" }
        return items.singleOrNull { it.netuid == subnet.value }
    }

    suspend fun recommendedHotkeys(netuid: Int): List<String> = recommendedValidators()
        .filter { it.netuid == netuid }
        .mapNotNull { it.hotkey }
        .distinct()

    suspend fun recommendedValidators(): List<BittensorRecommendationsRemote.Item> = mapRecommendedValidators(
        cache.get(
            path = "recommendations",
            allowStale = readExpiredCache,
            type = BittensorRecommendationsRemote::class.java,
            validate = { mapRecommendedValidators(it) },
            generation = { mapBittensorRecommendationGeneration(it.meta) },
            fetch = api::recommendations
        ).recommendationsForDisplay()
    )

    private suspend fun subnetRecommendations(): BittensorRecommendationsRemote = cache.get(
        path = "recommendations/subnets",
        allowStale = readExpiredCache,
        type = BittensorRecommendationsRemote::class.java,
        validate = { mapBittensorValidatorGates(it) },
        generation = { mapBittensorRecommendationGeneration(it.meta) },
        fetch = api::recommendations
    ).recommendationsForDisplay()

    private fun requireAvailableRootComponent(component: BittensorRecommendationsRemote.Component?) {
        requireAvailableBittensorComponent(component, "Root yield data")
        parseBittensorTimestamp(requireNotNull(component?.asOf))
    }

    private fun mapRecommendedValidators(response: BittensorRecommendationsRemote): List<BittensorRecommendationsRemote.Item> {
        requireUsableBittensorRecommendations(response)
        val sourceBlock = requireNotNull(response.meta?.generation?.sourceBlockNumber) { "Missing recommendation generation" }
        require(sourceBlock.isNonNegative) { "Invalid recommendation block" }
        val classes = requireNotNull(response.classes) { "Missing validator recommendations" }
        return listOf(classes.stable, classes.balanced, classes.higherUpside).flatMap { it.orEmpty() }
    }
}

private fun BittensorCachedResponse<BittensorValidatorsRemote>.directoryForDisplay(): BittensorValidatorsRemote {
    if (!stale) return value
    val components = value.meta?.components
    val expired = BittensorValidatorsRemote.Components(
        components?.validatorStakes?.asStale(),
        components?.validatorMetagraph?.asStale(),
        components?.validatorIdentities?.asStale()
    )
    return BittensorValidatorsRemote(value.items, BittensorValidatorsRemote.Meta(value.meta?.completeness, expired))
}

private fun BittensorCachedResponse<BittensorRootValidatorRemote>.rootForDisplay(): BittensorRootValidatorRemote {
    if (!stale) return value
    val components = value.meta?.components
    val expired = BittensorRootValidatorRemote.Components(components?.rootValidators?.asStale(), components?.stakeYieldWindows?.asStale())
    return BittensorRootValidatorRemote(value.items, BittensorRootValidatorRemote.Meta(expired))
}

private fun BittensorRecommendationsRemote.Meta.asStale(): BittensorRecommendationsRemote.Meta {
    val expired = BittensorRecommendationsRemote.Components(
        components?.recommendations?.asStale(),
        components?.rootValidator?.asStale(),
        components?.rootClaims?.asStale(),
        components?.alphaYield?.asStale(),
        components?.operationHistory?.asStale()
    )
    return BittensorRecommendationsRemote.Meta(clientGates, expired, generation)
}

private fun BittensorCachedResponse<BittensorRankedValidatorsRemote>.rankingForDisplay(): BittensorRankedValidatorsRemote =
    if (stale) BittensorRankedValidatorsRemote(value.meta?.asStale(), value.items) else value

private fun BittensorCachedResponse<BittensorRecommendationsRemote>.recommendationsForDisplay(): BittensorRecommendationsRemote =
    if (stale) BittensorRecommendationsRemote(value.meta?.asStale(), value.classes, value.items) else value
