package io.novafoundation.nova.feature_swap_impl.domain

import io.novafoundation.nova.feature_swap_api.domain.model.OnChainSwapEdge
import io.novafoundation.nova.feature_swap_api.domain.model.SwapGraphEdge
import io.novafoundation.nova.feature_swap_impl.domain.swap.outputCommission
import io.novafoundation.nova.feature_swap_impl.domain.swap.operationPrototypes
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.QuotedAmounts
import io.novafoundation.nova.common.utils.asPrecision
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicSwapOperationPrototype
import io.novafoundation.nova.feature_swap_core_api.data.paths.PathFeeEstimator
import io.novafoundation.nova.feature_swap_core_api.data.paths.model.PathRoughFeeEstimation
import io.novafoundation.nova.feature_swap_core_api.data.paths.model.QuotedEdge
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.exchangerA.ExchangerAAssetExchange
import io.novafoundation.nova.feature_swap_impl.data.exchanger.ExchangerAQuoteRepository
import io.novafoundation.nova.feature_swap_impl.domain.exchanger.ExchangerAAmountConstraints
import io.novafoundation.nova.feature_swap_impl.domain.exchanger.ExchangerAPayLimits
import io.novafoundation.nova.feature_swap_impl.domain.exchanger.ExchangerAQuoteArgs
import io.novafoundation.nova.feature_swap_impl.domain.exchanger.ExchangerAQuoteResult
import io.novafoundation.nova.feature_swap_impl.domain.exchanger.catalogue.ExchangerASwapPair
import io.novafoundation.nova.feature_swap_impl.domain.exchanger.catalogue.ExchangerATaoCatalogue
import io.novafoundation.nova.feature_wallet_api.domain.model.ChainAssetWithAmount
import io.novafoundation.nova.test_shared.any
import io.novafoundation.nova.feature_swap_impl.domain.swap.QuoteProviderException.DepositLimit
import android.util.Log
import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.utils.graph.Graph
import io.novafoundation.nova.common.utils.graph.create
import io.novafoundation.nova.common.utils.graph.Path
import io.novafoundation.nova.common.utils.graph.WeightedEdge
import io.novafoundation.nova.common.utils.graph.EdgeVisitFilter
import io.novafoundation.nova.feature_swap_core.domain.paths.RealPathQuoterFactory
import io.novafoundation.nova.feature_swap_core_api.data.primitive.errors.SwapQuoteException
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.QuotableEdge
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.SwapDirection
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import java.io.IOException
import java.math.BigInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mockStatic

class SwapQuoteErrorsTest {
    private val input = asset(0)
    private val output = asset(2)
    private val cache = TestCache()
    private val factory = RealPathQuoterFactory(cache)

    @Test
    fun `network failure is preserved instead of reported as insufficient liquidity`() = runBlocking<Unit> {
        mockStatic(Log::class.java).use {
            val failure = IOException("offline")
            val quoter = factory.create(flowOf(Graph.create(listOf(edge { throw failure }))), this, null, null)
            assertSame(failure, runCatching { quoter.findBestPath(input, output, BigInteger.TEN, SwapDirection.SPECIFIED_IN) }.exceptionOrNull())
        }
    }

    @Test
    fun `a successful alternative route survives another pool failing`() = runBlocking<Unit> {
        mockStatic(Log::class.java).use {
            val quoter = factory.create(flowOf(Graph.create(listOf(edge { throw IOException() }, edge { BigInteger.valueOf(25) }))), this, null, null)
            assertEquals(1, quoter.findBestPath(input, output, BigInteger.TEN, SwapDirection.SPECIFIED_IN).candidates.size)
        }
    }

