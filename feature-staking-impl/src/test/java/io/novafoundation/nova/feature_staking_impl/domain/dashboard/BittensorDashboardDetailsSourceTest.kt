package io.novafoundation.nova.feature_staking_impl.domain.dashboard

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.BittensorDashboardDetails
import com.google.gson.Gson
import io.novafoundation.nova.feature_staking_impl.data.bittensor.InMemoryPreferences
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorPositionRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.RealBittensorDashboardSnapshotRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorPortfolioState
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorDataStatus
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioPositions
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRecommendedYieldInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootPosition
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetPosition
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
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
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

    private val otherHotkey = AccountIdKey(ByteArray(32) { 2 })

    private val subnet = BittensorNetuid.Subnet(1)

    private val portfolio = BittensorPortfolioPositions(
        listOf(BittensorRootPosition(hotkey, BigInteger.TEN)),
        emptyList(),
        emptyMap()
    )

    private val portfolioState = MutableStateFlow(BittensorPortfolioState(portfolio, BittensorDataStatus.Fresh))

    private val snapshots = RealBittensorDashboardSnapshotRepository(InMemoryPreferences(), Gson())

    private val source = BittensorDashboardDetailsSource(positions, validators, recommendations, snapshots, logger)

    @Test
    fun `timed out root rate settles without a rate until root holdings change`() = runTest {
        prepare()
        val timeout = try {
            withTimeout(1) { awaitCancellation() }
        } catch (failure: TimeoutCancellationException) {
            failure
        }
        `when`(validators.yields(BittensorNetuid.Root)).thenThrow(timeout).thenReturn(mapOf(hotkey to BigDecimal.TEN, otherHotkey to BigDecimal.ONE))
        val emitted = mutableListOf<BittensorDashboardDetails>()
        val job = backgroundScope.launch {
            source.observe(account, listOf(chain)).collect { it["chain"]?.let(emitted::add) }
        }
        runCurrent()
        assertEquals(BigInteger.TEN, emitted.last().totalStakeInPlanks)
        assertNull(emitted.last().rootAnnualPercentageYield)
        assertFalse(emitted.last().rootYieldLoading)

        advanceTimeBy(120_000)
        runCurrent()
        assertNull(emitted.last().rootAnnualPercentageYield)
        verify(validators, times(1)).yields(BittensorNetuid.Root)

        val twoValidators = listOf(BittensorRootPosition(hotkey, BigInteger.TEN), BittensorRootPosition(otherHotkey, BigInteger.TEN))
        portfolioState.value = BittensorPortfolioState(portfolio.copy(root = twoValidators), BittensorDataStatus.Fresh)
        runCurrent()
        assertEquals(0, BigDecimal("5.5").compareTo(emitted.last().rootAnnualPercentageYield))
        verify(validators, times(2)).yields(BittensorNetuid.Root)
        job.cancel()
    }

    @Test
    fun `rates are requested once per held set and subnet rates not next to root stake`() = runTest {
        prepare()
        val mixed = portfolio.copy(subnets = listOf(BittensorSubnetPosition(subnet, hotkey, BigInteger.TEN)))
        portfolioState.value = BittensorPortfolioState(mixed, BittensorDataStatus.Fresh)
        val job = backgroundScope.launch { source.observe(account, listOf(chain)).collect() }
        runCurrent()
        portfolioState.value = BittensorPortfolioState(mixed.copy(root = listOf(BittensorRootPosition(hotkey, BigInteger.ONE))), BittensorDataStatus.Fresh)
        advanceTimeBy(120_000)
        runCurrent()
        verify(validators, times(1)).yields(BittensorNetuid.Root)
        verify(validators, never()).yields(subnet)
        job.cancel()
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

    @Test
    fun `active stake does not request start staking recommendations`() = runTest {
        prepare()
        val job = backgroundScope.launch { source.observe(account, listOf(chain)).collect() }
        runCurrent()
        advanceTimeBy(120_000)
        source.refreshMaximumYield()
        runCurrent()
        verifyNoInteractions(recommendations)
        job.cancel()
    }

    @Test
    fun `failed maximum yield is requested again only on refresh`() = runTest {
        prepare()
        portfolioState.value = BittensorPortfolioState(BittensorPortfolioPositions(emptyList(), emptyList(), emptyMap()), BittensorDataStatus.Fresh)
        `when`(recommendations.maximumYields()).thenThrow(IllegalStateException("offline")).thenReturn(mapOf(subnet to BigDecimal.ONE))
        val emitted = mutableListOf<BittensorDashboardDetails>()
        val job = backgroundScope.launch { source.observe(account, listOf(chain)).collect { it["chain"]?.let(emitted::add) } }
        runCurrent()
        advanceTimeBy(120_000)
        runCurrent()
        assertNull(emitted.last().maximumAnnualPercentageYield)
        assertFalse(emitted.last().maximumYieldLoading)
        verify(recommendations, times(1)).maximumYields()

        source.refreshMaximumYield()
        runCurrent()
        assertEquals(BigDecimal.ONE, emitted.last().maximumAnnualPercentageYield)
        verify(recommendations, times(2)).maximumYields()
        job.cancel()
    }

    @Test
    fun `no stake requests recommendations and claimable rewards count as active`() = runTest {
        prepare()
        portfolioState.value = BittensorPortfolioState(BittensorPortfolioPositions(emptyList(), emptyList(), emptyMap()), BittensorDataStatus.Fresh)
        val emitted = mutableListOf<BittensorDashboardDetails>()
        val job = backgroundScope.launch { source.observe(account, listOf(chain)).collect { it["chain"]?.let(emitted::add) } }
        runCurrent()
        assertEquals(BigDecimal.ONE, emitted.last().maximumAnnualPercentageYield)
        verify(recommendations, times(1)).maximumYields()

        snapshots.saveClaimableRootRewards("chain", coldKey, BigInteger.TEN)
        runCurrent()
        assertEquals(BigInteger.TEN, emitted.last().claimableRootRewardsInPlanks)
        assertTrue(emitted.last().showsClaimableRewardsPanel)
        advanceTimeBy(120_000)
        runCurrent()
        verify(recommendations, times(1)).maximumYields()
        verifyNoInteractions(validators)
        job.cancel()
    }

    @Test
    fun `fresh figures are persisted and shown while positions are unavailable on the next start`() = runTest {
        prepare()
        val first = backgroundScope.launch { source.observe(account, listOf(chain)).collect() }
        runCurrent()
        first.cancel()
        val stored = requireNotNull(snapshots.snapshot("chain", coldKey))
        assertEquals(BigInteger.TEN, stored.rootStakeInPlanks)
        assertEquals(0, BigDecimal.TEN.compareTo(stored.rootAnnualPercentageYield))

        // Cold start offline: positions never load, rates fail.
        portfolioState.value = BittensorPortfolioState(null, BittensorDataStatus.Stale(IllegalStateException("offline")))
        `when`(validators.yields(BittensorNetuid.Root)).thenThrow(IllegalStateException("offline"))
        val details = source.observe(account, listOf(chain)).first().getValue("chain")

        assertEquals(BittensorDashboardDetails.Pricing.KNOWN, details.pricing)
        assertEquals(BigInteger.TEN, details.totalStakeInPlanks)
        assertEquals(BigInteger.TEN, details.panelStakeInPlanks)
        assertEquals(0, BigDecimal.TEN.compareTo(details.panelAnnualPercentageYield))
        assertFalse(details.panelYieldLoading)
    }

    @Test
    fun `unpriced held subnets keep the stored total until their price request completes`() = runTest {
        prepare()
        val first = backgroundScope.launch { source.observe(account, listOf(chain)).collect() }
        runCurrent()
        first.cancel()

        val withSubnet = portfolio.copy(subnets = listOf(BittensorSubnetPosition(subnet, hotkey, BigInteger.TEN)))
        portfolioState.value = BittensorPortfolioState(withSubnet, BittensorDataStatus.Fresh, pricesPending = true)
        val emitted = mutableListOf<BittensorDashboardDetails>()
        val job = backgroundScope.launch { source.observe(account, listOf(chain)).collect { it["chain"]?.let(emitted::add) } }
        runCurrent()
        assertEquals(BittensorDashboardDetails.Pricing.LOADING, emitted.last().pricing)
        assertEquals(BigInteger.TEN, snapshots.snapshot("chain", coldKey)?.totalStakeInPlanks)

        portfolioState.value = BittensorPortfolioState(withSubnet, BittensorDataStatus.Fresh, pricesPending = false)
        runCurrent()
        assertEquals(BittensorDashboardDetails.Pricing.UNKNOWN, emitted.last().pricing)
        assertNull(snapshots.snapshot("chain", coldKey)?.totalStakeInPlanks)
        job.cancel()
    }

    @Test
    fun `stored maximum yield is shown while positions are unknown and kept when its request fails`() = runTest {
        prepare()
        val noStake = BittensorPortfolioState(BittensorPortfolioPositions(emptyList(), emptyList(), emptyMap()), BittensorDataStatus.Fresh)
        portfolioState.value = noStake
        val first = backgroundScope.launch { source.observe(account, listOf(chain)).collect() }
        runCurrent()
        first.cancel()

        portfolioState.value = BittensorPortfolioState(null, BittensorDataStatus.Loading)
        val emitted = mutableListOf<BittensorDashboardDetails>()
        val job = backgroundScope.launch { source.observe(account, listOf(chain)).collect { it["chain"]?.let(emitted::add) } }
        runCurrent()
        assertEquals(BigDecimal.ONE, emitted.last().maximumAnnualPercentageYield)
        assertFalse(emitted.last().maximumYieldLoading)

        `when`(recommendations.maximumYields()).thenThrow(IllegalStateException("offline"))
        portfolioState.value = noStake
        runCurrent()
        assertEquals(BigDecimal.ONE, emitted.last().maximumAnnualPercentageYield)
        assertEquals(BigDecimal.ONE, snapshots.snapshot("chain", coldKey)?.maximumAnnualPercentageYield)
        verify(recommendations, times(2)).maximumYields()
        job.cancel()
    }

    @Test
    fun `recommendations wait for unknown positions and load once they resolve to no stake`() = runTest {
        prepare()
        portfolioState.value = BittensorPortfolioState(null, BittensorDataStatus.Loading)
        val emitted = mutableListOf<BittensorDashboardDetails>()
        val job = backgroundScope.launch { source.observe(account, listOf(chain)).collect { it["chain"]?.let(emitted::add) } }
        runCurrent()
        assertTrue(emitted.last().maximumYieldLoading)
        verifyNoInteractions(recommendations)

        portfolioState.value = BittensorPortfolioState(null, BittensorDataStatus.Stale(IllegalStateException("offline")))
        runCurrent()
        assertFalse(emitted.last().maximumYieldLoading)
        verifyNoInteractions(recommendations)

        portfolioState.value = BittensorPortfolioState(BittensorPortfolioPositions(emptyList(), emptyList(), emptyMap()), BittensorDataStatus.Fresh)
        runCurrent()
        assertEquals(BigDecimal.ONE, emitted.last().maximumAnnualPercentageYield)
        verify(recommendations, times(1)).maximumYields()
        job.cancel()
    }

    @Test
    fun `without a stored snapshot unknown positions stay loading`() = runTest {
        prepare()
        portfolioState.value = BittensorPortfolioState(null, BittensorDataStatus.Loading)
        val details = source.observe(account, listOf(chain)).first().getValue("chain")
        assertEquals(BittensorDashboardDetails.Pricing.LOADING, details.pricing)
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
