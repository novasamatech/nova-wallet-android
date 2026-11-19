package io.novafoundation.nova.feature_swap_impl.domain

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFee
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapDustCheckFee
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationFailure.TooSmallRemainingBalance
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationPayload
import io.novafoundation.nova.feature_swap_impl.domain.validation.validations.SwapDoNotLooseAssetInDustValidation
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.ChainAssetWithAmount
import io.novafoundation.nova.feature_wallet_api.domain.validation.context.AssetsValidationContext
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class SwapDustValidationTest {

    private val context = mock(AssetsValidationContext::class.java)

    private val asset = mock(Asset::class.java)

    private val chainAsset = mock(Chain.Asset::class.java)

    private val fee = mock(SwapFee::class.java)

    private val payload = mock(SwapValidationPayload::class.java)

    private val validation = SwapDoNotLooseAssetInDustValidation(context)

    @Test
    fun `Max fee reserve alone does not trigger the dust warning`() = runBlocking {
        prepare(amountIn = 8_990, dustCheckFee = SwapDustCheckFee.MaxFeeWithMargin(1_010.toBigInteger()))

        assertTrue(validation.validate(payload) is ValidationStatus.Valid)
    }

    @Test
    fun `manual amount still warns for dust left after the latest fee`() = runBlocking {
        prepare(amountIn = 8_990)

        val failure = validation.validate(payload) as ValidationStatus.NotValid
        val reason = failure.reason as TooSmallRemainingBalance
        assertEquals(2.toBigInteger(), reason.remainingBalance)
    }

    @Test
    fun `Max still warns for dust beyond the fee reserve`() = runBlocking {
        prepare(amountIn = 8_950, dustCheckFee = SwapDustCheckFee.MaxFeeWithMargin(1_010.toBigInteger()))

        val failure = validation.validate(payload) as ValidationStatus.NotValid
        val reason = failure.reason as TooSmallRemainingBalance
        assertEquals(40.toBigInteger(), reason.remainingBalance)
    }

    private suspend fun prepare(amountIn: Long, dustCheckFee: SwapDustCheckFee = SwapDustCheckFee.QuoteFee) {
        `when`(chainAsset.chainId).thenReturn("hub")
        `when`(context.getAsset(chainAsset)).thenReturn(asset)
        `when`(context.getExistentialDeposit(FullChainAssetId("hub", 0))).thenReturn(100.toBigInteger())
        `when`(asset.balanceCountedTowardsEDInPlanks).thenReturn(10_000.toBigInteger())
        `when`(fee.totalFeeAmount(chainAsset)).thenReturn(1_008.toBigInteger())
        `when`(payload.fee).thenReturn(fee)
        `when`(payload.dustCheckFee).thenReturn(dustCheckFee)
        `when`(payload.amountIn).thenReturn(ChainAssetWithAmount(chainAsset, amountIn.toBigInteger()))
    }
}
