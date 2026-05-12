package io.novafoundation.nova.feature_staking_impl.presentation.staking.start.landing

import io.novafoundation.nova.feature_staking_impl.data.notices.model.StakingNotice
import org.junit.Assert.assertEquals
import org.junit.Test

class StartStakingCriticalNoticeInterceptTest {

    /**
     * Intercept decision: given the currently-resolved StakingNotice (or null), should the
     * Start CTA show the critical sheet first, or proceed directly?
     */
    private fun shouldShowCriticalSheet(notice: StakingNotice?): Boolean =
        notice?.severity == StakingNotice.Severity.CRITICAL

    private fun fakeNotice(severity: StakingNotice.Severity) = StakingNotice(
        chainId = "deadbeef",
        severity = severity,
        shortText = "x",
        longText = "y",
        endDate = null
    )

    @Test
    fun `no notice does not show sheet`() {
        assertEquals(false, shouldShowCriticalSheet(null))
    }

    @Test
    fun `info notice does not show sheet`() {
        assertEquals(false, shouldShowCriticalSheet(fakeNotice(StakingNotice.Severity.INFO)))
    }

    @Test
    fun `critical notice shows sheet`() {
        assertEquals(true, shouldShowCriticalSheet(fakeNotice(StakingNotice.Severity.CRITICAL)))
    }
}
