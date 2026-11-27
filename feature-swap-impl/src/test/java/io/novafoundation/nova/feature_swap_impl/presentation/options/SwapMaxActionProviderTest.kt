package io.novafoundation.nova.feature_swap_impl.presentation.options

import io.novafoundation.nova.common.utils.Precision
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicSwapOperation
import io.novafoundation.nova.feature_swap_api.domain.model.SubmissionFeeWithLabel
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFee
import io.novafoundation.nova.feature_swap_api.domain.model.SwapGraphEdge
import io.novafoundation.nova.feature_swap_api.domain.model.SwapMaxAdditionalAmountDeduction
import io.novafoundation.nova.feature_swap_api.domain.model.SwapPoolId
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuote
import io.novafoundation.nova.feature_swap_api.domain.model.SwapRouteEdgeId
import io.novafoundation.nova.feature_swap_core_api.data.paths.model.PathRoughFeeEstimation
import io.novafoundation.nova.feature_swap_core_api.data.paths.model.QuotedEdge
import io.novafoundation.nova.feature_swap_core_api.data.paths.model.QuotedPath
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.SwapDirection
import io.novafoundation.nova.feature_swap_impl.domain.AssetInAdditionalSwapDeductionUseCase
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapDustCheckFee
import io.novafoundation.nova.feature_swap_impl.presentation.main.input.SwapMaxActionProviderFactory
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixinBase.InputState
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixinBase.InputState.InputKind
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.BaseAmountChooserProvider
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.invokeMaxClick
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.isMaxAction
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction.MaxActionProvider
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.setAmountInput
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeStatus
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class SwapMaxActionProviderTest {

    private val registry = mock(ChainRegistry::class.java)

    private val deduction = mock(AssetInAdditionalSwapDeductionUseCase::class.java)

    private val factory = SwapMaxActionProviderFactory(registry, deduction)

    private val input = chainAsset(0)

    private val output = chainAsset(1)

    private val assets = MutableStateFlow(balance(10_000))

    private val feeResetVersion = MutableStateFlow(0)

    private val amountInputState = MutableStateFlow(InputState.empty())

    private val fees = MutableStateFlow<FeeStatus<SwapFee, Unit>>(FeeStatus.NoFee)

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun `input and confirmation use the same buffered maximum once the fee arrives`() = runBlocking {
        prepareReserve()
        val inputProvider = createProvider(fees)
        inputProvider.awaitBalance(9_940)

        val feeStatus = feeStatus(1_000)
        fees.value = feeStatus
        inputProvider.awaitBalance(8_930)
        inputProvider.affordableBalanceProvider.awaitBalance(8_940)

        val confirmationProvider = createProvider(MutableStateFlow(feeStatus))
        confirmationProvider.awaitBalance(8_930)
    }

    @Test
    fun `active max keeps the fee that set the amount within one percent while validation uses the new fee`() = runBlocking {
        fees.value = feeStatus(1_000)
        val provider = createProvider(fees)
        val amountChooser = BaseAmountChooserProvider(
            coroutineScope = scope,
            tokenFlow = flowOf(null),
            maxActionProvider = provider,
            inputState = amountInputState
        )
        provider.awaitBalance(8_930)

        val maxInput = withTimeout(5_000) {
            amountChooser.invokeMaxClick()
            amountInputState.first { it.inputKind.isMaxAction() }
        }
        assertEquals("8930", maxInput.value)

        fees.value = feeStatus(1_008)
        provider.affordableBalanceProvider.awaitBalance(8_932)
        assertEquals(BigInteger.valueOf(8_930), provider.maxAvailableBalance.first().actualBalance)
        assertEquals(SwapDustCheckFee.MaxFeeWithMargin(1_010.toBigInteger()), provider.dustCheckFeeFor(quote(8_930)))
        assertEquals(SwapDustCheckFee.QuoteFee, provider.dustCheckFeeFor(quote(8_929)))
        assertEquals(SwapDustCheckFee.QuoteFee, provider.dustCheckFeeFor(quote(8_930, pool = "new")))

        fees.value = feeStatus(1_010)
        provider.affordableBalanceProvider.awaitBalance(8_930)
        assertEquals(BigInteger.valueOf(8_930), provider.maxAvailableBalance.first().actualBalance)
        assertEquals(maxInput, amountInputState.value)

        fees.value = feeStatus(1_016)
        provider.affordableBalanceProvider.awaitBalance(8_924)
        provider.awaitBalance(8_914)
        withTimeout(5_000) { amountInputState.first { it.value == "8914" } }

        fees.value = feeStatus(1_008)
        provider.affordableBalanceProvider.awaitBalance(8_932)
        assertEquals(BigInteger.valueOf(8_914), provider.maxAvailableBalance.first().actualBalance)

        amountChooser.setAmountInput("5000", initiatedByUser = true)
        provider.awaitBalance(8_922)
    }

    @Test
    fun `route fee asset and reserve changes each reset the max fee anchor`() = runBlocking {
        amountInputState.value = InputState("8930", initiatedByUser = true, inputKind = InputKind.MAX_ACTION)
        fees.value = feeStatus(1_000)
        val provider = createProvider(fees)
        provider.awaitBalance(8_930)

        fees.value = feeStatus(1_008, pool = "new")
        provider.awaitBalance(8_922)

        val feeAsset = chainAsset(2)
        fees.value = feeStatus(1_016, pool = "new", feeAsset = feeAsset)
        provider.awaitBalance(8_914)

        fees.value = feeStatus(1_024, pool = "new", feeAsset = feeAsset, reserve = 150)
        provider.awaitBalance(8_856)

        assets.value = balance(11_000)
        provider.awaitBalance(9_856)
    }

    @Test
    fun `reset discards the previous fee for the same asset pair`() = runBlocking {
        prepareReserve()
        amountInputState.value = InputState("8930", initiatedByUser = true, inputKind = InputKind.MAX_ACTION)
        fees.value = feeStatus(1_000)
        val provider = createProvider(fees)
        provider.awaitBalance(8_930)

        fees.value = FeeStatus.NoFee
        feeResetVersion.value = 1
        provider.awaitBalance(9_940)
        fees.value = feeStatus(1_008)
        provider.awaitBalance(8_922)
    }

    private fun quote(amount: Long, pool: String = "pool"): SwapQuote {
        val amountIn = BigInteger.valueOf(amount)
        val inputId = input.fullId
        val outputId = output.fullId
        val poolId = SwapPoolId(input.chainId, pool)
        val edge = mock(SwapGraphEdge::class.java)
        `when`(edge.from).thenReturn(inputId)
        `when`(edge.to).thenReturn(outputId)
        `when`(edge.exchangeId).thenReturn("swap")
        `when`(edge.poolId).thenReturn(poolId)
        val path = QuotedPath(
            direction = SwapDirection.SPECIFIED_IN,
            path = listOf(QuotedEdge(amountIn, BigInteger.ONE, edge)),
            roughFeeEstimation = PathRoughFeeEstimation.zero(),
            netAmountOut = BigInteger.ONE
        )

        return mock(SwapQuote::class.java).also {
            `when`(it.direction).thenReturn(SwapDirection.SPECIFIED_IN)
            `when`(it.assetIn).thenReturn(input)
            `when`(it.planksIn).thenReturn(amountIn)
            `when`(it.quotedPath).thenReturn(path)
        }
    }

    private suspend fun prepareReserve() {
        val chain = mock(Chain::class.java)
        `when`(registry.getChain("hub")).thenReturn(chain)
        `when`(chain.swap).thenReturn(listOf(Chain.Swap.ASSET_CONVERSION))
        `when`(deduction.invoke(input, output, true)).thenReturn(BigInteger.valueOf(100))
    }

    private fun createProvider(feeStatuses: MutableStateFlow<FeeStatus<SwapFee, Unit>>) =
        factory.create(
            scope = scope,
            assets = assets,
            outputAssets = flowOf(output),
            maxActive = amountInputState.map { it.inputKind.isMaxAction() }.distinctUntilChanged(),
            feeLoaderMixin = mock<FeeLoaderMixinV2<SwapFee, Unit>>().also { `when`(it.fee).thenReturn(feeStatuses) },
            feeResetVersion = feeResetVersion
        )

    private suspend fun MaxActionProvider.awaitBalance(amount: Long) {
        val expected = BigInteger.valueOf(amount)
        val balance = withTimeout(5_000) { maxAvailableBalance.first { it.actualBalance == expected } }
        assertEquals(expected, balance.displayedBalance)
    }

    private fun chainAsset(id: Int) = mock(Chain.Asset::class.java).also {
        `when`(it.chainId).thenReturn("hub")
        `when`(it.id).thenReturn(id)
        `when`(it.precision).thenReturn(Precision(0))
    }

    private fun balance(amount: Long): Asset {
        val token = mock(Token::class.java)
        `when`(token.configuration).thenReturn(input)

        return mock(Asset::class.java).also {
            `when`(it.token).thenReturn(token)
            `when`(it.transferableInPlanks).thenReturn(BigInteger.valueOf(amount))
            `when`(it.balanceCountedTowardsEDInPlanks).thenReturn(BigInteger.valueOf(amount + 40))
        }
    }

    private fun feeStatus(amount: Long, pool: String = "pool", feeAsset: Chain.Asset = input, reserve: Long = 100) =
        FeeStatus.Loaded(FeeModel(fee(amount, pool, feeAsset, reserve), Unit))

    private fun fee(amount: Long, pool: String, feeAsset: Chain.Asset, reserve: Long): SwapFee {
        val inputId = input.fullId
        val outputId = output.fullId
        val operation = mock(AtomicSwapOperation::class.java)
        `when`(operation.assetIn).thenReturn(inputId)
        `when`(operation.assetOut).thenReturn(outputId)

        val segment = mock(SwapFee.SwapSegment::class.java)
        `when`(segment.operation).thenReturn(operation)

        val submissionFee = mock(SubmissionFeeWithLabel::class.java)
        `when`(submissionFee.asset).thenReturn(feeAsset)
        val route = listOf(SwapRouteEdgeId(inputId, outputId, "swap", pool))
        val deduction = SwapMaxAdditionalAmountDeduction(BigInteger.valueOf(reserve))

        return mock(SwapFee::class.java).also {
            `when`(it.additionalMaxAmountDeduction).thenReturn(deduction)
            `when`(it.totalFeeAmount(input)).thenReturn(BigInteger.valueOf(amount))
            `when`(it.initialSubmissionFee).thenReturn(submissionFee)
            `when`(it.segments).thenReturn(listOf(segment))
            `when`(it.route).thenReturn(route)
        }
    }
}
