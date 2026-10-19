package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.util.Locale

class BittensorValidatorSortingTest {

    @Test
    fun `stake order is descending with unknown stakes last`() {
        val validators = listOf(validator("Alpha", null), validator("Beta", "10"), validator("Gamma", "100"))

        assertEquals(listOf("Gamma", "Beta", "Alpha"), sort(validators, BittensorValidatorSorting.TOTAL_STAKE))
    }

    @Test
    fun `stake comparison preserves decimal precision`() {
        val validators = listOf(validator("Alpha", "100000000000000000.0001"), validator("Beta", "100000000000000000.0002"))

        assertEquals(listOf("Beta", "Alpha"), sort(validators, BittensorValidatorSorting.TOTAL_STAKE))
    }

    @Test
    fun `equal stakes are ordered by name`() {
        val validators = listOf(validator("Zulu", "10.0"), validator("Alpha", "10"))

        assertEquals(listOf("Alpha", "Zulu"), sort(validators, BittensorValidatorSorting.TOTAL_STAKE))
    }

    @Test
    fun `name sorting ignores stake and recommendation`() {
        val validators = listOf(validator("Zulu", "100", recommended = true), validator("Alpha", "1"))

        assertEquals(listOf("Alpha", "Zulu"), sort(validators, BittensorValidatorSorting.NAME))
    }

    @Test
    fun `selected validator remains first for either sort`() {
        val validators = listOf(validator("Alpha", "100"), validator("Zulu", "1"))

        BittensorValidatorSorting.entries.forEach { sorting ->
            assertEquals(listOf("Zulu", "Alpha"), sort(validators, sorting, selectedAddress = "Zulu"))
        }
    }

    @Test
    fun `inactive selected validator is not offered`() {
        val validators = listOf(validator("Alpha", "100", selectable = false), validator("Beta", "1"))

        assertEquals(listOf("Beta"), sort(validators, BittensorValidatorSorting.TOTAL_STAKE, selectedAddress = "Alpha"))
    }

    @Test
    fun `missing selection does not affect sorting`() {
        val validators = listOf(validator("Zulu", null), validator("Alpha", null))

        assertEquals(listOf("Alpha", "Zulu"), sort(validators, BittensorValidatorSorting.NAME, selectedAddress = "Missing"))
    }

    @Test
    fun `APY sorting uses descending rates then stake and unknown rates last`() {
        val low = validator("Low", "100")
        val high = validator("High", "1")
        val unknown = validator("Unknown", "1000")
        val rates = mapOf(low.validator.hotkey to BigDecimal.ONE, high.validator.hotkey to BigDecimal.TEN)
        val sorted = sortBittensorValidators(listOf(unknown, low, high), BittensorValidatorSorting.APY, null, Locale.US, rates)
        assertEquals(listOf("High", "Low", "Unknown"), sorted.map { it.address })
    }

    @Test
    fun `unusual filter is recommendation membership not a numeric cutoff and retains selection`() {
        val selected = validator("Selected", "1")
        val preselected = validator("Preselected", "1")
        val recommended = validator("Recommended", "1", recommended = true)
        val hidden = validator("Hidden", "100")
        val rates = mapOf(recommended.validator.hotkey to BigDecimal("999999"), hidden.validator.hotkey to BigDecimal.ONE)
        val rows = sortBittensorValidators(
            listOf(hidden, recommended, selected, preselected),
            BittensorValidatorSorting.APY,
            "Selected",
            Locale.US,
            rates,
            hideUnusualApy = true,
            preselectedAddress = "Preselected"
        )
        assertEquals(listOf("Preselected", "Recommended", "Selected"), rows.map { it.address })
    }

    @Test
    fun `unavailable directory stays readable during recommendation recovery`() {
        val row = validator("Unavailable", "10", selectable = false)
        val rows = sortBittensorValidators(
            listOf(row),
            BittensorValidatorSorting.APY,
            null,
            Locale.US,
            hideUnusualApy = true,
            includeUnavailable = true
        )
        assertEquals(listOf(row), rows)
    }

    private fun sort(
        validators: List<BittensorSubnetValidator>,
        sorting: BittensorValidatorSorting,
        selectedAddress: String? = null
    ): List<String> = sortBittensorValidators(validators, sorting, selectedAddress, Locale.US)
        .map { it.validator.name }

    private fun validator(
        name: String,
        stake: String?,
        selectable: Boolean = true,
        recommended: Boolean = false
    ): BittensorSubnetValidator {
        val hotkey = AccountIdKey(name.toByteArray())
        val validator = BittensorValidator(hotkey, name, stake?.toBigDecimal())
        return BittensorSubnetValidator(validator, name, BigDecimal.ZERO, selectable, recommended)
    }
}
