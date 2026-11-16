package io.novafoundation.nova.feature_swap_impl.domain

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.feature_account_api.data.model.SubstrateFeeBase
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicSwapOperation
import io.novafoundation.nova.feature_swap_api.domain.model.NovaSwapCommission
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFee
import io.novafoundation.nova.feature_swap_api.domain.model.SwapLimit
import io.novafoundation.nova.feature_swap_api.domain.model.buildSwapSegments
import io.novafoundation.nova.feature_swap_api.domain.model.fee.AtomicSwapOperationFee
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.assetConversion.AssetHubCommissionResolver
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationPayload
import io.novafoundation.nova.feature_swap_impl.domain.validation.validations.SwapIntermediateReceivesMeetEDValidation
import io.novafoundation.nova.feature_swap_impl.domain.validation.validations.SwapSufficientAmountOutToStayAboveEDValidation
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.domain.validation.context.AssetsValidationContext
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigInteger

class SwapOutputEdValidationTest {

    private val context = mock(AssetsValidationContext::class.java)

    private val output = mock(Chain.Asset::class.java)

    private val token = mock(Token::class.java)

    private val asset = mock(Asset::class.java)

    private val fee = mock(SwapFee::class.java)

    private val operationFee = mock(AtomicSwapOperationFee::class.java)

    private val operation = mock(AtomicSwapOperation::class.java)

    private val payload = mock(SwapValidationPayload::class.java)

    private val commission = NovaSwapCommission()

    private val resolver = AssetHubCommissionResolver(commission)

    private val outputValidation = SwapSufficientAmountOutToStayAboveEDValidation(context)

    private val intermediateValidation = SwapIntermediateReceivesMeetEDValidation(context)

    private val outputId = FullChainAssetId("asset-hub", 0)

    @Test
    fun `small DED to DOT swap retains commission and accepts funded DOT account`() = runTest {
        val limit = SwapLimit.SpecifiedIn(
            amountIn = BigInteger.valueOf(46_999999),
            amountOutQuote = BigInteger.valueOf(750_000),
            amountOutMin = BigInteger.valueOf(746_250),
        )
        prepare(limit, minimumBalance = 100_000_000)
        `when`(asset.balanceCountedTowardsEDInPlanks).thenReturn(BigInteger.valueOf(6_144_100_000))

        assertTrue(outputValidation.validate(payload) is ValidationStatus.Valid)
        assertTrue(fee.segments.single().netFlow.amountOut < limit.amountOutQuote)

        // Removing the existing DOT balance must still fail normal output validation.
        `when`(asset.balanceCountedTowardsEDInPlanks).thenReturn(BigInteger.ZERO)
        assertTrue(outputValidation.validate(payload) is ValidationStatus.NotValid)
    }

    @Test
    fun `existing balance plus net minimum must meet ED exactly or exceed it`() = runTest {
        val limit = SwapLimit.SpecifiedIn(BigInteger.valueOf(100), BigInteger.valueOf(1_000), BigInteger.valueOf(100))
        prepare(limit, minimumBalance = 100)
        assertEquals(BigInteger.valueOf(99), fee.segments.single().netFlow.amountOutMin)

        `when`(asset.balanceCountedTowardsEDInPlanks).thenReturn(BigInteger.ZERO)
        assertTrue(outputValidation.validate(payload) is ValidationStatus.NotValid)

        `when`(asset.balanceCountedTowardsEDInPlanks).thenReturn(BigInteger.ONE)
        assertTrue(outputValidation.validate(payload) is ValidationStatus.Valid)

        `when`(asset.balanceCountedTowardsEDInPlanks).thenReturn(BigInteger.TWO)
        assertTrue(outputValidation.validate(payload) is ValidationStatus.Valid)
    }

    @Test
    fun `intermediate output uses the same funded account rule`() = runTest {
        val limit = SwapLimit.SpecifiedIn(BigInteger.valueOf(100), BigInteger.valueOf(1_000), BigInteger.valueOf(100))
        prepare(limit, minimumBalance = 100)
        val segment = fee.segments.single()
        `when`(fee.segments).thenReturn(listOf(segment, segment))

        `when`(asset.balanceCountedTowardsEDInPlanks).thenReturn(BigInteger.ZERO)
        assertTrue(intermediateValidation.validate(payload) is ValidationStatus.NotValid)

        `when`(asset.balanceCountedTowardsEDInPlanks).thenReturn(BigInteger.ONE)
        assertTrue(intermediateValidation.validate(payload) is ValidationStatus.Valid)
    }

    private suspend fun prepare(limit: SwapLimit, minimumBalance: Long) {
        val commissionAmount = requireNotNull(resolver.resolveRequired(limit))
        `when`(operation.estimatedSwapLimit).thenReturn(limit)
        `when`(operation.assetOut).thenReturn(outputId)
        `when`(operationFee.serviceCommission).thenReturn(SubstrateFeeBase(commissionAmount, output))
        val segments = buildSwapSegments(listOf(operationFee), listOf(operation))
        `when`(fee.segments).thenReturn(segments)
        `when`(payload.fee).thenReturn(fee)
        `when`(context.getExistentialDeposit(outputId)).thenReturn(BigInteger.valueOf(minimumBalance))
        `when`(context.getAsset(outputId)).thenReturn(asset)
        `when`(asset.token).thenReturn(token)
        `when`(token.configuration).thenReturn(output)
    }
}
