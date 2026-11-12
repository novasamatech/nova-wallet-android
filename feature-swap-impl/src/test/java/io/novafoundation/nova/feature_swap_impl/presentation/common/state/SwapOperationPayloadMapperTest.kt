package io.novafoundation.nova.feature_swap_impl.presentation.common.state

import io.novafoundation.nova.common.utils.Fraction.Companion.fractions
import io.novafoundation.nova.feature_account_api.data.extrinsic.SubmissionOrigin
import io.novafoundation.nova.feature_account_api.data.model.SubstrateFee
import io.novafoundation.nova.feature_account_api.data.model.SubstrateFeeBase
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicSwapOperation
import io.novafoundation.nova.feature_swap_api.domain.model.SwapGraphEdge
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFeeArgs
import io.novafoundation.nova.feature_swap_api.domain.model.routeId
import io.novafoundation.nova.feature_swap_api.domain.model.SubmissionFeeWithLabel
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicOperationFeeDisplayData
import io.novafoundation.nova.feature_swap_api.domain.model.network
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuote
import io.novafoundation.nova.feature_swap_api.domain.model.SwapExecutionEstimate
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFee
import io.novafoundation.nova.feature_swap_api.domain.model.SwapMaxAdditionalAmountDeduction
import io.novafoundation.nova.feature_swap_api.domain.model.fee.AtomicSwapOperationFee
import io.novafoundation.nova.feature_swap_api.domain.swap.SwapService
import io.novafoundation.nova.feature_swap_core_api.data.paths.model.QuotedPath
import io.novafoundation.nova.feature_swap_core_api.data.paths.model.QuotedEdge
import io.novafoundation.nova.feature_swap_core_api.data.paths.model.PathRoughFeeEstimation
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.SwapDirection
import io.novafoundation.nova.feature_wallet_api.domain.model.withAmount
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import kotlin.time.Duration.Companion.seconds

class SwapOperationPayloadMapperTest {
    private val registry = mock(ChainRegistry::class.java)
    private val service = mock(SwapService::class.java)
    private val input = asset(0)
    private val output = asset(2)
    private val edge = mock(SwapGraphEdge::class.java)
    private val operation = mock(AtomicSwapOperation::class.java)
    private val unusedArgs = mock(SwapFeeArgs::class.java)
    private val mapper = SwapOperationPayloadMapper(registry, service)
    private var operationArgs: SwapFeeArgs? = null

    @Test
    fun `recreated screen preserves reviewed net amounts fees slippage and wallet without requoting`() = runBlocking {
        prepare()
        val state = state()
        val payload = mapper.toPayload(state, metaAccountId = 42, returnToTab = true)
        `when`(service.resolveRoute(listOf(edge.routeId()), this)).thenReturn(listOf(edge))
        `when`(service.constructOperations(any() ?: unusedArgs)).thenAnswer {
            operationArgs = it.getArgument(0)
            listOf(operation)
        }
        val restored = SwapOperationPayloadMapper(registry, service).fromPayload(payload, this)
        assertEquals(state.quote.planksIn, restored.quote.planksIn)
        assertEquals(state.quote.planksOut, restored.quote.planksOut)
        assertEquals(state.slippage, restored.slippage)
        assertEquals(BigInteger.valueOf(17), restored.fee.segments.single().fee.serviceCommission!!.amount)
        assertEquals(BigInteger.valueOf(1950), restored.fee.segments.single().netFlow.amountOutMin)
        assertEquals(BigInteger.valueOf(1983), restored.fee.segments.single().netFlow.amountOut)
        assertEquals(42L, operationArgs!!.metaAccountId)
        assertEquals(BigInteger.valueOf(2000), operationArgs!!.executionPath.single().quotedSwapEdge.quote)
        assertEquals(BigInteger.valueOf(20), restored.quote.quotedPath.roughFeeEstimation.inAssetOut)
        assertEquals(BigInteger.valueOf(10), restored.quote.quotedPath.roughFeeEstimation.inAssetIn)
        assertEquals(state.fee.initialSubmissionFee.submissionOrigin, restored.fee.initialSubmissionFee.submissionOrigin)
        assertEquals(state.fee.totalFeeAmount(input), restored.fee.totalFeeAmount(input))
        verify(service).resolveRoute(listOf(edge.routeId()), this)
        verify(service).constructOperations(operationArgs!!)
        verifyNoMoreInteractions(service)
    }