    @Test
    fun `no route is distinct from a route without liquidity`() = runBlocking<Unit> {
        mockStatic(Log::class.java).use {
            val noRoute = factory.create(flowOf(Graph.create(emptyList<QuotableEdge>())), this, null, null)
            assertSame(
                SwapQuoteException.NoRoute,
                runCatching { noRoute.findBestPath(input, output, BigInteger.TEN, SwapDirection.SPECIFIED_IN) }.exceptionOrNull()
            )
            val noLiquidity = factory.create(flowOf(Graph.create(listOf(edge { throw SwapQuoteException.NotEnoughLiquidity }))), this, null, null)
            assertSame(
                SwapQuoteException.NotEnoughLiquidity,
                runCatching { noLiquidity.findBestPath(input, output, BigInteger.TEN, SwapDirection.SPECIFIED_IN) }.exceptionOrNull()
            )
        }
    }

    @Test
    fun `account-specific visit filters never reuse another quoters paths`() = runBlocking<Unit> {
        mockStatic(Log::class.java).use {
            val graph = flowOf(Graph.create(listOf(edge { BigInteger.TEN })))
            factory.create(graph, this, null, null).findBestPath(input, output, BigInteger.ONE, SwapDirection.SPECIFIED_IN)
            val blocked = factory.create(graph, this, null, EdgeVisitFilter { _, _ -> false })
            assertSame(
                SwapQuoteException.NoRoute,
                runCatching { blocked.findBestPath(input, output, BigInteger.ONE, SwapDirection.SPECIFIED_IN) }.exceptionOrNull()
            )
        }
    }

    @Test
    fun `deposit limits on a receive quote are reported in the deposit asset`() = runBlocking<Unit> {
        val catalogue = mock(ExchangerATaoCatalogue::class.java)
        val quotes = mock(ExchangerAQuoteRepository::class.java)
        `when`(catalogue.availablePairs()).thenReturn(listOf(ExchangerASwapPair(input, output)))
        val edge = ExchangerAAssetExchange(catalogue, quotes, ExchangerAAmountConstraints()).availableDirectSwapConnections().single()

        suspend fun quoteWithLimit(reason: ExchangerAQuoteResult.OutOfRange.Reason, maximum: BigInteger?): Throwable? {
            val args = ExchangerAQuoteArgs(input, output, BigInteger.TEN, SwapDirection.SPECIFIED_OUT)
            val result = ExchangerAQuoteResult.OutOfRange(args, ExchangerAPayLimits(BigInteger.ONE, maximum), reason)
            `when`(quotes.quote(args)).thenReturn(result)
            return runCatching { edge.quote(args.amount, args.swapDirection) }.exceptionOrNull()
        }

        val minimum = quoteWithLimit(ExchangerAQuoteResult.OutOfRange.Reason.BELOW_MINIMUM, null) as DepositLimit.BelowMinimum
        assertSame(input, minimum.asset)
        assertEquals(BigInteger.ONE, minimum.minimum)

        for (maximum in listOf(BigInteger.TEN, null)) {
            val limit = quoteWithLimit(ExchangerAQuoteResult.OutOfRange.Reason.ABOVE_MAXIMUM, maximum) as DepositLimit.AboveMaximum
            assertSame(input, limit.asset)
            assertEquals(maximum, limit.maximum)
        }
    }

