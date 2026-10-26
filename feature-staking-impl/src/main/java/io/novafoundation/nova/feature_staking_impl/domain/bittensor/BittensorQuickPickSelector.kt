package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import java.math.BigInteger
import java.math.BigDecimal
import kotlin.random.Random

enum class BittensorQuickPickOrder {
    POOL_SIZE,
    AGE,
    APY
}

class BittensorQuickPickSelector(private val random: Random) {

    fun select(
        subnets: List<BittensorSubnet>,
        order: BittensorQuickPickOrder,
        annualYields: Map<BittensorNetuid.Subnet, BigDecimal> = emptyMap()
    ): BittensorSubnetReference {
        val candidates = candidates(subnets, order, annualYields)
        require(candidates.isNotEmpty()) { "No subnets are available for quick pick" }
        return candidates[random.nextInt(candidates.size)]
    }

    internal fun candidates(
        subnets: List<BittensorSubnet>,
        order: BittensorQuickPickOrder,
        annualYields: Map<BittensorNetuid.Subnet, BigDecimal> = emptyMap()
    ): List<BittensorSubnetReference> {
        val comparator = when (order) {
            BittensorQuickPickOrder.POOL_SIZE -> compareByDescending<BittensorSubnet> { it.taoReserveInPlanks }
            BittensorQuickPickOrder.AGE -> compareBy { it.registrationBlock }
            BittensorQuickPickOrder.APY -> compareByDescending { annualYields[it.id] }
        }
            .thenBy { it.id.value }

        require(subnets.all { it.taoReserveInPlanks != null && it.registrationBlock != null }) {
            "Quick pick requires complete pool and registration data"
        }

        return subnets
            .filter { requireNotNull(it.taoReserveInPlanks) >= MINIMUM_POOL_RESERVE_IN_RAO }
            .filter { order != BittensorQuickPickOrder.APY || it.id in annualYields }
            .sortedWith(comparator)
            .take(CANDIDATE_LIMIT)
            .map { BittensorSubnetReference(it.id, requireNotNull(it.registrationBlock)) }
    }

    private companion object {

        const val CANDIDATE_LIMIT = 10

        // Shared iOS quick-pick policy, in protocol units (not display precision).
        val MINIMUM_POOL_RESERVE_IN_RAO = BigInteger.valueOf(2_000_000_000_000L)
    }
}
