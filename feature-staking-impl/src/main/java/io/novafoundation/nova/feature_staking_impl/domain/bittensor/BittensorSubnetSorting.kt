package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetFavorites
import java.math.BigDecimal
import java.math.BigInteger
import java.text.Collator
import java.util.Locale

enum class BittensorSubnetSorting { SEVEN_DAY_CHANGE, POOL_DEPTH, AGE, NAME, APY }

internal fun sortBittensorSubnets(
    subnets: List<BittensorSubnet>,
    sorting: BittensorSubnetSorting,
    favorites: BittensorSubnetFavorites,
    hideThinPools: Boolean,
    yields: Map<BittensorNetuid.Subnet, BigDecimal>,
    weekly: Map<BittensorNetuid.Subnet, BigDecimal>,
    previousOrder: List<BittensorSubnetReference>,
    locale: Locale,
    title: (BittensorSubnet) -> String = BittensorSubnet::name
): List<BittensorSubnet> {
    val names = naturalOrder(Collator.getInstance(locale))
    val ranks = previousOrder.withIndex().associate { it.value to it.index }
    val order: Comparator<BittensorSubnet> = when (sorting) {
        BittensorSubnetSorting.SEVEN_DAY_CHANGE -> compareByDescending<BittensorSubnet> { weekly[it.id] }
        BittensorSubnetSorting.POOL_DEPTH -> compareByDescending { it.taoReserveInPlanks }
        BittensorSubnetSorting.AGE -> compareBy { it.registrationBlock }
        BittensorSubnetSorting.NAME -> Comparator { first, second -> names.compare(title(first), title(second)) }
        BittensorSubnetSorting.APY -> compareByDescending { yields[it.id] }
    }
    val comparator = compareByDescending<BittensorSubnet> { it in favorites }.then(order)
        .thenBy { subnet -> subnet.registrationBlock?.let { ranks[BittensorSubnetReference(subnet.id, it)] } ?: Int.MAX_VALUE }
        .thenBy { it.id.value }
        .thenBy { it.registrationBlock }
    return subnets.filter { !hideThinPools || passesBittensorThinPoolFilter(it, favorites) }
        .sortedWith(comparator)
}

internal fun passesBittensorThinPoolFilter(subnet: BittensorSubnet, favorites: BittensorSubnetFavorites): Boolean =
    subnet in favorites || (subnet.taoReserveInPlanks ?: BigInteger.ZERO) >= THIN_POOL_THRESHOLD

private val THIN_POOL_THRESHOLD = BigInteger.valueOf(2_000_000_000_000L)

private val DIGITS_OR_TEXT = Regex("\\d+|\\D+")

/** Like iOS `localizedStandardCompare`: digit runs compare as numbers, so "Subnet 9" sorts before "Subnet 42". */
private fun naturalOrder(collator: Collator) = Comparator<String> { first, second ->
    val firstParts = DIGITS_OR_TEXT.findAll(first).map { it.value }.iterator()
    val secondParts = DIGITS_OR_TEXT.findAll(second).map { it.value }.iterator()
    while (firstParts.hasNext() && secondParts.hasNext()) {
        val left = firstParts.next()
        val right = secondParts.next()
        val result = if (left[0].isDigit() && right[0].isDigit()) {
            left.toBigInteger().compareTo(right.toBigInteger())
        } else {
            collator.compare(left, right)
        }
        if (result != 0) return@Comparator result
    }
    firstParts.hasNext().compareTo(secondParts.hasNext())
}
