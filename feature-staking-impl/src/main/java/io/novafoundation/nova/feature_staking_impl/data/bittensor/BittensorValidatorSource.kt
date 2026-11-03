package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.utils.isNonNegative
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorGates
import java.math.BigDecimal
import kotlinx.coroutines.flow.Flow

class BittensorValidatorSource(
    private val client: BittensorBackendClient,
    private val validatorsApi: BittensorValidatorsApi,
    private val readExpiredCache: Boolean = true
) {

    suspend fun validators(netuid: Int): BittensorValidatorsRemote = client.get(
        url = client.url("subnets/$netuid/validators"),
        type = BittensorValidatorsRemote::class.java,
        allowStale = readExpiredCache,
        validate = { requireNotNull(it.items) { "Missing validator directory" } },
        fetch = validatorsApi::validators
    )

    suspend fun rootValidators(): BittensorRootValidatorRemote = client.get(
        url = client.url("root/validators"),
        type = BittensorRootValidatorRemote::class.java,
        allowStale = readExpiredCache,
        validate = ::validateRootValidators,
        fetch = validatorsApi::rootValidators
    )

    fun observeRootValidators(): Flow<BittensorRootValidatorRemote> = client.observe(
        url = { client.url("root/validators") },
        type = BittensorRootValidatorRemote::class.java,
        validate = ::validateRootValidators,
        fetch = validatorsApi::rootValidators
    )

    private suspend fun rootOverview(): BittensorRootOverviewRemote = client.get(
        url = client.url("root/overview"),
        type = BittensorRootOverviewRemote::class.java,
        allowStale = readExpiredCache,
        validate = {
            requireAvailableRootComponent(it.meta?.components?.indexes)
            it.stakeIndex?.stakeYield?.d30?.let { requireNotNull(it.apy).toBigDecimal() }
        },
        fetch = validatorsApi::rootOverview
    )

    suspend fun rankedValidators(netuid: Int): BittensorRankedValidatorsRemote {
        BittensorNetuid.from(netuid)
        return client.get(
            url = client.url("recommendations/subnets/$netuid/validators"),
            type = BittensorRankedValidatorsRemote::class.java,
            allowStale = readExpiredCache,
            validate = {
                mapBittensorValidatorGates(BittensorRecommendationsRemote(it.meta))
                requireNotNull(it.items)
            },
            generation = { mapBittensorRecommendationGeneration(it.meta) },
            fetch = validatorsApi::rankedValidators
        )
    }

    private suspend fun validatorRecommendations(): BittensorRecommendationsRemote = client.get(
        url = client.url("recommendations"),
        type = BittensorRecommendationsRemote::class.java,
        allowStale = readExpiredCache,
        validate = { validateRecommendedValidators(it) },
        generation = { mapBittensorRecommendationGeneration(it.meta) },
        fetch = validatorsApi::recommendations
    )

    private suspend fun subnetRecommendations(): BittensorRecommendationsRemote = client.get(
        url = client.url("recommendations/subnets"),
        type = BittensorRecommendationsRemote::class.java,
        allowStale = readExpiredCache,
        validate = ::validateSubnetRecommendations,
        generation = { mapBittensorRecommendationGeneration(it.meta) },
        fetch = validatorsApi::recommendations
    )

    /** Annual fraction (8.5 reported percent becomes 0.085). */
    suspend fun networkRootRate(): BigDecimal? {
        val response = rootOverview()
        return response.stakeIndex?.stakeYield?.d30?.apy?.toBigDecimal()?.takeIf { it.isNonNegative }?.movePointLeft(2)
    }

    suspend fun gates(): BittensorValidatorGates = mapBittensorValidatorGates(subnetRecommendations())

    suspend fun rankedSubnet(subnet: BittensorNetuid.Subnet, registrationBlock: java.math.BigInteger?): BittensorSubnetRankingRemote? {
        val document = subnetRecommendations()
        requireUsableBittensorRecommendations(document)
        val sourceBlock = requireNotNull(document.meta?.generation?.sourceBlockNumber)
        require(registrationBlock == null || sourceBlock >= registrationBlock) { "Subnet ranking predates registration" }
        return requireNotNull(document.items).singleOrNull { it.netuid == subnet.value }
    }

    suspend fun recommendedHotkeys(netuid: Int): List<String> = recommendedValidators()
        .filter { it.netuid == netuid }
        .mapNotNull { it.hotkey }
        .distinct()

    suspend fun recommendedValidators(): List<BittensorRecommendationsRemote.Item> = mapRecommendedValidators(
        validatorRecommendations()
    )

    private fun mapRecommendedValidators(response: BittensorRecommendationsRemote): List<BittensorRecommendationsRemote.Item> {
        requireUsableBittensorRecommendations(response)
        val sourceBlock = requireNotNull(response.meta?.generation?.sourceBlockNumber) { "Missing recommendation generation" }
        require(sourceBlock.isNonNegative) { "Invalid recommendation block" }
        val classes = requireNotNull(response.classes) { "Missing validator recommendations" }
        return listOf(classes.stable, classes.balanced, classes.higherUpside).flatMap { it.orEmpty() }
    }

    private fun validateRootValidators(response: BittensorRootValidatorRemote) {
        requireAvailableRootComponent(response.meta?.components?.rootValidators)
        requireNotNull(response.items).forEach { row ->
            row.rootStakeTao?.toBigDecimal()
            row.stakeYield?.d30?.let { requireNotNull(it.apy).toBigDecimal() }
        }
    }

    private fun requireAvailableRootComponent(component: BittensorRecommendationsRemote.Component?) {
        requireAvailableBittensorComponent(component, "Root yield data")
        parseBittensorTimestamp(requireNotNull(component?.asOf))
    }

    private fun validateRecommendedValidators(response: BittensorRecommendationsRemote) {
        requireUsableBittensorRecommendations(response)
        val sourceBlock = requireNotNull(response.meta?.generation?.sourceBlockNumber) { "Missing recommendation generation" }
        require(sourceBlock.isNonNegative) { "Invalid recommendation block" }
        requireNotNull(response.classes) { "Missing validator recommendations" }
    }

    private fun validateSubnetRecommendations(response: BittensorRecommendationsRemote) {
        requireUsableBittensorRecommendations(response)
        val items = requireNotNull(response.items) { "Missing subnet rankings" }
        require(items.map { it.netuid }.distinct().size == items.size) { "Duplicate subnet rankings" }
    }
}
