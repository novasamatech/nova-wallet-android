package io.novafoundation.nova.feature_swap_impl.domain

import io.novafoundation.nova.common.validation.DefaultFailureLevel
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicSwapOperation
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFee
import io.novafoundation.nova.feature_swap_api.domain.model.SwapLimit
import io.novafoundation.nova.feature_swap_api.domain.model.fee.AtomicSwapOperationFee
import io.novafoundation.nova.feature_swap_api.domain.model.SwapGraphEdge
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuote
import io.novafoundation.nova.feature_swap_core_api.data.paths.model.PathRoughFeeEstimation
import io.novafoundation.nova.feature_swap_core_api.data.paths.model.QuotedEdge
import io.novafoundation.nova.feature_swap_core_api.data.paths.model.QuotedPath
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.SwapDirection
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.TradeAmountLimitedEdge
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationFailure
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationPayload
import io.novafoundation.nova.feature_swap_impl.domain.validation.poolTradeLimits
import io.novafoundation.nova.feature_swap_impl.domain.validation.validations.SwapPoolTradeLimitsValidation
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.domain.validation.context.AssetsValidationContext
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class SwapPoolTradeLimitsValidationTest {

    private val assetsContext = mock(AssetsValidationContext::class.java)

    private val hopAsset = mock(Chain.Asset::class.java)

    private val token = mock(Token::class.java)

    private val asset = mock(Asset::class.java)

    private val validation = SwapPoolTradeLimitsValidation(assetsContext)

    @Test
    fun `sell blocks 306 and 999 but accepts equality and above without a maximum`() = runBlocking {
        assertMinimumBoundaries(SwapDirection.SPECIFIED_IN)
    }

    @Test
    fun `buy blocks 306 and 999 but accepts equality and above without a maximum`() = runBlocking {
        assertMinimumBoundaries(SwapDirection.SPECIFIED_OUT)
    }

    @Test
    fun `sell checks input of an intermediate hop even when user input and hop output exceed minimum`() = runBlocking {
        val direction = SwapDirection.SPECIFIED_IN
        val first = edge("a", "b")
        val intermediate = edge("b", "c", minIn = 1000)
        bindAsset(intermediate.from)
        val payload = payload(direction, quoted(first, 5000, 306), quoted(intermediate, 306, 5000))

        assertMinimumFailure(systemStatus(payload))
    }

    @Test
    fun `buy checks output of an intermediate hop even when user output and hop input exceed minimum`() = runBlocking {
        val direction = SwapDirection.SPECIFIED_OUT
        val intermediate = edge("a", "b", minOut = 1000)
        val last = edge("b", "c")
        bindAsset(intermediate.to)
        val payload = payload(direction, quoted(intermediate, 306, 5000), quoted(last, 5000, 306))

        assertMinimumFailure(systemStatus(payload))
    }

    @Test
    fun `quoted counterpart below minimum does not reject a valid specified amount`() = runBlocking {
        for (direction in SwapDirection.entries) {
            val edge = edge("a", "b", minIn = 1000, minOut = 1000)
            val payload = payload(direction, quoted(edge, 1000, 306))
            assertTrue(systemStatus(payload) is ValidationStatus.Valid)
        }
    }

    @Test
    fun `absent optional bounds remain unrestricted`() = runBlocking {
        val edge = edge("a", "b")
        for (direction in SwapDirection.entries) {
            assertTrue(systemStatus(payload(direction, quoted(edge, 306, 5000))) is ValidationStatus.Valid)
        }
    }

    @Test
    fun `missing required runtime minimum blocks sell and buy rather than passing`() = runBlocking {
        for (direction in SwapDirection.entries) {
            val edge = edge("a", "b")
            val cause = NoSuchElementException("Required runtime minimum is missing")
            when (direction) {
                SwapDirection.SPECIFIED_IN -> `when`(edge.minAllowedAmountIn()).thenThrow(cause)
                SwapDirection.SPECIFIED_OUT -> `when`(edge.minAllowedAmountOut()).thenThrow(cause)
            }

            val status = systemStatus(payload(direction, quoted(edge, 5000, 5000))) as ValidationStatus.NotValid
            assertEquals(DefaultFailureLevel.ERROR, status.level)
            assertSame(cause, (status.reason as SwapValidationFailure.PoolTradeLimitsUnavailable).cause)
        }
    }

    @Test
    fun `missing intermediate minimum blocks both route directions`() = runBlocking {
        for (direction in SwapDirection.entries) {
            val first = edge("a", "b")
            val last = edge("b", "c")
            val cause = NoSuchElementException("Intermediate pool minimum unavailable")
            when (direction) {
                SwapDirection.SPECIFIED_IN -> `when`(last.minAllowedAmountIn()).thenThrow(cause)
                SwapDirection.SPECIFIED_OUT -> `when`(first.minAllowedAmountOut()).thenThrow(cause)
            }

            val status = systemStatus(payload(direction, quoted(first, 5000, 5000), quoted(last, 5000, 5000))) as ValidationStatus.NotValid
            assertEquals(DefaultFailureLevel.ERROR, status.level)
            assertSame(cause, (status.reason as SwapValidationFailure.PoolTradeLimitsUnavailable).cause)
        }
    }

    @Test
    fun `unavailable intermediate maximum still blocks after an unrestricted first hop`() = runBlocking {
        val first = edge("a", "b")
        val intermediate = edge("b", "c", minIn = 1000)
        val cause = IllegalStateException("Pool state unavailable")
        `when`(intermediate.maxAllowedAmountIn()).thenThrow(cause)

        val status = systemStatus(
            payload(SwapDirection.SPECIFIED_IN, quoted(first, 5000, 5000), quoted(intermediate, 5000, 5000))
        ) as ValidationStatus.NotValid
        assertEquals(DefaultFailureLevel.ERROR, status.level)
        assertSame(cause, (status.reason as SwapValidationFailure.PoolTradeLimitsUnavailable).cause)
    }

    @Test
    fun `cancellation is propagated rather than converted to an unavailable limit`() = runBlocking {
        val edge = edge("a", "b")
        val cancellation = CancellationException("Leaving screen")
        `when`(edge.minAllowedAmountIn()).thenThrow(cancellation)

        try {
            validation.validate(payload(SwapDirection.SPECIFIED_IN, quoted(edge, 306, 5000)))
            throw AssertionError("Expected cancellation")
        } catch (actual: CancellationException) {
            assertSame(cancellation, actual)
        }
    }

    @Test
    fun `maximum boundary and user adjustable hop semantics are preserved in both directions`() = runBlocking {
        for (direction in SwapDirection.entries) {
            val edge = edge("a", "b", minIn = 1000, minOut = 1000)
            `when`(edge.maxAllowedAmountIn()).thenReturn(2000.toBigInteger())
            `when`(edge.maxAllowedAmountOut()).thenReturn(2000.toBigInteger())
            bindAsset(if (direction == SwapDirection.SPECIFIED_IN) edge.from else edge.to)
            assertTrue(systemStatus(payload(direction, quoted(edge, 2000, 5000))) is ValidationStatus.Valid)

            val status = systemStatus(payload(direction, quoted(edge, 2001, 5000))) as ValidationStatus.NotValid
            val reason = status.reason as SwapValidationFailure.AmountExceedsPoolTradeLimit
            assertEquals(2000.toBigInteger(), reason.maxAmount)
            assertSame(hopAsset, reason.limitedAsset)
            assertEquals(direction, reason.direction)
            assertTrue(reason.isUserInputAdjustable)
        }
    }

    @Test
    fun `intermediate maximum failures cannot directly adjust user amounts`() = runBlocking {
        for (direction in SwapDirection.entries) {
            val first = edge("a", "b")
            val last = edge("b", "c")
            when (direction) {
                SwapDirection.SPECIFIED_IN -> {
                    `when`(last.maxAllowedAmountIn()).thenReturn(2000.toBigInteger())
                    bindAsset(last.from)
                }
                SwapDirection.SPECIFIED_OUT -> {
                    `when`(first.maxAllowedAmountOut()).thenReturn(2000.toBigInteger())
                    bindAsset(first.to)
                }
            }

            val status = systemStatus(payload(direction, quoted(first, 2001, 5000), quoted(last, 2001, 5000))) as ValidationStatus.NotValid
            val reason = status.reason as SwapValidationFailure.AmountExceedsPoolTradeLimit
            assertEquals(2000.toBigInteger(), reason.maxAmount)
            assertSame(hopAsset, reason.limitedAsset)
            assertEquals(false, reason.isUserInputAdjustable)
        }
    }

    @Test
    fun `cross chain buy followed by Hydration blocks sell input 306 despite buy output 5000`() = runBlocking {
        val xcm = edge("a", "b", fromChain = "origin")
        val hydration = edge("b", "c", minIn = 1000, minOut = 1000)
        bindAsset(hydration.from)
        val payload = groupedPayload(
            SwapDirection.SPECIFIED_OUT,
            listOf(listOf(quoted(xcm, 306, 306)), listOf(quoted(hydration, 5000, 306)))
        )

        assertMinimumFailure(systemStatus(payload))
    }

    @Test
    fun `cross chain buy followed by Hydration accepts sell input 5000 despite buy output 306`() = runBlocking {
        val xcm = edge("a", "b", fromChain = "origin")
        val hydration = edge("b", "c", minIn = 1000, minOut = 1000)
        val payload = groupedPayload(
            SwapDirection.SPECIFIED_OUT,
            listOf(listOf(quoted(xcm, 5000, 5000)), listOf(quoted(hydration, 306, 5000)))
        )

        assertTrue(systemStatus(payload) is ValidationStatus.Valid)
    }

    @Test
    fun `LP deposit after first operation checks sell input even though buying shares has no minimum`() = runBlocking {
        val xcm = edge("a", "b", fromChain = "origin")
        val lpDeposit = edge("b", "c", minIn = 1000, minOut = null)
        bindAsset(lpDeposit.from)
        val payload = groupedPayload(
            SwapDirection.SPECIFIED_OUT,
            listOf(listOf(quoted(xcm, 306, 306)), listOf(quoted(lpDeposit, 5000, 306)))
        )

        assertMinimumFailure(systemStatus(payload))
    }

    @Test
    fun `multiple hops within first buy operation retain output checks`() = runBlocking {
        val first = edge("a", "b", minIn = 1000, minOut = 1000)
        val last = edge("b", "c", minIn = 1000, minOut = 1000)
        val after = edge("c", "d", minIn = 1000, minOut = 1000)
        val payload = groupedPayload(
            SwapDirection.SPECIFIED_OUT,
            listOf(listOf(quoted(first, 306, 5000), quoted(last, 5000, 306)), listOf(quoted(after, 5000, 5000)))
        )
        bindAsset(first.to)
        assertMinimumFailure(systemStatus(payload))

        val validPayload = groupedPayload(
            SwapDirection.SPECIFIED_OUT,
            listOf(listOf(quoted(first, 5000, 5000), quoted(last, 5000, 5000)), listOf(quoted(after, 5000, 5000)))
        )
        assertTrue(systemStatus(validPayload) is ValidationStatus.Valid)
    }

    @Test
    fun `second hop of first buy operation is not converted to sell`() = runBlocking {
        val first = edge("a", "b")
        val last = edge("b", "c", minIn = 1000, minOut = 1000)
        val after = edge("c", "d")
        val payload = groupedPayload(
            SwapDirection.SPECIFIED_OUT,
            listOf(listOf(quoted(first, 306, 5000), quoted(last, 5000, 306)), listOf(quoted(after, 5000, 5000)))
        )

        assertTrue(systemStatus(payload) is ValidationStatus.Valid)
    }

    @Test
    fun `first buy checks gross pool output rather than output net of its own commission`() = runBlocking {
        val hydration = edge("a", "b", minOut = 1000)
        val payload = groupedPayload(
            SwapDirection.SPECIFIED_OUT,
            listOf(listOf(quoted(hydration, 1000, 5000))),
            netOutputs = listOf(999)
        )

        assertTrue(systemStatus(payload) is ValidationStatus.Valid)
    }

    @Test
    fun `downstream buy quote uses sell maximum and cannot adjust user output`() = runBlocking {
        val xcm = edge("a", "b", fromChain = "origin")
        val hydration = edge("b", "c")
        `when`(hydration.maxAllowedAmountIn()).thenReturn(1000.toBigInteger())
        `when`(hydration.maxAllowedAmountOut()).thenReturn(200.toBigInteger())
        bindAsset(hydration.from)
        val valid = groupedPayload(
            SwapDirection.SPECIFIED_OUT,
            listOf(listOf(quoted(xcm, 306, 306)), listOf(quoted(hydration, 5000, 306)))
        )
        assertTrue(systemStatus(valid) is ValidationStatus.Valid)

        val invalid = groupedPayload(
            SwapDirection.SPECIFIED_OUT,
            listOf(listOf(quoted(xcm, 5000, 5000)), listOf(quoted(hydration, 306, 5000)))
        )
        val status = systemStatus(invalid) as ValidationStatus.NotValid
        val reason = status.reason as SwapValidationFailure.AmountExceedsPoolTradeLimit
        assertEquals(SwapDirection.SPECIFIED_IN, reason.direction)
        assertEquals(false, reason.isUserInputAdjustable)
        assertSame(hopAsset, reason.limitedAsset)
    }

    @Test
    fun `missing fee operations fail closed instead of using global quote direction`() = runBlocking {
        val hydration = edge("a", "b", minIn = 1000)
        val payload = payload(SwapDirection.SPECIFIED_OUT, quoted(hydration, 5000, 306))
        `when`(payload.fee.segments).thenReturn(emptyList())

        assertTrue((systemStatus(payload) as ValidationStatus.NotValid).reason is SwapValidationFailure.PoolTradeLimitsUnavailable)
    }

    @Test
    fun `unmatched operation boundary fails closed`() = runBlocking {
        val hydration = edge("a", "b")
        val payload = payload(SwapDirection.SPECIFIED_OUT, quoted(hydration, 5000, 306))
        `when`(payload.fee.segments.single().operation.assetOut).thenReturn(FullChainAssetId("hydration", 999))

        assertTrue((systemStatus(payload) as ValidationStatus.NotValid).reason is SwapValidationFailure.PoolTradeLimitsUnavailable)
    }

    @Test
    fun `operations must cover the entire quoted path`() = runBlocking {
        val first = edge("a", "b")
        val last = edge("b", "c")
        val payload = groupedPayload(
            SwapDirection.SPECIFIED_OUT,
            listOf(listOf(quoted(first, 5000, 5000)), listOf(quoted(last, 5000, 5000)))
        )
        val incompleteSegments = payload.fee.segments.take(1)
        `when`(payload.fee.segments).thenReturn(incompleteSegments)

        assertTrue((systemStatus(payload) as ValidationStatus.NotValid).reason is SwapValidationFailure.PoolTradeLimitsUnavailable)
    }

    private suspend fun assertMinimumBoundaries(direction: SwapDirection) {
        val edge = edge("a", "b", minIn = 1000, minOut = 1000)
        bindAsset(if (direction == SwapDirection.SPECIFIED_IN) edge.from else edge.to)

        for (amount in listOf(0, 306, 999)) {
            assertMinimumFailure(systemStatus(payload(direction, quoted(edge, amount, 5000))))
        }
        for (amount in listOf(1000, 1001, 5000)) {
            assertTrue(systemStatus(payload(direction, quoted(edge, amount, 5000))) is ValidationStatus.Valid)
        }
    }

    private fun assertMinimumFailure(status: ValidationStatus<SwapValidationFailure>) {
        val invalid = status as ValidationStatus.NotValid
        assertEquals(DefaultFailureLevel.ERROR, invalid.level)
        val reason = invalid.reason as SwapValidationFailure.AmountBelowPoolTradeMinimum
        assertEquals(1000.toBigInteger(), reason.minAmount)
        assertSame(hopAsset, reason.limitedAsset)
    }

    private suspend fun systemStatus(payload: SwapValidationPayload): ValidationStatus<SwapValidationFailure> {
        val system = ValidationSystem<SwapValidationPayload, SwapValidationFailure> {
            poolTradeLimits(assetsContext)
        }
        return system.validate(payload).getOrThrow()
    }

    private suspend fun bindAsset(id: FullChainAssetId) {
        `when`(token.configuration).thenReturn(hopAsset)
        `when`(asset.token).thenReturn(token)
        `when`(assetsContext.getAsset(id)).thenReturn(asset)
    }

    private suspend fun edge(
        from: String,
        to: String,
        minIn: Int? = null,
        minOut: Int? = null,
        fromChain: String = "hydration"
    ): LimitedSwapEdge {
        val edge = mock(LimitedSwapEdge::class.java)
        `when`(edge.from).thenReturn(FullChainAssetId(fromChain, from.first().code))
        `when`(edge.to).thenReturn(FullChainAssetId("hydration", to.first().code))
        `when`(edge.minAllowedAmountIn()).thenReturn(minIn?.toBigInteger())
        `when`(edge.minAllowedAmountOut()).thenReturn(minOut?.toBigInteger())
        return edge
    }

    private fun quoted(edge: SwapGraphEdge, amount: Int, counterpart: Int): QuotedEdge<SwapGraphEdge> {
        return QuotedEdge(amount.toBigInteger(), counterpart.toBigInteger(), edge)
    }

    private fun payload(direction: SwapDirection, vararg hops: QuotedEdge<SwapGraphEdge>): SwapValidationPayload {
        return groupedPayload(direction, listOf(hops.toList()))
    }

    private fun groupedPayload(
        direction: SwapDirection,
        groups: List<List<QuotedEdge<SwapGraphEdge>>>,
        netOutputs: List<Int>? = null
    ): SwapValidationPayload {
        val path = QuotedPath(direction, groups.flatten(), PathRoughFeeEstimation.zero())
        val segments = groups.mapIndexed { index, hops ->
            val grossInput = if (direction == SwapDirection.SPECIFIED_IN) hops.first().quotedAmount else hops.first().quote
            val grossOutput = if (direction == SwapDirection.SPECIFIED_IN) hops.last().quote else hops.last().quotedAmount
            val limit = when (direction) {
                SwapDirection.SPECIFIED_IN -> SwapLimit.SpecifiedIn(grossInput, grossOutput, grossOutput)
                SwapDirection.SPECIFIED_OUT -> SwapLimit.SpecifiedOut(grossOutput, grossInput, grossInput)
            }
            val operation = mock(AtomicSwapOperation::class.java)
            val operationFee = mock(AtomicSwapOperationFee::class.java)
            val operationAssetIn = hops.first().edge.from
            val operationAssetOut = hops.last().edge.to
            `when`(operation.assetIn).thenReturn(operationAssetIn)
            `when`(operation.assetOut).thenReturn(operationAssetOut)
            `when`(operation.estimatedSwapLimit).thenReturn(limit)
            val netFlow = SwapFee.SwapSegment.SegmentNetFlow(
                amountIn = grossInput,
                amountOut = netOutputs?.get(index)?.toBigInteger() ?: grossOutput,
                amountOutMin = netOutputs?.get(index)?.toBigInteger() ?: grossOutput
            )
            SwapFee.SwapSegment(operationFee, operation, netFlow)
        }
        val fee = mock(SwapFee::class.java)
        val quote = mock(SwapQuote::class.java)
        val payload = mock(SwapValidationPayload::class.java)
        `when`(fee.segments).thenReturn(segments)
        `when`(quote.quotedPath).thenReturn(path)
        `when`(payload.swapQuote).thenReturn(quote)
        `when`(payload.fee).thenReturn(fee)
        return payload
    }

    private interface LimitedSwapEdge : SwapGraphEdge, TradeAmountLimitedEdge
}
