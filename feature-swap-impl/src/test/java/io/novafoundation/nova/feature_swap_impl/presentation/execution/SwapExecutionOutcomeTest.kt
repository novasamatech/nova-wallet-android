package io.novafoundation.nova.feature_swap_impl.presentation.execution

import io.novafoundation.nova.common.base.errors.SigningCancelledException
import io.novafoundation.nova.feature_swap_api.domain.model.SwapProgressStep
import io.novafoundation.nova.feature_swap_api.domain.model.SwapSubmittedTransaction
import io.novafoundation.nova.feature_swap_api.domain.model.SwapProgress
import io.novafoundation.nova.feature_swap_api.domain.model.SwapOperationSubmissionException
import java.io.IOException
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
import org.mockito.Mockito.mock

class SwapExecutionOutcomeTest {
    private val step = mock(SwapProgressStep::class.java)

    @Test
    fun `timeout without receipt cannot offer retry as a proven failure`() {
        assertFalse(SwapProgress.Failure(IOException("connection lost"), step).hasKnownOutcome())
    }

    @Test
    fun `successful origin receipt does not prove failed cross chain delivery`() {
        val receipt = SwapSubmittedTransaction("origin", "hash", failedOnChain = false)
        assertFalse(SwapProgress.Failure(IOException("arrival unavailable"), step, receipt).hasKnownOutcome())
    }

    @Test
    fun `failed chain receipt allows returning the failed step to the form`() {
        val receipt = SwapSubmittedTransaction("origin", "hash", failedOnChain = true)
        assertTrue(SwapProgress.Failure(IllegalStateException(), step, receipt).hasKnownOutcome())
    }

    @Test
    fun `cancelled signing and failed simulation are known not to have submitted`() {
        assertTrue(SwapProgress.Failure(SigningCancelledException(), step).hasKnownOutcome())
        assertTrue(SwapProgress.Failure(SwapOperationSubmissionException.SimulationFailed(), step).hasKnownOutcome())
    }
}
