package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.amount

import org.junit.Assert.assertEquals
import org.junit.Test

class BittensorRootStakeEntryTest {

    @Test
    fun `a fixed validator is always add stake`() {
        val payload = BittensorStakeAmountPayload(1L, "validator")

        assertEquals(BittensorRootStakeEntry.ADD_STAKE, bittensorRootStakeEntry(payload, BittensorRootStakePresentation.QUICK_PICK))
        assertEquals(BittensorRootStakeEntry.ADD_STAKE, bittensorRootStakeEntry(payload, BittensorRootStakePresentation.STANDARD))
    }

    @Test
    fun `where to stake root opens the quick pick`() {
        assertEquals(BittensorRootStakeEntry.QUICK_PICK, bittensorRootStakeEntry(null, BittensorRootStakePresentation.QUICK_PICK))
    }

    @Test
    fun `the picker root row and wallet-bound entries open the plain root setup`() {
        assertEquals(BittensorRootStakeEntry.PICKER, bittensorRootStakeEntry(null, BittensorRootStakePresentation.STANDARD))
        assertEquals(BittensorRootStakeEntry.PICKER, bittensorRootStakeEntry(BittensorStakeAmountPayload(1L, null), BittensorRootStakePresentation.STANDARD))
    }
}
