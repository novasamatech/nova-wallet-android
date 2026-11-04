package io.novafoundation.nova.feature_swap_impl.domain.validation

import io.novafoundation.nova.common.utils.Fraction
import io.novafoundation.nova.common.utils.Fraction.Companion.percents
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentCurrency
import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentCurrency.Asset.Companion.toFeePaymentCurrency
import io.novafoundation.nova.feature_account_api.data.model.SubstrateFeeBase
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicSwapOperation
import io.novafoundation.nova.feature_swap_api.domain.model.SubmissionFeeWithLabel
import io.novafoundation.nova.feature_swap_api.domain.model.SwapExecutionEstimate
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFee
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFeeQuoteIdentity
import io.novafoundation.nova.feature_swap_api.domain.model.SwapGraphEdge
import io.novafoundation.nova.feature_swap_api.domain.model.SwapMaxAdditionalAmountDeduction
import io.novafoundation.nova.feature_swap_api.domain.model.SwapPoolId
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuote
import io.novafoundation.nova.feature_swap_api.domain.model.fee.AtomicSwapOperationFee
import io.novafoundation.nova.feature_swap_api.domain.model.matchesQuote
import io.novafoundation.nova.feature_swap_api.domain.model.quoteIdentity
import io.novafoundation.nova.feature_swap_api.domain.model.toExecuteArgs
import io.novafoundation.nova.feature_swap_api.domain.swap.SwapService
import io.novafoundation.nova.feature_swap_core_api.data.paths.model.PathRoughFeeEstimation
import io.novafoundation.nova.feature_swap_core_api.data.paths.model.QuotedEdge
import io.novafoundation.nova.feature_swap_core_api.data.paths.model.QuotedPath
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.SwapDirection
import io.novafoundation.nova.feature_swap_impl.data.network.blockhain.updaters.SwapUpdateSystemFactory
import io.novafoundation.nova.feature_swap_impl.domain.interactor.SwapInteractor
import io.novafoundation.nova.feature_swap_impl.domain.swap.PriceImpactThresholds
import io.novafoundation.nova.feature_swap_impl.domain.validation.validations.CanReceiveAssetOutValidationFactory
import io.novafoundation.nova.feature_swap_impl.domain.validation.validations.SwapQuoteFeeConsistencyValidation
import io.novafoundation.nova.feature_swap_impl.presentation.common.state.SwapState
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.ShowReceivedAssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TokenRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.ChainAssetWithAmount
import io.novafoundation.nova.feature_wallet_api.domain.validation.context.AssetsValidationContext
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import java.math.BigInteger
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.`when`
import kotlin.time.Duration

class SwapQuoteFeeConsistencyValidationTest {

    private val assetIn = asset(1)

    private val assetOut = asset(2)

    private val intermediateAsset = asset(3)

    private val slippage = 1.percents

    private val validation = SwapQuoteFeeConsistencyValidation()

    private val swapService = mock(SwapService::class.java)

    private val thresholds = mock(PriceImpactThresholds::class.java)

    private val tokenRepository = mock(TokenRepository::class.java)

    private val updateSystemFactory = mock(SwapUpdateSystemFactory::class.java)

    private val contextFactory = mock(AssetsValidationContext.Factory::class.java)

    private val context = mock(AssetsValidationContext::class.java)

    private val receiveValidationFactory = mock(CanReceiveAssetOutValidationFactory::class.java)

    private val showReceivedAsset = mock(ShowReceivedAssetUseCase::class.java)

    private val interactor = SwapInteractor(
        thresholds, swapService, tokenRepository, updateSystemFactory, contextFactory, receiveValidationFactory, showReceivedAsset
    )

    @Test
    fun `equivalent recreated edge and quoted path accept existing fee`() = runBlocking {
        val original = quote()
        val equivalent = quote() // Distinct edge and QuotedPath instances, identical execution identity.
        val fee = feeFor(original)

        assertTrue(fee.matchesQuote(equivalent, slippage))
        assertTrue(validation.validate(SwapValidationPayload(fee, equivalent, slippage)) is ValidationStatus.Valid)
    }

    @Test
    fun `same endpoints through a different pool reject fee`() = runBlocking {
        val original = quote()
        val anotherPool = quote(path = listOf(segment(pool = "another-pool")))
        assertMismatch(feeFor(original), anotherPool)
    }

