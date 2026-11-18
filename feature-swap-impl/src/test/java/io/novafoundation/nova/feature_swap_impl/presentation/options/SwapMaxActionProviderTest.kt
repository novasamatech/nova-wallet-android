package io.novafoundation.nova.feature_swap_impl.presentation.options

import io.novafoundation.nova.feature_swap_api.domain.model.AtomicSwapOperation
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFee
import io.novafoundation.nova.feature_swap_api.domain.model.SwapMaxAdditionalAmountDeduction
import io.novafoundation.nova.feature_swap_impl.domain.AssetInAdditionalSwapDeductionUseCase
import io.novafoundation.nova.feature_swap_impl.presentation.main.input.SwapMaxActionProviderFactory
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class SwapMaxActionProviderTest {

    private val registry = mock(ChainRegistry::class.java)

    private val deduction = mock(AssetInAdditionalSwapDeductionUseCase::class.java)

    private val factory = SwapMaxActionProviderFactory(registry, deduction)

    @Test
    fun `input and confirmation use the same maximum once the fee arrives`() = runBlocking {
        val chain = mock(Chain::class.java)
        val input = mock(Chain.Asset::class.java)
        val output = mock(Chain.Asset::class.java)
        val token = mock(Token::class.java)
        val asset = mock(Asset::class.java)
        val fee = mock(SwapFee::class.java)
        val operation = mock(AtomicSwapOperation::class.java)
        val segment = mock(SwapFee.SwapSegment::class.java)
        val reserve = SwapMaxAdditionalAmountDeduction(BigInteger.valueOf(100))
        `when`(input.chainId).thenReturn("hub")
        `when`(input.id).thenReturn(0)
        `when`(output.chainId).thenReturn("hub")
        `when`(output.id).thenReturn(1)
        `when`(registry.getChain("hub")).thenReturn(chain)
        `when`(chain.swap).thenReturn(listOf(Chain.Swap.ASSET_CONVERSION))
        `when`(deduction.invoke(input, output, true)).thenReturn(BigInteger.valueOf(100))
        `when`(token.configuration).thenReturn(input)
        `when`(asset.token).thenReturn(token)
        `when`(asset.transferableInPlanks).thenReturn(BigInteger.valueOf(1000))
        `when`(asset.balanceCountedTowardsEDInPlanks).thenReturn(BigInteger.valueOf(1040))
        `when`(fee.additionalMaxAmountDeduction).thenReturn(reserve)
        `when`(fee.totalFeeAmount(input)).thenReturn(BigInteger.TEN)
        val inputId = input.fullId
        val outputId = output.fullId
        `when`(operation.assetIn).thenReturn(inputId)
        `when`(operation.assetOut).thenReturn(outputId)
        `when`(segment.operation).thenReturn(operation)
        `when`(fee.segments).thenReturn(listOf(segment))
        val fees = MutableStateFlow<FeeStatus<SwapFee, Unit>>(FeeStatus.NoFee)
        val inputFeeLoader = mock<FeeLoaderMixinV2<SwapFee, Unit>>()
        `when`(inputFeeLoader.fee).thenReturn(fees)
        val job = SupervisorJob()
        val scope = CoroutineScope(Dispatchers.Default + job)
        try {
            val provider = factory.create(scope, flowOf(asset), flowOf(output), inputFeeLoader)
            val initial = withTimeout(5000) { provider.maxAvailableBalance.first() }
            assertEquals(BigInteger.valueOf(940), initial.actualBalance)
            assertEquals(initial.actualBalance, initial.displayedBalance)

            val feeStatus = FeeStatus.Loaded(FeeModel(fee, Unit))
            fees.value = feeStatus
            val loaded = withTimeout(5000) { provider.maxAvailableBalance.first { it.actualBalance == BigInteger.valueOf(930) } }
            assertEquals(loaded.actualBalance, loaded.displayedBalance)

            val confirmationFeeLoader = mock<FeeLoaderMixinV2<SwapFee, Unit>>()
            `when`(confirmationFeeLoader.fee).thenReturn(MutableStateFlow(feeStatus))
            val confirmationProvider = factory.create(scope, flowOf(asset), flowOf(output), confirmationFeeLoader)
            val confirmed = withTimeout(5000) { confirmationProvider.maxAvailableBalance.first() }
            assertEquals(loaded, confirmed)
        } finally {
            scope.cancel()
        }
    }
}
