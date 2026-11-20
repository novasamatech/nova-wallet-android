package io.novafoundation.nova.feature_swap_impl.domain

import io.novafoundation.nova.common.data.network.runtime.binding.AccountInfo
import io.novafoundation.nova.feature_account_api.data.extrinsic.SubmissionOrigin
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicSwapOperation
import io.novafoundation.nova.feature_swap_api.domain.model.SubmissionFeeWithLabel
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFee
import io.novafoundation.nova.feature_swap_api.domain.model.SwapMaxAdditionalAmountDeduction
import io.novafoundation.nova.feature_swap_api.domain.model.fee.AtomicSwapOperationFee
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSource
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.AssetBalance
import io.novafoundation.nova.feature_wallet_api.data.repository.AccountInfoRepository
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`

class SwapInputReserveTest {

    private val assetSourceRegistry = mock(AssetSourceRegistry::class.java)

    private val accountInfoRepository = mock(AccountInfoRepository::class.java)

    private val reserve = SwapInputReserve(assetSourceRegistry, accountInfoRepository)

    private val assetIn = mock(Chain.Asset::class.java)

    private val operation = mock(AtomicSwapOperation::class.java)

    private val segment = mock(SwapFee.SwapSegment::class.java)

    private val existentialDeposit = BigInteger("100000000")

    private val executingAccount = ByteArray(32) { 1 }

    @Test
    fun `native XCM swap keeps ED for the account whose assets are spent`() = runBlocking {
        setUpRoute()
        setUpAccount(consumers = BigInteger.ONE)

        val reserve = resolveReserve()
        val transferable = BigInteger("10000000000")
        val networkFee = BigInteger("12600000")

        assertEquals(existentialDeposit, reserve)
        assertEquals(BigInteger("9887400000"), swapMaximum(transferable, transferable, reserve, networkFee))
        assertEquals(transferable - networkFee, swapMaximum(transferable, transferable + existentialDeposit, reserve, networkFee))
    }

    @Test
    fun `native XCM swap can spend full balance after fees when there are no consumers`() = runBlocking {
        setUpRoute()
        setUpAccount(consumers = BigInteger.ZERO)

        assertEquals(BigInteger.ZERO, resolveReserve())
    }

    @Test
    fun `existing route reserve covers ED without a second account lookup`() = runBlocking {
        val routeReserve = existentialDeposit * BigInteger.TWO
        setUpRoute(routeReserve)

        assertEquals(routeReserve, resolveReserve())
        verifyNoInteractions(accountInfoRepository)
    }

    @Test
    fun `non-native input retains route reserve independently of native consumers`() = runBlocking {
        val routeReserve = BigInteger.TEN
        setUpRoute(routeReserve)
        `when`(assetIn.type).thenReturn(Chain.Asset.Type.EvmErc20("token"))

        assertEquals(routeReserve, resolveReserve())
        verifyNoInteractions(accountInfoRepository)
    }

    private suspend fun setUpRoute(routeReserve: BigInteger = BigInteger.ZERO) {
        val source = mock(AssetSource::class.java)
        val balance = mock(AssetBalance::class.java)
        `when`(assetIn.chainId).thenReturn("asset-hub")
        `when`(assetIn.type).thenReturn(Chain.Asset.Type.Native)
        `when`(assetSourceRegistry.sourceFor(assetIn)).thenReturn(source)
        `when`(source.balance).thenReturn(balance)
        `when`(balance.existentialDeposit(assetIn)).thenReturn(existentialDeposit)
        `when`(segment.operation).thenReturn(operation)
        `when`(operation.additionalMaxAmountDeduction()).thenReturn(SwapMaxAdditionalAmountDeduction(routeReserve))
    }

    private suspend fun setUpAccount(consumers: BigInteger) {
        val fee = mock(AtomicSwapOperationFee::class.java)
        val submissionFee = mock(SubmissionFeeWithLabel::class.java)
        val signingAccount = ByteArray(32) { 2 }
        val origin = SubmissionOrigin(executingAccount, signingAccount)
        val accountInfo = AccountInfo(
            consumers = consumers,
            providers = BigInteger.ONE,
            sufficients = BigInteger.ZERO,
            data = AccountInfo.empty().data
        )

        `when`(segment.fee).thenReturn(fee)
        `when`(fee.submissionFee).thenReturn(submissionFee)
        `when`(submissionFee.submissionOrigin).thenReturn(origin)
        `when`(accountInfoRepository.getAccountInfo("asset-hub", executingAccount)).thenReturn(accountInfo)
    }

    private suspend fun resolveReserve(): BigInteger {
        return reserve.requiredFor(assetIn, segment).fromCountedTowardsEd
    }
}
