package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.common.data.network.runtime.binding.DispatchError
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorOperationResult
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorFailureStage
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.mev.BittensorMevCarrier
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.mev.BittensorMevNotExecuted
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.mev.BittensorMevNotReady
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model.BittensorResultAction
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model.BittensorStakeResultUi
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model.BittensorSubmissionState
import io.novasama.substrate_sdk_android.runtime.metadata.module.ErrorMetadata
import io.novasama.substrate_sdk_android.runtime.metadata.module.Module
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
        `when`(resources.getString(R.string.bittensor_result_price_moved_format, "1.23%"))
            .thenReturn("Order cancelled · price moved > 1.23%")
        `when`(resources.getString(R.string.bittensor_result_unconfirmed_title)).thenReturn("Unconfirmed")
        `when`(resources.getString(R.string.bittensor_result_unconfirmed_description)).thenReturn("Outcome and charges unknown")
        `when`(resources.getString(R.string.bittensor_mev_not_executed)).thenReturn("Order not executed")
        `when`(resources.getString(R.string.bittensor_mev_unchanged)).thenReturn("Order not executed; stake unchanged")
        `when`(resources.getString(R.string.bittensor_mev_nothing_spent)).thenReturn("Nothing was spent")
        `when`(resources.getString(R.string.bittensor_mev_unavailable)).thenReturn("MEV protection unavailable")
        `when`(resources.getString(R.string.bittensor_result_dispatched_failure)).thenReturn("Carrier included; network fee charged")
        `when`(resources.getString(R.string.bittensor_mev_fee_may_be_charged)).thenReturn("A network fee may have been charged")
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
        val failures = listOf(dispatchFailure("Swap", "PriceLimitExceeded"), dispatchFailure("SubtensorModule", "SlippageTooHigh"))
        failures.forEach { failure ->
            listOf("0.5%", "1.23%").forEach { slippage ->
                val state = failure.toSubmissionState(resources, slippage = slippage) as BittensorSubmissionState.Failed
                val result = state.result as BittensorStakeResultUi.Failure

                assertEquals("Order cancelled · price moved > $slippage", result.error)
                assertEquals("Only the network fee was charged.", result.description)
                assertFalse(result.error.contains("transaction-hash"))
                assertFalse(result.description.contains("transaction-hash"))
                assertTrue(result.canRetry)
            }
        }
    }

    @Test
    fun `unknown slippage outcomes remain neutral and cannot be retried`() {
        val failures = listOf(dispatchFailure("Swap", "PriceLimitExceeded"), dispatchFailure("SubtensorModule", "SlippageTooHigh"))
        failures.forEach { failure ->
            val unknown = failure.copy(stage = BittensorFailureStage.UNKNOWN)
            val withoutIdentity = failure.copy(transactionHash = null)
            listOf(unknown, withoutIdentity).forEach { evidence ->
                val state = evidence.toSubmissionState(resources, slippage = "0.5%") as BittensorSubmissionState.Unconfirmed
                val result = state.result as BittensorStakeResultUi.Unconfirmed

                assertEquals("Outcome and charges unknown", result.description)
                assertFalse(result.actions.contains(BittensorResultAction.TRY_AGAIN))
                assertFalse(state.canSubmit)
                assertFalse(state.dismissResult().canSubmit)
            }
        }
    }

    @Test
    fun `MEV non execution retains carrier fee explanation without exposing the transaction hash`() {
        val expectedDescriptions = mapOf(
            BittensorMevCarrier.ABSENT to "Nothing was spent",
            BittensorMevCarrier.INCLUDED to "Carrier included; network fee charged",
            BittensorMevCarrier.UNKNOWN to "A network fee may have been charged"
        )
        expectedDescriptions.forEach { (carrier, description) ->
            val cause = BittensorMevNotExecuted(carrier)
            val failure = BittensorOperationResult.Failure(cause, "transaction-hash", BittensorFailureStage.NOT_EXECUTED)

            val result = failure.mapResult(resources, slippage = "0.5%") as BittensorStakeResultUi.Failure

            assertEquals("Order not executed", result.title)
            assertEquals("Order not executed; stake unchanged", result.error)
            assertEquals(description, result.description)
            assertFalse(result.error.contains("transaction-hash"))
            assertFalse(result.description.contains("transaction-hash"))
            assertTrue(result.canRetry)
        }
    }

    @Test
    fun `MEV readiness failure retains its explanation and reports nothing spent`() {
        val cause = BittensorMevNotReady()
        val failure = BittensorOperationResult.Failure(cause, stage = BittensorFailureStage.NOT_SUBMITTED)

        val result = failure.mapResult(resources, slippage = "0.5%") as BittensorStakeResultUi.Failure

        assertEquals("Order not executed", result.title)
        assertEquals("MEV protection unavailable", result.error)
        assertEquals("Order not executed; stake unchanged Nothing was spent", result.description)
        assertTrue(result.canRetry)
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
