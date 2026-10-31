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
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class BittensorOperationResultUiMappingTest {

    private val resources = mock(ResourceManager::class.java)

    @Before
    fun setup() {
        `when`(resources.getString(R.string.common_failed)).thenReturn("Failed")
        `when`(resources.getString(R.string.common_error_general_title)).thenReturn("Unable to complete operation")
        `when`(resources.getString(R.string.bittensor_result_fee_charged)).thenReturn("Only the network fee was charged.")
        `when`(resources.getString(R.string.bittensor_result_price_moved)).thenReturn("Order cancelled · price moved beyond your limit")
        `when`(resources.getString(R.string.bittensor_result_price_moved_format, "0.5%"))
            .thenReturn("Order cancelled · price moved > 0.5%")
    }

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
        `when`(resources.getString(R.string.bittensor_result_nothing_spent)).thenReturn("Not submitted; nothing spent")
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
        `when`(resources.getString(R.string.bittensor_result_fee_charged)).thenReturn("Execution failed; fee may have been charged")
        val cause = IllegalStateException("raw dispatch details")
        val evidence = BittensorOperationResult.Failure(cause, "hash", BittensorFailureStage.DISPATCHED)

        val result = evidence.mapResult(resources) as BittensorStakeResultUi.Failure

        assertEquals("Execution failed; fee may have been charged", result.description)
        assertEquals("Unable to complete operation", result.error)
        assertFalse(result.error.contains("hash"))
        assertEquals(listOf(BittensorResultAction.CLOSE, BittensorResultAction.TRY_AGAIN), result.actions)
    }

    @Test
    fun `dispatch error a retry cannot change offers only close`() {
        `when`(resources.getString(R.string.common_failed)).thenReturn("Failed")
        `when`(resources.getString(R.string.bittensor_result_fee_charged)).thenReturn("Execution failed; fee may have been charged")
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
    fun `price limit failure shows the chosen tolerance without the transaction hash`() {
        val failure = dispatchFailure("Swap", "PriceLimitExceeded")

        val state = failure.toSubmissionState(resources, slippage = "0.5%") as BittensorSubmissionState.Failed
        val result = state.result as BittensorStakeResultUi.Failure

        assertEquals("Order cancelled · price moved > 0.5%", result.error)
        assertEquals("Only the network fee was charged.", result.description)
        assertFalse(result.error.contains("transaction-hash"))
        assertFalse(result.description.contains("transaction-hash"))
        org.junit.Assert.assertTrue(result.canRetry)
    }

    @Test
    fun `slippage failure without a display tolerance still explains the price limit`() {
        val failure = dispatchFailure("SubtensorModule", "SlippageTooHigh")

        val result = failure.mapResult(resources) as BittensorStakeResultUi.Failure

        assertEquals("Order cancelled · price moved beyond your limit", result.error)
    }

    @Test
    fun `unrelated pallet error is not presented as a price move`() {
        val failure = dispatchFailure("OtherPallet", "PriceLimitExceeded")

        val result = failure.mapResult(resources, slippage = "0.5%") as BittensorStakeResultUi.Failure

        assertEquals("OtherPallet.PriceLimitExceeded", result.error)
    }

    private fun dispatchFailure(pallet: String, name: String): BittensorOperationResult.Failure {
        val module = mock(Module::class.java)
        `when`(module.name).thenReturn(pallet)
        val metadata = mock(ErrorMetadata::class.java)
        `when`(metadata.name).thenReturn(name)
        return BittensorOperationResult.Failure(DispatchError.Module(module, metadata), "transaction-hash", BittensorFailureStage.DISPATCHED)
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