    @Test
    fun `reversed assets reject the previous trade fee`() = runBlocking {
        val original = quote()
        val reverse = quote(
            path = listOf(segment(from = assetOut.fullId, to = assetIn.fullId)),
            inputAsset = assetOut,
            outputAsset = assetIn
        )
        assertMismatch(feeFor(original), reverse)
    }

    @Test
    fun `buy versus sell rejects even with same assets and numeric amounts`() = runBlocking {
        val original = quote()
        val buy = quote(direction = SwapDirection.SPECIFIED_OUT)
        assertMismatch(feeFor(original), buy)
    }

    @Test
    fun `changed quoted amount rejects the old fee`() = runBlocking {
        val original = quote()
        val changed = quote(path = listOf(segment(amount = 101)))
        assertMismatch(feeFor(original), changed)
    }

    @Test
    fun `changed quote output rejects the old fee`() = runBlocking {
        val original = quote()
        val changed = quote(path = listOf(segment(output = 201)))
        assertMismatch(feeFor(original), changed)
    }

    @Test
    fun `changed displayed net input rejects the fee while gross path stays identical`() = runBlocking {
        for (direction in SwapDirection.entries) {
            val original = quote(direction = direction)
            val altered = original.copy(amountIn = ChainAssetWithAmount(assetIn, 101.bi()))
            assertSame(original.quotedPath, altered.quotedPath)
            assertMismatch(feeFor(original), altered)
        }
    }

    @Test
    fun `changed displayed net output rejects the fee while gross path stays identical`() = runBlocking {
        for (direction in SwapDirection.entries) {
            val original = quote(direction = direction)
            val altered = original.copy(amountOut = ChainAssetWithAmount(assetOut, 199.bi()))
            assertSame(original.quotedPath, altered.quotedPath)
            assertMismatch(feeFor(original), altered)
        }
    }

    @Test
    fun `net output after commission is bound separately from gross path quote`() = runBlocking {
        val grossQuote = quote()
        val netQuote = grossQuote.copy(amountOut = ChainAssetWithAmount(assetOut, 198.bi()))
        val fee = feeFor(netQuote)
        val args = netQuote.toExecuteArgs(slippage, FeePaymentCurrency.Native)

        assertSame(netQuote.amountIn, args.displayedAmountIn)
        assertSame(netQuote.amountOut, args.displayedAmountOut)
        assertEquals(200.bi(), args.executionPath.single().quotedSwapEdge.quote)
        assertEquals(198.bi(), args.quoteIdentity().displayedAmountOut)
        assertTrue(fee.matchesQuote(netQuote, slippage))
        assertTrue(validation.validate(SwapValidationPayload(fee, netQuote, slippage)) is ValidationStatus.Valid)
        assertMismatch(fee, grossQuote)
    }

    @Test
    fun `changed slippage rejects the old fee`() = runBlocking {
        assertMismatch(feeFor(quote()), quote(), 2.percents)
    }

    @Test
    fun `current payment currency must match the fee request`() {
        val quote = quote()
        val fee = feeFor(quote)
        assertTrue(fee.matchesQuote(quote, slippage, FeePaymentCurrency.Native))
        assertFalse(fee.matchesQuote(quote, slippage, assetOut.toFeePaymentCurrency()))
    }

    @Test
    fun `different intermediate path rejects same aggregate assets and amounts`() = runBlocking {
        val original = quote()
        val changed = quote(
            path = listOf(
                segment(to = intermediateAsset.fullId, pool = "first", output = 150),
                segment(from = intermediateAsset.fullId, pool = "second", amount = 150)
            )
        )
        assertMismatch(feeFor(original), changed)
    }

    @Test
    fun `legacy fee with missing identity cannot validate`() = runBlocking {
        val fee = mock(SwapFee::class.java)
        assertMismatch(fee, quote())
    }

    @Test
    fun `inconsistent quote direction and path cannot validate`() = runBlocking {
        val original = quote()
        assertMismatch(feeFor(original), original.copy(direction = SwapDirection.SPECIFIED_OUT))
    }

    @Test
    fun `confirmation restored with reversed fee fails first shared rule without submission`(): Unit = runBlocking {
        `when`(contextFactory.create()).thenReturn(context)
        val displayedQuote = quote()
        val reversedQuote = quote(
            path = listOf(segment(from = assetOut.fullId, to = assetIn.fullId)),
            inputAsset = assetOut,
            outputAsset = assetIn
        )
        val fee = feeFor(reversedQuote)
        val storedState = SwapState(displayedQuote, fee, slippage)
        val confirmationPayload = SwapValidationPayload(storedState.fee, storedState.quote, storedState.slippage)

        val status = interactor.validationSystem().validate(confirmationPayload).getOrThrow()
        assertTrue(status is ValidationStatus.NotValid)
        assertSame(SwapValidationFailure.QuoteFeeMismatch, (status as ValidationStatus.NotValid).reason)
        verify(swapService, never()).submitFirstSwapStep(fee)
        verify(swapService, never()).swap(fee)
    }

