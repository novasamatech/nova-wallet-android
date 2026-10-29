package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import java.text.Collator
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BittensorSubnetNameComparatorTest {

    private val comparator = BittensorSubnetNameComparator(Locale.US)

    private val tieBreakCollator = Collator.getInstance(Locale.US)

    @Test
    fun `numeric parts compare by value without integer overflow`() {
        assertTrue(comparator.compare("Subnet 2", "Subnet 10") < 0)
        assertTrue(comparator.compare("Subnet 999999999999999999999999", "Subnet 1000000000000000000000000") < 0)
    }

    @Test
    fun `equal numeric parts continue with the following text regardless of leading zeros`() {
        assertTrue(comparator.compare("Subnet 002 Beta", "Subnet 2 Alpha") > 0)
        assertTrue(comparator.compare("Subnet 0 Alpha", "Subnet 000 Beta") < 0)
    }

    @Test
    fun `equivalent parts use the existing locale tie break for case and leading zeros`() {
        for ((first, second) in listOf("ALPHA" to "alpha", "Subnet 02" to "Subnet 2", "" to "")) {
            assertEquals(tieBreakCollator.compare(first, second), comparator.compare(first, second))
        }
    }

    @Test
    fun `empty text and prefixes keep their existing order`() {
        assertTrue(comparator.compare("", "Alpha") < 0)
        assertTrue(comparator.compare("Subnet", "Subnet 2") < 0)
        assertEquals(0, comparator.compare("Subnet 2", "Subnet 2"))
    }
}
