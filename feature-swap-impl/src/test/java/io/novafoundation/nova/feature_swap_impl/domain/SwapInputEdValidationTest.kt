package io.novafoundation.nova.feature_swap_impl.domain

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.utils.Precision
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationFailure
import io.novafoundation.nova.feature_swap_impl.domain.validation.enoughAssetInToPayForSwapAndFee
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFee
import io.novafoundation.nova.feature_swap_api.domain.model.SwapMaxAdditionalAmountDeduction
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationPayload
import io.novafoundation.nova.feature_swap_impl.domain.validation.validations.SwapKeepsNecessaryAmountOfAssetIn
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.ChainAssetWithAmount
import io.novafoundation.nova.feature_wallet_api.domain.validation.context.AssetsValidationContext
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import java.math.BigDecimal
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class SwapInputEdValidationTest {

    private val context = mock(AssetsValidationContext::class.java)
    private val input = mock(Chain.Asset::class.java)
    private val output = mock(Chain.Asset::class.java)
    private val asset = mock(Asset::class.java)
    private val fee = mock(SwapFee::class.java)
    private val payload = mock(SwapValidationPayload::class.java)
    private val validation = SwapKeepsNecessaryAmountOfAssetIn(context)

    @Test
    fun `fee amount validation accepts Max with covered ED and suggests the same Max`() = runBlocking {
        val transferable = BigInteger("4714246059")
        val locked = BigInteger("5000000000")
        val totalFees = BigInteger("13600000")
        val ed = BigInteger("100000000")
        val deduction = SwapMaxAdditionalAmountDeduction(ed)
        `when`(input.precision).thenReturn(Precision(10))
        `when`(context.getAsset(input)).thenReturn(asset)
        `when`(asset.transferableInPlanks).thenReturn(transferable)
        `when`(asset.balanceCountedTowardsEDInPlanks).thenReturn(transferable + locked)
        `when`(fee.additionalMaxAmountDeduction).thenReturn(deduction)
        `when`(fee.totalFeeAmount(input)).thenReturn(totalFees)
        `when`(payload.fee).thenReturn(fee)
        val system = ValidationSystem<SwapValidationPayload, SwapValidationFailure> {
            enoughAssetInToPayForSwapAndFee(context)
        }
        val maximum = swapMaximum(transferable, transferable + locked, ed, totalFees)
        `when`(payload.amountIn).thenReturn(ChainAssetWithAmount(input, maximum))
        assertTrue(system.validate(payload).getOrThrow() is ValidationStatus.Valid)

        `when`(payload.amountIn).thenReturn(ChainAssetWithAmount(input, maximum + BigInteger.ONE))
        val status = system.validate(payload).getOrThrow() as ValidationStatus.NotValid
        val failure = status.reason as SwapValidationFailure.InsufficientBalance.CannotPayFeeDueToAmount
        assertEquals(0, failure.maxSwapAmount.compareTo(BigDecimal("0.4700646059")))

        // Without an ED-covering balance, the same amount must still be rejected.
        `when`(asset.balanceCountedTowardsEDInPlanks).thenReturn(transferable)
        `when`(payload.amountIn).thenReturn(ChainAssetWithAmount(input, maximum))
        assertTrue(system.validate(payload).getOrThrow() is ValidationStatus.NotValid)
        `when`(payload.amountIn).thenReturn(ChainAssetWithAmount(input, maximum - ed))
        assertTrue(system.validate(payload).getOrThrow() is ValidationStatus.Valid)
    }

    @Test
    fun `fee-only max is rejected while amount leaving ED after all fees passes`() = runBlocking {
        val balance = BigInteger("10001000000")
        val totalFees = BigInteger("13523000")
        val ed = BigInteger("100000000")
        val deduction = SwapMaxAdditionalAmountDeduction(ed)
        `when`(context.getAsset(input)).thenReturn(asset)
        `when`(asset.balanceCountedTowardsEDInPlanks).thenReturn(balance)
        `when`(fee.additionalMaxAmountDeduction).thenReturn(deduction)
        `when`(fee.totalFeeAmount(input)).thenReturn(totalFees)
        `when`(payload.fee).thenReturn(fee)
        `when`(payload.amountOut).thenReturn(ChainAssetWithAmount(output, BigInteger.ONE))

        `when`(payload.amountIn).thenReturn(ChainAssetWithAmount(input, balance - totalFees))
        assertTrue(validation.validate(payload) is ValidationStatus.NotValid)

        `when`(payload.amountIn).thenReturn(ChainAssetWithAmount(input, balance - totalFees - ed))
        assertTrue(validation.validate(payload) is ValidationStatus.Valid)

        `when`(payload.amountIn).thenReturn(ChainAssetWithAmount(input, balance - totalFees - ed + BigInteger.ONE))
        assertTrue(validation.validate(payload) is ValidationStatus.NotValid)
    }
}
