package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.common.data.network.runtime.binding.DispatchError
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorOperationResult
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorFailureStage
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model.BittensorResultAction
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model.BittensorStakeResultUi
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model.BittensorSubmissionState
import io.novasama.substrate_sdk_android.runtime.metadata.module.ErrorMetadata
import io.novasama.substrate_sdk_android.runtime.metadata.module.Module
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class BittensorOperationResultUiMappingTest {

    private val resources = mock(ResourceManager::class.java)

    @Test
    fun `unstaged failure cannot claim nothing spent or enable retry`() {
        `when`(resources.getString(R.string.bittensor_result_unconfirmed_title)).thenReturn("Unconfirmed")
        `when`(resources.getString(R.string.bittensor_result_unconfirmed_description)).thenReturn("Outcome and charges unknown")
        val cause = IllegalStateException("unknown phase")
        val failure = BittensorOperationResult.Failure(cause)

        val result = failure.mapResult(resources)
        val state = result.toSubmissionState()

        org.junit.Assert.assertTrue(result is BittensorStakeResultUi.Unconfirmed)
        org.junit.Assert.assertTrue(state is BittensorSubmissionState.Unconfirmed)
        assertFalse(state.canSubmit)
        assertFalse(state.dismissResult().canSubmit)
    }

    @Test
    fun `dispatched failure without identity degrades to unknown`() {
        `when`(resources.getString(R.string.bittensor_result_unconfirmed_title)).thenReturn("Unconfirmed")
        `when`(resources.getString(R.string.bittensor_result_unconfirmed_description)).thenReturn("Outcome and charges unknown")
        val cause = IllegalStateException("missing identity")
        val failure = BittensorOperationResult.Failure(cause, stage = BittensorFailureStage.DISPATCHED)

        val result = failure.mapResult(resources) as BittensorStakeResultUi.Unconfirmed

        assertNull(result.transactionHash)
        assertFalse(result.toSubmissionState().canSubmit)
    }

    @Test
    fun `unconfirmed uses neutral explanation and preserves optional hash`() {
        `when`(resources.getString(R.string.bittensor_result_unconfirmed_title)).thenReturn("Unconfirmed")
        `when`(resources.getString(R.string.bittensor_result_unconfirmed_description)).thenReturn("Outcome and charges unknown")

        val knownHash = BittensorOperationResult.Unknown("hash").mapResult(resources)
        val missingHash = BittensorOperationResult.Unknown(null).mapResult(resources)

        assertEquals("Outcome and charges unknown", knownHash.description)
        assertEquals("hash", knownHash.transactionHash)
        assertNull(missingHash.transactionHash)
    }

    @Test
    fun `pre submission explanation never exposes raw exception`() {
        `when`(resources.getString(R.string.common_failed)).thenReturn("Failed")
        `when`(resources.getString(R.string.bittensor_result_not_submitted)).thenReturn("Not submitted; nothing spent")
        `when`(resources.getString(R.string.common_error_general_title)).thenReturn("Unable to complete operation")
        val cause = IllegalStateException("raw endpoint details")
        val evidence = BittensorOperationResult.Failure(cause, stage = BittensorFailureStage.NOT_SUBMITTED)

        val result = evidence.mapResult(resources) as BittensorStakeResultUi.Failure

        assertEquals("Not submitted; nothing spent", result.description)
        assertFalse(result.error.contains(cause.message!!))
    }

    @Test
    fun `dispatch failure uses possible fee statement not nothing spent`() {
        `when`(resources.getString(R.string.common_failed)).thenReturn("Failed")
        `when`(resources.getString(R.string.bittensor_result_dispatched_failure)).thenReturn("Execution failed; fee may have been charged")
        `when`(resources.getString(R.string.bittensor_result_transaction_hash, "hash")).thenReturn("Transaction hash: hash")
        val cause = IllegalStateException("raw dispatch details")
        val evidence = BittensorOperationResult.Failure(cause, "hash", BittensorFailureStage.DISPATCHED)

        val result = evidence.mapResult(resources) as BittensorStakeResultUi.Failure

        assertEquals("Execution failed; fee may have been charged", result.description)
        assertEquals("Transaction hash: hash", result.error)
        assertEquals(listOf(BittensorResultAction.CLOSE, BittensorResultAction.TRY_AGAIN), result.actions)
    }

    @Test
    fun `dispatch error a retry cannot change offers only close`() {
        `when`(resources.getString(R.string.common_failed)).thenReturn("Failed")
        `when`(resources.getString(R.string.bittensor_result_dispatched_failure)).thenReturn("Execution failed; fee may have been charged")
        `when`(resources.getString(R.string.bittensor_result_transaction_hash, "hash")).thenReturn("Transaction hash: hash")
        val module = mock(Module::class.java)
        `when`(module.name).thenReturn("SubtensorModule")
        val error = mock(ErrorMetadata::class.java)
        `when`(error.name).thenReturn("HotKeyAccountNotExists")
        val evidence = BittensorOperationResult.Failure(DispatchError.Module(module, error), "hash", BittensorFailureStage.DISPATCHED)

        val result = evidence.mapResult(resources) as BittensorStakeResultUi.Failure

        assertFalse(result.canRetry)
        assertEquals(listOf(BittensorResultAction.CLOSE), result.actions)
    }

    @Test
    fun `unconfirmed result offers the landing actions`() {
        `when`(resources.getString(R.string.bittensor_result_unconfirmed_title)).thenReturn("Unconfirmed")
        `when`(resources.getString(R.string.bittensor_result_unconfirmed_description)).thenReturn("Outcome and charges unknown")
        val landing = listOf(BittensorResultAction.YOUR_BITTENSOR, BittensorResultAction.BACK_TO_POSITION)

        val result = BittensorOperationResult.Failure(IllegalStateException("unknown phase")).mapResult(resources, landing)

        assertEquals(landing, result.actions)
    }
}
