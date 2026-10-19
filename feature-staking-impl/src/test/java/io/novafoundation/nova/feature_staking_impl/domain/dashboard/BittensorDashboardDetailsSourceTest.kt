package io.novafoundation.nova.feature_staking_impl.domain.dashboard

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.BittensorDashboardDetails
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorPositionRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorPortfolioState
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorDataStatus
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioPositions
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRecommendedYieldInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootPosition
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorInteractor
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.math.BigInteger

@OptIn(ExperimentalCoroutinesApi::class)
class BittensorDashboardDetailsSourceTest {

    private val positions = mock(BittensorPositionRepository::class.java)

    private val validators = mock(BittensorValidatorInteractor::class.java)

    private val recommendations = mock(BittensorRecommendedYieldInteractor::class.java)

    private val logger = mock(Logger::class.java)

    private val account = mock(MetaAccount::class.java)

    private val chain = mock(Chain::class.java)

    private val asset = mock(Chain.Asset::class.java)

    private val coldKey = ByteArray(32)

    private val hotkey = AccountIdKey(ByteArray(32) { 1 })

    private val subnet = BittensorNetuid.Subnet(1)

    private val portfolio = BittensorPortfolioPositions(
        listOf(BittensorRootPosition(hotkey, BigInteger.TEN)),
        emptyList(),
        emptyMap()
    )

    private val portfolioState = MutableStateFlow(BittensorPortfolioState(portfolio, BittensorDataStatus.Fresh))

    private val source = BittensorDashboardDetailsSource(positions, validators, recommendations, logger)

    @Test
    fun `owned network timeout clears rate and independently recovers on refresh`() = runTest {
        prepare()
        val timeout = try {
            withTimeout(1) { awaitCancellation() }
        } catch (failure: TimeoutCancellationException) {
            failure
        }
        `when`(validators.yields(BittensorNetuid.Root)).thenThrow(timeout).thenReturn(mapOf(hotkey to BigDecimal.TEN))
        val emitted = mutableListOf<BittensorDashboardDetails>()
        val job = backgroundScope.launch {
            source.observe(account, listOf(chain)).collect { it["chain"]?.let(emitted::add) }
        }
        runCurrent()
        assertEquals(BigInteger.TEN, emitted.last().totalStakeInPlanks)
        assertNull(emitted.last().rootAnnualPercentageYield)
        assertFalse(emitted.last().rootYieldLoading)
        assertEquals(BigDecimal.ONE, emitted.last().maximumAnnualPercentageYield)

        advanceTimeBy(60_000)
        runCurrent()
        assertEquals(0, BigDecimal.TEN.compareTo(emitted.last().rootAnnualPercentageYield))
        verify(validators, times(2)).yields(BittensorNetuid.Root)
        job.cancel()
    }

    @Test
    fun `cancelling consumer stops refreshes`() = runTest {
        prepare()
        val job = backgroundScope.launch { source.observe(account, listOf(chain)).collect() }
        runCurrent()
        job.cancel()
        runCurrent()
        advanceTimeBy(120_000)
        runCurrent()
        verify(validators, times(1)).yields(BittensorNetuid.Root)
    }

    @Test
    fun `actual cancellation is not converted to a missing rate`() = runTest {
        prepare()
        `when`(validators.yields(BittensorNetuid.Root)).thenThrow(CancellationException("cancel"))
        val job = backgroundScope.launch { source.observe(account, listOf(chain)).collect() }
        runCurrent()
        advanceTimeBy(120_000)
        runCurrent()
        verify(validators, times(1)).yields(BittensorNetuid.Root)
        verifyNoInteractions(logger)
        job.cancel()
    }

    @Test
    fun `wallet without account still receives maximum yield without position reads`() = runTest {
        prepare()
        `when`(account.accountIdIn(chain)).thenReturn(null)
        val details = source.observe(account, listOf(chain)).first { it["chain"]?.maximumAnnualPercentageYield != null }.getValue("chain")
        assertEquals(BigInteger.ZERO, details.totalStakeInPlanks)
        assertEquals(BigDecimal.ONE, details.maximumAnnualPercentageYield)
        verifyNoInteractions(positions)
    }

    private suspend fun prepare() {
        `when`(chain.id).thenReturn("chain")
        `when`(chain.assets).thenReturn(listOf(asset))
        `when`(asset.staking).thenReturn(listOf(Chain.Asset.StakingType.BITTENSOR_ROOT))
        `when`(account.accountIdIn(chain)).thenReturn(coldKey)
        `when`(positions.observePortfolio("chain", coldKey)).thenReturn(portfolioState)
        `when`(validators.yields(BittensorNetuid.Root)).thenReturn(mapOf(hotkey to BigDecimal.TEN))
        `when`(recommendations.maximumYields()).thenReturn(mapOf(subnet to BigDecimal.ONE))
    }
}
