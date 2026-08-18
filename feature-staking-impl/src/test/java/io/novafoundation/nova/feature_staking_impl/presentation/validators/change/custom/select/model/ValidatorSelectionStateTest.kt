package io.novafoundation.nova.feature_staking_impl.presentation.validators.change.custom.select.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidatorSelectionStateTest {

    @Test
    fun `total counters should include locked validators`() {
        val state = ValidatorSelectionState(
            communitySelected = 0,
            communityLimit = 15,
            lockedSelected = 1,
            totalLimit = 16
        )

        assertEquals(1, state.totalSelected)
        assertEquals(16, state.totalLimit)
    }

    @Test
    fun `total counters should sum community and locked selection`() {
        val state = ValidatorSelectionState(
            communitySelected = 2,
            communityLimit = 2,
            lockedSelected = 1,
            totalLimit = 3
        )

        assertEquals(3, state.totalSelected)
        assertEquals(3, state.totalLimit)
    }

    @Test
    fun `selection with only locked validators should not be empty`() {
        val state = ValidatorSelectionState(
            communitySelected = 0,
            communityLimit = 15,
            lockedSelected = 1,
            totalLimit = 16
        )

        assertFalse(state.isEmpty())
    }

    @Test
    fun `selection without any validator should be empty`() {
        val state = ValidatorSelectionState(
            communitySelected = 0,
            communityLimit = 16,
            lockedSelected = 0,
            totalLimit = 16
        )

        assertTrue(state.isEmpty())
    }
}