    @Test
    fun `terminal exchange quotes the intermediate deposit in both directions`() = runBlocking<Unit> {
        val bridge = asset(1)
        `when`(bridge.precision).thenReturn(10.asPrecision())
        val catalogue = mock(ExchangerATaoCatalogue::class.java)
        val quotes = mock(ExchangerAQuoteRepository::class.java)
        val constraints = ExchangerAAmountConstraints()
        val exchange = ExchangerAAssetExchange(catalogue, quotes, constraints)
        `when`(catalogue.availablePairs()).thenReturn(listOf(ExchangerASwapPair(bridge, output)))
        val terminal = exchange.availableDirectSwapConnections().single()
        val prefix = object : QuotableEdge {
            override val from = input.fullId
            override val to = bridge.fullId
            override fun weightForAppendingTo(path: Path<WeightedEdge<FullChainAssetId>>) = 100
            override suspend fun quote(amount: BigInteger, direction: SwapDirection) =
                if (direction == SwapDirection.SPECIFIED_IN) amount * 2.toBigInteger() else amount / 2.toBigInteger()
        }
        val charger = mock(AtomicSwapOperationPrototype::class.java)
        `when`(charger.serviceCommissionIncludedIn(1080.toBigInteger())).thenReturn(50.toBigInteger())
        `when`(charger.serviceCommissionToAddOnTop(1000.toBigInteger())).thenReturn(50.toBigInteger())
        val fees = object : PathFeeEstimator<QuotableEdge> {
            override suspend fun roughlyEstimateFee(path: Path<QuotedEdge<QuotableEdge>>) = PathRoughFeeEstimation.zero()
            override suspend fun outputCommission(path: Path<QuotableEdge>) =
                path.indexOf(prefix).takeIf { it >= 0 }?.let { IndexedValue(it, charger) }
        }
        val requests = mutableListOf<ExchangerAQuoteArgs>()
        `when`(quotes.quote(any())).thenAnswer { call ->
            val args = call.getArgument<ExchangerAQuoteArgs>(0)
            requests += args
            ExchangerAQuoteResult.Quote(
                args, ExchangerAPayLimits(BigInteger.ONE, null),
                ChainAssetWithAmount(bridge, if (args.swapDirection == SwapDirection.SPECIFIED_IN) args.amount else 1000.toBigInteger()),
                ChainAssetWithAmount(output, 21.toBigInteger()),
                null, null, null, null
            )
        }
        mockStatic(Log::class.java).use {
            val graph = Graph.create(listOf(prefix, terminal))
            val quoter = factory.create(flowOf(graph), this, fees, null)
            val sell = quoter.findBestPath(input, output, 540.toBigInteger(), SwapDirection.SPECIFIED_IN).bestPath
            assertEquals(1000.toBigInteger(), sell.path.last().quotedAmount)
            assertEquals(21.toBigInteger(), sell.path.last().quote)
            val buy = quoter.findBestPath(input, output, 20.toBigInteger(), SwapDirection.SPECIFIED_OUT).bestPath
            assertEquals(21.toBigInteger(), buy.path.last().quotedAmount)
            assertEquals(525.toBigInteger(), buy.path.first().quote)
            quoter.findBestPath(bridge, output, 1030.toBigInteger(), SwapDirection.SPECIFIED_IN)
            assertEquals(listOf(1000, 20, 1030).map { it.toBigInteger() }, requests.map { it.amount })
        }
    }

