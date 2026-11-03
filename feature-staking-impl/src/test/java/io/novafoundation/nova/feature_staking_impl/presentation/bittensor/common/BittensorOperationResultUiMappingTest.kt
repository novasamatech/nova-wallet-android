package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.common.data.network.runtime.binding.DispatchError
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorOperationResult
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorFailureStage
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.mev.BittensorMevCarrier
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.mev.BittensorMevNotExecuted
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.mev.BittensorMevNotReady
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorSubnetReviewPayload
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
import java.math.BigDecimal
import java.math.BigInteger

class BittensorOperationResultUiMappingTest {

    private val resources = mock(ResourceManager::class.java)

    @Before
    fun setup() {
        `when`(resources.getString(R.string.common_failed)).thenReturn("Failed")
        `when`(resources.getString(R.string.common_error_general_title)).thenReturn("Unable to complete operation")
        `when`(resources.getString(R.string.bittensor_result_fee_charged)).thenReturn("A network fee may have been charged.")
        `when`(resources.getString(R.string.bittensor_result_price_moved)).thenReturn("The price moved beyond your slippage tolerance.")
        `when`(resources.getString(R.string.bittensor_result_unconfirmed_title)).thenReturn("Unconfirmed")
        `when`(resources.getString(R.string.bittensor_result_unconfirmed_description)).thenReturn("Outcome and charges unknown")
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
    fun `both slippage errors pass the selected tolerance and actual settings screen into the reason`() {
        listOf("Swap" to "PriceLimitExceeded", "SubtensorModule" to "SlippageTooHigh").forEach { (pallet, error) ->
            listOf("0.5%", "1.23%", "3%").forEach { selectedSlippage ->
                val reason = stubSlippageReason(selectedSlippage)
                val failure = dispatchFailure(pallet, error)

                val state = failure.toSubmissionState(resources, slippage = selectedSlippage) as BittensorSubmissionState.Failed
                val result = state.result as BittensorStakeResultUi.Failure

                assertEquals(reason, result.error)
                assertEquals("A network fee may have been charged.", result.description)
                assertTrue(result.canRetry)
                assertFalse((result.error + result.description).contains("transaction-hash"))
            }
        }
    }

    @Test
    fun `price limit error without a tolerance still explains the reason`() {
        listOf("Swap" to "PriceLimitExceeded", "SubtensorModule" to "SlippageTooHigh").forEach { (pallet, error) ->
            val result = dispatchFailure(pallet, error).mapResult(resources) as BittensorStakeResultUi.Failure

            assertEquals("The price moved beyond your slippage tolerance.", result.error)
        }
    }

    @Test
    fun `unrelated pallet is not presented as a slippage error`() {
        val result = dispatchFailure("OtherPallet", "PriceLimitExceeded").mapResult(resources, slippage = "0.5%") as BittensorStakeResultUi.Failure

        assertEquals("OtherPallet.PriceLimitExceeded", result.error)
    }

    @Test
    fun `slippage errors with uncertain execution do not claim rejection or offer retry`() {
        val landing = listOf(BittensorResultAction.YOUR_BITTENSOR, BittensorResultAction.BACK_TO_POSITION)
        listOf("Swap" to "PriceLimitExceeded", "SubtensorModule" to "SlippageTooHigh").forEach { (pallet, error) ->
            val failure = dispatchFailure(pallet, error)
            listOf(
                failure.copy(stage = BittensorFailureStage.UNKNOWN),
                failure.copy(transactionHash = null)
            ).forEach { uncertain ->
                val state = uncertain.toSubmissionState(resources, landing, slippage = "0.5%") as BittensorSubmissionState.Unconfirmed
                val result = state.result as BittensorStakeResultUi.Unconfirmed

                assertEquals("Outcome and charges unknown", result.description)
                assertEquals(landing, result.actions)
                assertFalse(state.canSubmit)
                assertFalse(state.dismissResult().canSubmit)
                assertFalse(BittensorResultAction.TRY_AGAIN in result.actions)
            }
        }
    }

    @Test
    fun `a price limit rejection before submission does not claim a charged fee`() {
        `when`(resources.getString(R.string.bittensor_result_not_submitted)).thenReturn("Not submitted; nothing spent")
        val reason = stubSlippageReason("0.5%")
        val failure = dispatchFailure("Swap", "PriceLimitExceeded").copy(stage = BittensorFailureStage.NOT_SUBMITTED)

        val result = failure.mapResult(resources, slippage = "0.5%") as BittensorStakeResultUi.Failure

        assertEquals(reason, result.error)
        assertEquals("Not submitted; nothing spent", result.description)
    }

    @Test
    fun `shield non execution keeps the carrier fee explanation without exposing a hash`() {
        `when`(resources.getString(R.string.bittensor_mev_not_executed)).thenReturn("Order not executed")
        `when`(resources.getString(R.string.bittensor_mev_unchanged)).thenReturn("Your balances are unchanged.")
        `when`(resources.getString(R.string.bittensor_mev_nothing_spent)).thenReturn("Nothing spent.")
        `when`(resources.getString(R.string.bittensor_result_dispatched_failure)).thenReturn("Carrier fee may have been charged.")
        `when`(resources.getString(R.string.bittensor_mev_fee_may_be_charged)).thenReturn("Carrier fee is unknown.")
        val messages = mapOf(
            BittensorMevCarrier.ABSENT to "Nothing spent.",
            BittensorMevCarrier.INCLUDED to "Carrier fee may have been charged.",
            BittensorMevCarrier.UNKNOWN to "Carrier fee is unknown."
        )
        messages.forEach { (carrier, message) ->
            val failure = BittensorOperationResult.Failure(
                BittensorMevNotExecuted(carrier),
                "transaction-hash",
                BittensorFailureStage.NOT_EXECUTED
            )

            val result = failure.mapResult(resources, slippage = "0.5%") as BittensorStakeResultUi.Failure

            assertEquals("Order not executed", result.title)
            assertEquals("Your balances are unchanged.", result.error)
            assertEquals(message, result.description)
            assertFalse((result.error + result.description).contains("transaction-hash"))
            assertTrue(result.canRetry)
        }
    }

    @Test
    fun `MEV readiness failure retains its explanation and reports nothing spent`() {
        `when`(resources.getString(R.string.bittensor_mev_not_executed)).thenReturn("Order not executed")
        `when`(resources.getString(R.string.bittensor_mev_unchanged)).thenReturn("Order not executed; stake unchanged")
        `when`(resources.getString(R.string.bittensor_mev_nothing_spent)).thenReturn("Nothing was spent")
        `when`(resources.getString(R.string.bittensor_mev_unavailable)).thenReturn("MEV protection unavailable")
        val cause = BittensorMevNotReady()
        val failure = BittensorOperationResult.Failure(cause, stage = BittensorFailureStage.NOT_SUBMITTED)

        val result = failure.mapResult(resources, slippage = "0.5%") as BittensorStakeResultUi.Failure

        assertEquals("Order not executed", result.title)
        assertEquals("MEV protection unavailable", result.error)
        assertEquals("Order not executed; stake unchanged Nothing was spent", result.description)
        assertTrue(result.canRetry)
    }

    @Test
    fun `failure message uses the tolerance captured by the reviewed purchase`() {
        val payload = BittensorSubnetReviewPayload(92, BigInteger.TEN, 1, "validator", BigDecimal("0.0123"))
        val selectedSlippage = payload.slippage.movePointRight(2).stripTrailingZeros().toPlainString() + "%"
        val reason = stubSlippageReason("1.23%")
        val failure = dispatchFailure("Swap", "PriceLimitExceeded")

        val result = failure.toSubmissionState(resources, slippage = selectedSlippage).result as BittensorStakeResultUi.Failure

        assertEquals(reason, result.error)
        assertEquals(BigDecimal("0.0123"), payload.slippage)
    }

    private fun stubSlippageReason(slippage: String): String {
        val reason = "Order not executed beyond $slippage. Change slippage using the settings icon on the amount entry screen and try again."
        `when`(resources.getString(R.string.bittensor_result_price_moved_with_tolerance, slippage)).thenReturn(reason)
        return reason
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
