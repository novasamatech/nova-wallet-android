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
    subnetNumber: (Int) -> String
): List<BittensorSubnet> {
    val ranks = previousOrder.withIndex().associate { it.value to it.index }
    val order: Comparator<BittensorSubnet> = when (sorting) {
        BittensorSubnetSorting.SEVEN_DAY_CHANGE -> compareByDescending<BittensorSubnet> { weekly[it.id] }
        BittensorSubnetSorting.POOL_DEPTH -> compareByDescending { it.taoReserveInPlanks }
        BittensorSubnetSorting.AGE -> compareBy { it.registrationBlock }
        // By the title the row shows: an unnamed subnet sorts as "Subnet <netuid>".
        BittensorSubnetSorting.NAME -> compareBy(BittensorNaturalOrder(locale)) { subnet ->
            subnet.name.trim().ifEmpty { subnetNumber(subnet.id.value) }
        }
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

/** Finder order: case-insensitive, runs of digits compared by value ("Subnet 2" before "Subnet 10"). */
internal class BittensorNaturalOrder(locale: Locale) : Comparator<String> {

    private val letters = Collator.getInstance(locale).apply { strength = Collator.SECONDARY }

    private val exact = Collator.getInstance(locale)

    override fun compare(first: String, second: String): Int {
        val firstRuns = first.runs()
        val secondRuns = second.runs()
        firstRuns.zip(secondRuns).forEach { (left, right) ->
            val result = if (left.isNumber() && right.isNumber()) compareNumbers(left, right) else letters.compare(left, right)
            if (result != 0) return result
        }
        val byRuns = firstRuns.size.compareTo(secondRuns.size)
        return if (byRuns != 0) byRuns else exact.compare(first, second)
    }

    private fun String.runs(): List<String> = RUNS.findAll(this).map { it.value }.toList()

    private fun String.isNumber(): Boolean = first() in '0'..'9'

    private fun compareNumbers(left: String, right: String): Int {
        val leftDigits = left.trimStart('0')
        val rightDigits = right.trimStart('0')
        return if (leftDigits.length != rightDigits.length) leftDigits.length.compareTo(rightDigits.length) else leftDigits.compareTo(rightDigits)
    }

    private companion object {

        val RUNS = Regex("[0-9]+|[^0-9]+")
    }
}
