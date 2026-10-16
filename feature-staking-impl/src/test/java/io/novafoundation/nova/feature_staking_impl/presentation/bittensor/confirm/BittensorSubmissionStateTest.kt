package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm

import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model.BittensorStakeResultUi
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model.BittensorSubmissionState
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.model.BittensorFeeUi
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class BittensorSubmissionStateTest {

    @Test
    fun `closing success does not allow the confirmed operation to be submitted again`() {
        val success = BittensorStakeResultUi.Success("Done", "", "Validator", BittensorFeeUi("1", ""))
        val completed = BittensorSubmissionState.Completed(success).dismissResult()

        assertTrue(completed is BittensorSubmissionState.Completed)
        assertNull(completed.result)
        assertFalse(completed.canSubmit)
    }

    @Test
    fun `failure can be retried while progress cannot be dismissed or submitted twice`() {
        val failure = BittensorStakeResultUi.Failure("Failed", "", "RPC")
        assertTrue(BittensorSubmissionState.Failed(failure).dismissResult().canSubmit)
        val progress = BittensorSubmissionState.Submitting(BittensorStakeResultUi.Progress("Staking", ""))

        assertFalse(progress.canSubmit)
        assertSame(progress, progress.dismissResult())
    }
}