    @Test
    fun `payload account identities are isolated from the form and restored operation`() = runBlocking {
        prepare()
        val state = state()
        val payload = mapper.toPayload(state, metaAccountId = 42, returnToTab = true)
        state.fee.initialSubmissionFee.submissionOrigin.executingAccount[0] = 9
        state.fee.initialSubmissionFee.submissionOrigin.signingAccount[0] = 9
        val savedFee = payload.fee.segments.single().submissionFee
        assertArrayEquals(byteArrayOf(1), savedFee.executingAccount)
        assertArrayEquals(byteArrayOf(2), savedFee.signingAccount)

        `when`(service.resolveRoute(listOf(edge.routeId()), this)).thenReturn(listOf(edge))
        `when`(service.constructOperations(any() ?: unusedArgs)).thenReturn(listOf(operation))
        val restored = mapper.fromPayload(payload, this)
        restored.fee.initialSubmissionFee.submissionOrigin.executingAccount[0] = 8
        restored.fee.initialSubmissionFee.submissionOrigin.signingAccount[0] = 8
        assertArrayEquals(byteArrayOf(1), savedFee.executingAccount)
        assertArrayEquals(byteArrayOf(2), savedFee.signingAccount)
    }

    @Test
    fun `changed operation boundaries cannot silently replace reviewed route`() = runBlocking {
        prepare()
        val payload = mapper.toPayload(state(), 42, true)
        `when`(service.resolveRoute(listOf(edge.routeId()), this)).thenReturn(listOf(edge))
        `when`(service.constructOperations(any() ?: unusedArgs)).thenReturn(emptyList())
        assertTrue(runCatching { mapper.fromPayload(payload, this) }.exceptionOrNull() is IllegalArgumentException)
    }

    private fun prepare() {
        val chain = mock(Chain::class.java)
        `when`(chain.assetsById).thenReturn(mapOf(0 to input, 2 to output))
        `when`(registry.chainsById).thenReturn(MutableStateFlow(mapOf("chain" to chain)))
        val inputId = input.fullId
        val outputId = output.fullId
        `when`(edge.from).thenReturn(inputId)
        `when`(edge.to).thenReturn(outputId)
        `when`(edge.exchangeId).thenReturn("asset-conversion")
        `when`(operation.assetIn).thenReturn(inputId)
        `when`(operation.assetOut).thenReturn(outputId)
    }

    private fun state(): SwapState {
        val submission = SubmissionFeeWithLabel(SubstrateFee(BigInteger.valueOf(3), SubmissionOrigin(byteArrayOf(1), byteArrayOf(2)), input))
        val fee = object : AtomicSwapOperationFee {
            override val submissionFee = submission
            override val serviceCommission = SubstrateFeeBase(BigInteger.valueOf(17), output)
            override val postSubmissionFees = AtomicSwapOperationFee.PostSubmissionFees()
            override fun constructDisplayData() = AtomicOperationFeeDisplayData(
                listOf(AtomicOperationFeeDisplayData.SwapFeeComponentDisplay.network(submission))
            )
        }
        val quote = SwapQuote(
            input.withAmount(BigInteger.valueOf(1000)),
            output.withAmount(BigInteger.valueOf(1983)),
            0.01.fractions,
            QuotedPath(
                SwapDirection.SPECIFIED_IN,
                listOf(QuotedEdge(BigInteger.valueOf(1000), BigInteger.valueOf(2000), edge)),
                PathRoughFeeEstimation(BigInteger.valueOf(20), BigInteger.valueOf(10))
            ),
            SwapExecutionEstimate(listOf(12.seconds), 3.seconds),
            SwapDirection.SPECIFIED_IN
        )
        return SwapState(
            quote,
            SwapFee(
                listOf(
                    SwapFee.SwapSegment(
                        fee,
                        operation,
                        SwapFee.SwapSegment.SegmentNetFlow(BigInteger.valueOf(1000), BigInteger.valueOf(1983), BigInteger.valueOf(1950))
                    )
                ),
                SubstrateFeeBase(BigInteger.ZERO, input),
                SwapMaxAdditionalAmountDeduction(BigInteger.valueOf(100))
            ),
            0.005.fractions
        )
    }

    private fun asset(id: Int) = mock(Chain.Asset::class.java).also {
        `when`(it.chainId).thenReturn("chain")
        `when`(it.id).thenReturn(id)
    }
}
