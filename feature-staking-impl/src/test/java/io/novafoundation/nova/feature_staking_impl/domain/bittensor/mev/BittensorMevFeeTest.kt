package io.novafoundation.nova.feature_staking_impl.domain.bittensor.mev

import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorOperationResult
import io.novafoundation.nova.feature_account_api.data.extrinsic.SubmissionOrigin
import io.novafoundation.nova.feature_account_api.data.model.SubstrateFee
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.maximumBittensorStake
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorMevSwitchState
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorFeeEstimate
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.bittensorMevSwitchState
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.forMevSelection
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeStatus
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.loadedFeeOrNull
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock

class BittensorMevFeeTest {

    private val asset = mock(Chain.Asset::class.java)

    private val origin = SubmissionOrigin(ByteArray(32), ByteArray(32))

    private val inner = SubstrateFee(100.toBigInteger(), origin, asset)

    private val carrier = SubstrateFee(25.toBigInteger(), origin, asset)

    private val quote = BittensorMevQuote(inner, carrier)

    @Test
    fun `both fees and deposit must be payable in native balance at the exact boundary`() {
        assertFalse(BittensorMevFunds(134.toBigInteger(), 10.toBigInteger()).canPay(quote))
        assertTrue(BittensorMevFunds(135.toBigInteger(), 10.toBigInteger()).canPay(quote))
        assertEquals(125.toBigInteger(), quote.totalFee.amount)
    }

    @Test
    fun `Max additionally reserves the carrier fee`() {
        val available = 100_000_000.toBigInteger()
        val clearMax = maximumBittensorStake(available, inner.amount)
        val protectedMax = maximumBittensorStake(available, quote.totalFee.amount)
        assertEquals(carrier.amount, clearMax - protectedMax)
    }

    @Test
    fun `an unexecuted inner charges only the carrier and a proven absent carrier charges nothing`() {
        val estimate = BittensorMevReviewFee(inner, true, BittensorMevAvailability.Available(quote))
        val included = BittensorOperationResult.Failure(BittensorMevNotExecuted(BittensorMevCarrier.INCLUDED, 23.toBigInteger()))
        val absent = BittensorOperationResult.Failure(BittensorMevNotExecuted(BittensorMevCarrier.ABSENT))
        val unknown = BittensorOperationResult.Failure(BittensorMevNotExecuted(BittensorMevCarrier.UNKNOWN))
        assertEquals(23.toBigInteger(), included.mevPaidFee(estimate)?.amount)
        assertTrue(included.mevPaidFee(estimate) is BittensorMevPaidFee.Known)
        assertEquals(java.math.BigInteger.ZERO, absent.mevPaidFee(estimate)?.amount)
        assertTrue(absent.mevPaidFee(estimate) is BittensorMevPaidFee.Known)
        assertEquals(carrier.amount, unknown.mevPaidFee(estimate)?.amount)
        assertTrue(unknown.mevPaidFee(estimate) is BittensorMevPaidFee.Estimated)
    }

    @Test
    fun `switch selection and the fee used for submission stay aligned`() {
        val protected = BittensorMevReviewFee(inner, true, BittensorMevAvailability.Available(quote))
        val clear = BittensorMevReviewFee(inner, false, BittensorMevAvailability.Available(quote))
        assertEquals(quote.totalFee.amount, protected.amount)
        assertEquals(inner.amount, clear.amount)
        assertEquals(BittensorMevSwitchState.AVAILABLE_ON, bittensorMevSwitchState(true, true, protected))
        assertEquals(BittensorMevSwitchState.AVAILABLE_OFF, bittensorMevSwitchState(true, false, clear))
        assertEquals(BittensorMevSwitchState.LOADING, bittensorMevSwitchState(true, true, null))
        assertEquals(BittensorMevSwitchState.HIDDEN, bittensorMevSwitchState(false, true, null))
        val unavailable = BittensorMevReviewFee(inner, true, BittensorMevAvailability.Unavailable(BittensorMevUnavailableReason.KEY_UNAVAILABLE))
        assertEquals(BittensorMevSwitchState.UNAVAILABLE, bittensorMevSwitchState(true, true, unavailable))
    }

    @Test
    fun `protection can be switched off and back on before fee loading finishes`() {
        val loadingOn = bittensorMevSwitchState(true, true, null)
        assertTrue(loadingOn.checked)
        assertTrue(loadingOn.enabled)

        val loadingOff = bittensorMevSwitchState(true, false, null)
        assertFalse(loadingOff.checked)
        assertTrue(loadingOff.enabled)

        val loadingOnAgain = bittensorMevSwitchState(true, true, null)
        assertTrue(loadingOnAgain.checked)
        assertTrue(loadingOnAgain.enabled)
    }

    @Test
    fun `a late fee for the previous protection selection cannot enable confirmation`() {
        val protected = BittensorMevReviewFee(inner, true, BittensorMevAvailability.Available(quote))
        val clear = BittensorMevReviewFee(inner, false, BittensorMevAvailability.Available(quote))
        val protectedStatus = FeeStatus.Loaded(FeeModel(BittensorFeeEstimate(Unit, protected), Unit))
        val clearStatus = FeeStatus.Loaded(FeeModel(BittensorFeeEstimate(Unit, clear), Unit))

        assertNull(protectedStatus.forMevSelection(false).loadedFeeOrNull())
        assertEquals(inner.amount, clearStatus.forMevSelection(false).loadedFeeOrNull()?.amount)
        assertNull(clearStatus.forMevSelection(true).loadedFeeOrNull())
        assertEquals(quote.totalFee.amount, protectedStatus.forMevSelection(true).loadedFeeOrNull()?.amount)
    }
}