    @Test
    fun `commission passes through a regular next edge in its own units once for the entire grouped operation`() = runBlocking<Unit> {
        val first = asset(3)
        val second = asset(4)
        val charger = mock(AtomicSwapOperationPrototype::class.java)
        val transfer = mock(AtomicSwapOperationPrototype::class.java)
        `when`(charger.chargesServiceFee).thenReturn(true)
        `when`(charger.serviceCommissionIncludedIn(any())).thenReturn(10.toBigInteger())
        `when`(charger.serviceCommissionToAddOnTop(any())).thenReturn(10.toBigInteger())

        val hopA = onChainEdge(input, first, charger, rate = 1)
        val hopB = onChainEdge(first, second, charger, rate = 1, appendTo = charger)
        val tail = onChainEdge(second, output, transfer, rate = 3)
        val path: Path<SwapGraphEdge> = listOf(hopA, hopB, tail)
        val fees = object : PathFeeEstimator<SwapGraphEdge> {
            override suspend fun outputCommission(path: Path<SwapGraphEdge>) = path.outputCommission()
            override suspend fun roughlyEstimateFee(path: Path<QuotedEdge<SwapGraphEdge>>) = PathRoughFeeEstimation.zero()
        }
        assertEquals(listOf(1, 2), path.operationPrototypes().map { it.index })
        assertEquals(1, path.outputCommission()?.index)

        mockStatic(Log::class.java).use {
            val quoter = factory.create(flowOf(Graph.create(path)), this, fees, null)
            val sell = quoter.findBestPath(input, output, 100.toBigInteger(), SwapDirection.SPECIFIED_IN).bestPath
            assertEquals(listOf(100, 100, 90).map { it.toBigInteger() }, sell.path.map { it.quotedAmount })
            assertEquals(270.toBigInteger(), sell.netAmountOut)

            val buy = quoter.findBestPath(input, output, 270.toBigInteger(), SwapDirection.SPECIFIED_OUT).bestPath
            assertEquals(listOf(100, 100, 270).map { it.toBigInteger() }, buy.path.map { it.quotedAmount })
            assertEquals(100.toBigInteger(), buy.path.first().quote)
            assertEquals(270.toBigInteger(), buy.netAmountOut)

            val directSell = quoter.findBestPath(input, first, 100.toBigInteger(), SwapDirection.SPECIFIED_IN).bestPath
            assertEquals(100.toBigInteger(), directSell.path.single().quote)
            assertEquals(90.toBigInteger(), directSell.netAmountOut)
            val directBuy = quoter.findBestPath(input, first, 90.toBigInteger(), SwapDirection.SPECIFIED_OUT).bestPath
            assertEquals(100.toBigInteger(), directBuy.path.single().quote)
            assertEquals(90.toBigInteger(), directBuy.netAmountOut)
        }

        // A future external operation in the middle must also break the on-chain batch.
        val external = mock(SwapGraphEdge::class.java)
        val split = listOf(hopA, external, hopB)
        assertEquals(listOf(0, 2), split.operationPrototypes().map { it.index })
        assertEquals(2, split.outputCommission()?.index)
    }

    private fun onChainEdge(from: Chain.Asset, to: Chain.Asset, operation: AtomicSwapOperationPrototype, rate: Int, appendTo: AtomicSwapOperationPrototype? = null): OnChainSwapEdge {
        val delegate = mock(OnChainSwapEdge::class.java)
        return object : OnChainSwapEdge by delegate {
            override val from = from.fullId
            override val to = to.fullId
            override suspend fun beginOperationPrototype() = operation
            override suspend fun appendToOperationPrototype(currentTransaction: AtomicSwapOperationPrototype) = operation.takeIf { currentTransaction === appendTo }
            override suspend fun quoteAmounts(amount: BigInteger, direction: SwapDirection, isIntermediate: Boolean): QuotedAmounts {
                val other = if (direction == SwapDirection.SPECIFIED_IN) amount * rate.toBigInteger() else amount / rate.toBigInteger()
                return QuotedAmounts(amount, other)
            }
        }
    }

    private fun edge(quote: suspend () -> BigInteger) = object : QuotableEdge {
        override val from = input.fullId
        override val to = output.fullId
        override fun weightForAppendingTo(path: Path<WeightedEdge<FullChainAssetId>>) = 100
        override suspend fun quote(amount: BigInteger, direction: SwapDirection) = quote()
    }

    private fun asset(id: Int) = mock(Chain.Asset::class.java).also {
        `when`(it.chainId).thenReturn("chain")
        `when`(it.id).thenReturn(id)
    }

    private class TestCache : ComputationalCache {
        private val values = mutableMapOf<String, Any?>()

        @Suppress("UNCHECKED_CAST")
        override suspend fun <T> useCache(key: String, scope: CoroutineScope, computation: suspend CoroutineScope.() -> T): T {
            if (!values.containsKey(key)) values[key] = computation(scope)
            return values[key] as T
        }
        override fun <T> useSharedFlow(key: String, scope: CoroutineScope, flowLazy: suspend CoroutineScope.() -> Flow<T>): Flow<T> = error("Not used")
    }
}
