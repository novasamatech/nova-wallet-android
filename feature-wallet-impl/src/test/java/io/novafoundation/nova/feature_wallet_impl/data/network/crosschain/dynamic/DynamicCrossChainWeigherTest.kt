package io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.dynamic

import android.util.Log
import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.common.utils.asPrecision
import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentCurrency
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferBase
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.XcmTransferDryRunOrigin
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic.DynamicCrossChainTransferConfiguration
import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.dynamic.dryRun.XcmTransferDryRunResult
import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.dynamic.dryRun.XcmTransferDryRunner
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal
import java.math.BigInteger
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito

class DynamicCrossChainWeigherTest {

    private val chain = chainWithSingleAsset()
    private val asset = chain.assets.single()

    private val config = Mockito.mock(DynamicCrossChainTransferConfiguration::class.java)

    @Test
    fun `estimates fee with the default amount when the dry run succeeds`() = withSilencedLog {
        val dryRunner = RecordingDryRunner(failsAtOrAbove = null)

        val fee = runBlocking { DynamicCrossChainWeigher(dryRunner).estimateFee(config, transfer(amount = "0.5")) }

        assertEquals(listOf(planks("100")), dryRunner.attemptedAmounts)
        assertEquals(DELIVERY_FEE, fee.paidByAccount)
        assertEquals(EXECUTION_FEE, fee.paidFromHolding)
    }

    @Test
    fun `retries with a smaller amount when the default dry run fails`() = withSilencedLog {
        val dryRunner = RecordingDryRunner(failsAtOrAbove = planks("100"))

        val fee = runBlocking { DynamicCrossChainWeigher(dryRunner).estimateFee(config, transfer(amount = "0.5")) }

        assertEquals(listOf(planks("100"), planks("1")), dryRunner.attemptedAmounts)
        assertEquals(DELIVERY_FEE, fee.paidByAccount)
        assertEquals(EXECUTION_FEE, fee.paidFromHolding)
    }

    @Test
    fun `keeps the user amount when it exceeds the fallback minimum`() = withSilencedLog {
        val dryRunner = RecordingDryRunner(failsAtOrAbove = planks("100"))

        runBlocking { DynamicCrossChainWeigher(dryRunner).estimateFee(config, transfer(amount = "7")) }

        assertEquals(listOf(planks("100"), planks("7")), dryRunner.attemptedAmounts)
    }

    @Test(expected = IllegalStateException::class)
    fun `rethrows when the fallback dry run fails as well`() = withSilencedLog {
        val dryRunner = RecordingDryRunner(failsAtOrAbove = BigInteger.ZERO)

        runBlocking { DynamicCrossChainWeigher(dryRunner).estimateFee(config, transfer(amount = "0.5")) }
    }

    private fun withSilencedLog(block: () -> Unit) {
        // android.util.Log is not available on the JVM
        Mockito.mockStatic(Log::class.java).use { block() }
    }

    private fun transfer(amount: String): AssetTransferBase {
        return AssetTransferBase(
            recipient = "recipient",
            originChain = chain,
            originChainAsset = asset,
            destinationChain = chain,
            destinationChainAsset = asset,
            feePaymentCurrency = FeePaymentCurrency.Native,
            amountPlanks = planks(amount)
        )
    }

    private fun planks(amount: String): Balance = asset.planksFromAmount(BigDecimal(amount))

    private class RecordingDryRunner(private val failsAtOrAbove: Balance?) : XcmTransferDryRunner {

        val attemptedAmounts = mutableListOf<Balance>()

        override suspend fun dryRunXcmTransfer(
            config: DynamicCrossChainTransferConfiguration,
            transfer: AssetTransferBase,
            origin: XcmTransferDryRunOrigin
        ): Result<XcmTransferDryRunResult> {
            attemptedAmounts += transfer.amountPlanks

            if (failsAtOrAbove != null && transfer.amountPlanks >= failsAtOrAbove) {
                return Result.failure(IllegalStateException("Local execution incomplete"))
            }

            val result = XcmTransferDryRunResult(
                origin = XcmTransferDryRunResult.IntermediateSegment(deliveryFee = DELIVERY_FEE, trapped = BigInteger.ZERO),
                remoteReserve = null,
                destination = XcmTransferDryRunResult.FinalSegment(depositedAmount = transfer.amountPlanks - EXECUTION_FEE)
            )

            return Result.success(result)
        }
    }

    private fun chainWithSingleAsset(): Chain {
        val asset = Chain.Asset(
            icon = null,
            id = 0,
            priceId = null,
            chainId = "chain",
            symbol = TokenSymbol("ETH"),
            precision = 18.asPrecision(),
            buyProviders = emptyMap(),
            sellProviders = emptyMap(),
            staking = emptyList(),
            type = Chain.Asset.Type.Native,
            source = Chain.Asset.Source.DEFAULT,
            name = "Ethereum"
        )

        return Chain(
            id = "chain",
            name = "Chain",
            assets = listOf(asset),
            nodes = Chain.Nodes(
                autoBalanceStrategy = Chain.Nodes.AutoBalanceStrategy.ROUND_ROBIN,
                wssNodeSelectionStrategy = Chain.Nodes.NodeSelectionStrategy.AutoBalance,
                nodes = emptyList()
            ),
            explorers = emptyList(),
            externalApis = emptyList(),
            icon = null,
            addressPrefix = 42,
            legacyAddressPrefix = null,
            types = null,
            isEthereumBased = false,
            isTestNet = false,
            source = Chain.Source.DEFAULT,
            hasSubstrateRuntime = true,
            pushSupport = false,
            hasCrowdloans = false,
            supportProxy = false,
            governance = emptyList(),
            swap = emptyList(),
            customFee = emptyList(),
            multisigSupport = false,
            connectionState = Chain.ConnectionState.FULL_SYNC,
            parentId = null,
            additional = null
        )
    }

    private companion object {
        val DELIVERY_FEE: Balance = BigInteger.valueOf(5)
        val EXECUTION_FEE: Balance = BigInteger.valueOf(7)
    }
}
