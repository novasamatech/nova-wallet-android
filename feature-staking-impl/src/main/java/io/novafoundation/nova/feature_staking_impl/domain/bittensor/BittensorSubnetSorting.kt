package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetFavorites
import java.math.BigDecimal
import java.math.BigInteger
import java.util.Locale

enum class BittensorSubnetSorting {
    SEVEN_DAY_CHANGE,
    POOL_DEPTH,
    AGE,
    NAME,
    APY
}

internal fun sortBittensorSubnets(
    subnets: List<BittensorSubnet>,
    sorting: BittensorSubnetSorting,
    favorites: BittensorSubnetFavorites,
    hideThinPools: Boolean,
    yields: Map<BittensorNetuid.Subnet, BigDecimal>,
    weekly: Map<BittensorNetuid.Subnet, BigDecimal>,
    previousOrder: List<BittensorSubnetReference>,
    locale: Locale,
    subnetNumber: (Int) -> String
): List<BittensorSubnet> {
    val metricComparator = compareBySelectedMetric(
        sorting,
        yields,
        weekly,
        locale,
        subnetNumber
    )
    val previousOrderComparator = compareByPreviousOrder(previousOrder)
    val comparator = compareByDescending<BittensorSubnet> { it in favorites }
        .then(metricComparator)
        .then(previousOrderComparator)
        .thenBy { it.id.value }
        .thenBy { it.registrationBlock }

    return subnets
        .filter { !hideThinPools || passesBittensorThinPoolFilter(it, favorites) }
        .sortedWith(comparator)
}

internal fun passesBittensorThinPoolFilter(subnet: BittensorSubnet, favorites: BittensorSubnetFavorites): Boolean {
    if (subnet in favorites) return true

    val poolReserve = subnet.taoReserveInPlanks ?: return false
    return poolReserve >= THIN_POOL_THRESHOLD
}

private fun compareBySelectedMetric(
    sorting: BittensorSubnetSorting,
    annualYields: Map<BittensorNetuid.Subnet, BigDecimal>,
    weeklyChanges: Map<BittensorNetuid.Subnet, BigDecimal>,
    locale: Locale,
    subnetNumber: (Int) -> String
): Comparator<BittensorSubnet> {
    return when (sorting) {
        BittensorSubnetSorting.SEVEN_DAY_CHANGE -> compareByDescending { weeklyChanges[it.id] }
        BittensorSubnetSorting.POOL_DEPTH -> compareByDescending { it.taoReserveInPlanks }
        BittensorSubnetSorting.AGE -> compareBy(nullsLast<BigInteger>()) { it.registrationBlock }
        BittensorSubnetSorting.NAME -> compareByDisplayedName(
            locale,
            subnetNumber
        )
        BittensorSubnetSorting.APY -> compareByDescending { annualYields[it.id] }
    }
}

private fun compareByDisplayedName(locale: Locale, subnetNumber: (Int) -> String): Comparator<BittensorSubnet> {
    val nameComparator = BittensorSubnetNameComparator(locale)
    return compareBy(nameComparator) { subnet ->
        subnet.name.trim().ifEmpty { subnetNumber(subnet.id.value) }
    }
}

private fun compareByPreviousOrder(previousOrder: List<BittensorSubnetReference>): Comparator<BittensorSubnet> {
    val previousRanks = previousOrder.withIndex().associate { it.value to it.index }
    return compareBy { subnet ->
        subnet.previousRank(previousRanks)
    }
}

private fun BittensorSubnet.previousRank(previousRanks: Map<BittensorSubnetReference, Int>): Int {
    val registrationBlock = registrationBlock ?: return Int.MAX_VALUE
    val reference = BittensorSubnetReference(id, registrationBlock)
    return previousRanks[reference] ?: Int.MAX_VALUE
}

private val THIN_POOL_THRESHOLD = BigInteger.valueOf(2_000_000_000_000L)
