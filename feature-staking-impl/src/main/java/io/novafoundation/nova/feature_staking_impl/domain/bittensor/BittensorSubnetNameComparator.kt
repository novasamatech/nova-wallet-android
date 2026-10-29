package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import java.text.Collator
import java.util.Locale

/** Compares names without case first and numbers by value: "Subnet 2" precedes "Subnet 10". */
internal class BittensorSubnetNameComparator(locale: Locale) : Comparator<String> {

    private val caseInsensitiveCollator = Collator.getInstance(locale).apply { strength = Collator.SECONDARY }

    private val tieBreakCollator = Collator.getInstance(locale)

    override fun compare(first: String, second: String): Int {
        val firstParts = splitIntoTextAndNumbers(first)
        val secondParts = splitIntoTextAndNumbers(second)

        for (index in 0 until minOf(firstParts.size, secondParts.size)) {
            val comparison = compareParts(firstParts[index], secondParts[index])
            if (comparison != 0) return comparison
        }

        val partCountComparison = firstParts.size.compareTo(secondParts.size)
        if (partCountComparison != 0) return partCountComparison

        return tieBreakCollator.compare(first, second)
    }

    private fun splitIntoTextAndNumbers(name: String): List<String> {
        return TEXT_OR_NUMBER.findAll(name)
            .map { it.value }
            .toList()
    }

    private fun compareParts(first: String, second: String): Int {
        return if (first.isNumber() && second.isNumber()) {
            compareNumbers(first, second)
        } else {
            caseInsensitiveCollator.compare(first, second)
        }
    }

    private fun String.isNumber(): Boolean = first() in '0'..'9'

    private fun compareNumbers(first: String, second: String): Int {
        val firstDigits = first.trimStart('0')
        val secondDigits = second.trimStart('0')
        val digitCountComparison = firstDigits.length.compareTo(secondDigits.length)
        if (digitCountComparison != 0) return digitCountComparison

        return firstDigits.compareTo(secondDigits)
    }

    private companion object {

        val TEXT_OR_NUMBER = Regex("[0-9]+|[^0-9]+")
    }
}
