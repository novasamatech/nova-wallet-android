package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Test

class BittensorSubnetAgeFormattingTest {

    private fun age(days: String) = bittensorSubnetAge(BigDecimal(days))

    @Test
    fun `ages of a month and longer are shown in whole months`() {
        assertEquals(BittensorAge(33, BittensorAgeUnit.MONTHS), age("1010"))
        assertEquals(BittensorAge(1, BittensorAgeUnit.MONTHS), age("30.44"))
        assertEquals(BittensorAge(21, BittensorAgeUnit.MONTHS), age("667"))
    }

    @Test
    fun `ages under a month are shown in whole days`() {
        assertEquals(BittensorAge(30, BittensorAgeUnit.DAYS), age("30.4"))
        assertEquals(BittensorAge(12, BittensorAgeUnit.DAYS), age("12.9"))
        assertEquals(BittensorAge(1, BittensorAgeUnit.DAYS), age("1"))
    }

    @Test
    fun `ages under a day fall back to hours and then minutes`() {
        assertEquals(BittensorAge(5, BittensorAgeUnit.HOURS), age("0.24"))
        assertEquals(BittensorAge(30, BittensorAgeUnit.MINUTES), age("0.0209"))
        assertEquals(BittensorAge(0, BittensorAgeUnit.MINUTES), age("0"))
    }

    @Test
    fun `negative ages are treated as zero`() {
        assertEquals(BittensorAge(0, BittensorAgeUnit.MINUTES), age("-1"))
    }
}