    @Test
    fun `fee estimate retains the exact operations and identity used for confirmation`() = runBlocking {
        val quote = quote()
        val args = quote.toExecuteArgs(slippage, FeePaymentCurrency.Native)
        val operation = mock(AtomicSwapOperation::class.java)
        val operationFee = mock(AtomicSwapOperationFee::class.java)
        val submissionFee = mock(SubmissionFeeWithLabel::class.java)
        `when`(operationFee.submissionFee).thenReturn(submissionFee)
        `when`(operationFee.postSubmissionFees).thenReturn(AtomicSwapOperationFee.PostSubmissionFees())
        val segments = listOf(
            SwapFee.SwapSegment(operationFee, operation, SwapFee.SwapSegment.SegmentNetFlow(100.bi(), 200.bi(), 198.bi()))
        )
        val convertedFee = SubstrateFeeBase(BigInteger.ZERO, assetIn)
        val deduction = SwapMaxAdditionalAmountDeduction(BigInteger.ZERO)
        val estimated = SwapFee(segments, convertedFee, deduction)
        `when`(swapService.estimateFee(args)).thenReturn(estimated)

        val bound = interactor.estimateFee(args)
        assertSame(segments, bound.segments)
        assertSame(operation, bound.segments.single().operation)
        assertSame(convertedFee, bound.intermediateSegmentFeesInAssetIn)
        assertSame(deduction, bound.additionalMaxAmountDeduction)
        assertEquals(args.quoteIdentity(), bound.quoteIdentity)
        assertTrue(bound.matchesQuote(quote, slippage))
        verify(swapService).estimateFee(args)
        verifyNoMoreInteractions(swapService)
    }

    private suspend fun assertMismatch(fee: SwapFee, quote: SwapQuote, currentSlippage: Fraction = slippage) {
        assertFalse(fee.matchesQuote(quote, currentSlippage))
        val status = validation.validate(SwapValidationPayload(fee, quote, currentSlippage))
        assertTrue(status is ValidationStatus.NotValid)
        assertSame(SwapValidationFailure.QuoteFeeMismatch, (status as ValidationStatus.NotValid).reason)
    }

    private fun feeFor(quote: SwapQuote): SwapFee {
        val identity: SwapFeeQuoteIdentity = quote.toExecuteArgs(slippage, FeePaymentCurrency.Native).quoteIdentity()
        val fee = mock(SwapFee::class.java)
        `when`(fee.quoteIdentity).thenReturn(identity)
        return fee
    }

    private fun quote(
        path: List<QuotedEdge<SwapGraphEdge>> = listOf(segment()),
        direction: SwapDirection = SwapDirection.SPECIFIED_IN,
        inputAsset: Chain.Asset = assetIn,
        outputAsset: Chain.Asset = assetOut
    ): SwapQuote {
        val quotedPath = QuotedPath(direction, path, PathRoughFeeEstimation.zero())
        return SwapQuote(
            ChainAssetWithAmount(inputAsset, 100.bi()),
            ChainAssetWithAmount(outputAsset, 200.bi()),
            Fraction.ZERO,
            quotedPath,
            SwapExecutionEstimate(emptyList(), Duration.ZERO),
            direction
        )
    }

    private fun segment(
        from: FullChainAssetId = assetIn.fullId,
        to: FullChainAssetId = assetOut.fullId,
        pool: String = "pool",
        amount: Int = 100,
        output: Int = 200
    ): QuotedEdge<SwapGraphEdge> {
        val edge = mock(SwapGraphEdge::class.java)
        `when`(edge.from).thenReturn(from)
        `when`(edge.to).thenReturn(to)
        `when`(edge.poolId).thenReturn(SwapPoolId("chain", pool))
        return QuotedEdge(amount.bi(), output.bi(), edge)
    }

    private fun asset(id: Int): Chain.Asset {
        val asset = mock(Chain.Asset::class.java)
        `when`(asset.chainId).thenReturn("chain")
        `when`(asset.id).thenReturn(id)
        return asset
    }

    private fun Int.bi() = toBigInteger()
}
