package io.novafoundation.nova.feature_staking_impl.domain.dashboard

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.AggregatedStakingDashboardOption
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.BittensorDashboardDetails
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.StakingDashboardValue
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
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.AccountId
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
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

    private val token = mock(Token::class.java)

    private val coldKey: AccountId = ByteArray(32)

    private val hotkey = AccountIdKey(ByteArray(32) { 1 })

    private val otherHotkey = AccountIdKey(ByteArray(32) { 2 })

    private val subnet = BittensorNetuid.Subnet(1)

    private val portfolio = BittensorPortfolioPositions(
        listOf(BittensorRootPosition(hotkey, BigInteger.TEN)),
        subnets = emptyList(),
        subnetPrices = emptyMap()
    )

    private val emptyPositions = BittensorPortfolioPositions(root = emptyList(), subnets = emptyList(), subnetPrices = emptyMap())

    private val portfolioState = MutableStateFlow(BittensorPortfolioState(portfolio, BittensorDataStatus.Fresh))

    private val preferences = InMemoryPreferences()

    private val gson = Gson()

    private val snapshots = RealBittensorDashboardSnapshotRepository(preferences, gson)

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
        assertEquals(StakingDashboardValue.Known(BigInteger.TEN), emitted.last().totalStake)
        assertSame(StakingDashboardValue.Unavailable, emitted.last().rootAnnualPercentageYield)
        assertTrue(emitted.last().rootAnnualPercentageYield !is StakingDashboardValue.Loading)

        advanceTimeBy(120_000)
        runCurrent()
        assertSame(StakingDashboardValue.Unavailable, emitted.last().rootAnnualPercentageYield)
        verify(validators, times(1)).yields(BittensorNetuid.Root)

        val twoValidators = listOf(BittensorRootPosition(hotkey, BigInteger.TEN), BittensorRootPosition(otherHotkey, BigInteger.TEN))
        portfolioState.value = BittensorPortfolioState(portfolio.copy(root = twoValidators), BittensorDataStatus.Fresh)
        runCurrent()
        assertKnownYieldEquals(BigDecimal("5.5"), emitted.last().rootAnnualPercentageYield)
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
        val details = source.observe(account, listOf(chain))
            .first { it["chain"]?.maximumAnnualPercentageYield is StakingDashboardValue.Known }
            .getValue("chain")
        assertEquals(StakingDashboardValue.Known(BigInteger.ZERO), details.totalStake)
        assertEquals(StakingDashboardValue.Known(BigDecimal.ONE), details.maximumAnnualPercentageYield)
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
        portfolioState.value = BittensorPortfolioState(emptyPositions, BittensorDataStatus.Fresh)
        `when`(recommendations.maximumYields()).thenThrow(IllegalStateException("offline")).thenReturn(mapOf(subnet to BigDecimal.ONE))
        val emitted = mutableListOf<BittensorDashboardDetails>()
        val job = backgroundScope.launch { source.observe(account, listOf(chain)).collect { it["chain"]?.let(emitted::add) } }
        runCurrent()
        advanceTimeBy(120_000)
        runCurrent()
        assertSame(StakingDashboardValue.Unavailable, emitted.last().maximumAnnualPercentageYield)
        assertTrue(emitted.last().maximumAnnualPercentageYield !is StakingDashboardValue.Loading)
        verify(recommendations, times(1)).maximumYields()

        source.refreshMaximumYield()
        runCurrent()
        assertEquals(StakingDashboardValue.Known(BigDecimal.ONE), emitted.last().maximumAnnualPercentageYield)
        verify(recommendations, times(2)).maximumYields()
        job.cancel()
    }

    @Test
    fun `no stake requests recommendations and claimable rewards count as active`() = runTest {
        prepare()
        portfolioState.value = BittensorPortfolioState(emptyPositions, BittensorDataStatus.Fresh)
        val emitted = mutableListOf<BittensorDashboardDetails>()
        val job = backgroundScope.launch { source.observe(account, listOf(chain)).collect { it["chain"]?.let(emitted::add) } }
        runCurrent()
        assertEquals(StakingDashboardValue.Known(BigDecimal.ONE), emitted.last().maximumAnnualPercentageYield)
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
        portfolioState.value = BittensorPortfolioState(positions = null, BittensorDataStatus.Stale(IllegalStateException("offline")))
        `when`(validators.yields(BittensorNetuid.Root)).thenThrow(IllegalStateException("offline"))
        val details = source.observe(account, listOf(chain)).first().getValue("chain")

        assertEquals(StakingDashboardValue.Known(BigInteger.TEN), details.totalStake)
        assertEquals(StakingDashboardValue.Known(BigInteger.TEN), details.panelStake)
        assertKnownYieldEquals(BigDecimal.TEN, details.panelAnnualPercentageYield)
        assertTrue(details.panelAnnualPercentageYield !is StakingDashboardValue.Loading)
    }

    @Test
    fun `known zero snapshot remains a value while positions reload`() = runTest {
        prepare()
        portfolioState.value = BittensorPortfolioState(emptyPositions, BittensorDataStatus.Fresh)
        val first = backgroundScope.launch { source.observe(account, listOf(chain)).collect() }
        runCurrent()
        first.cancel()

        portfolioState.value = BittensorPortfolioState(positions = null, status = BittensorDataStatus.Loading)
        val details = source.observe(account, listOf(chain)).first().getValue("chain")

        assertEquals(StakingDashboardValue.Known(BigInteger.ZERO), details.totalStake)
        assertEquals(BigInteger.ZERO, snapshots.snapshot("chain", coldKey)?.totalStakeInPlanks)
    }

    @Test
    fun `another coldkey does not restore the previous account valuation`() = runTest {
        prepare()
        val first = backgroundScope.launch { source.observe(account, listOf(chain)).collect() }
        runCurrent()
        first.cancel()

        val otherColdKey: AccountId = ByteArray(32) { 3 }
        `when`(account.accountIdIn(chain)).thenReturn(otherColdKey)
        `when`(positions.observePortfolio("chain", otherColdKey)).thenReturn(portfolioState)
        portfolioState.value = BittensorPortfolioState(positions = null, status = BittensorDataStatus.Loading)
        val details = source.observe(account, listOf(chain)).first().getValue("chain")

        assertSame(StakingDashboardValue.Loading, details.totalStake)
        assertNull(details.rootStakeInPlanks)
        assertNull(snapshots.snapshot("chain", otherColdKey))
    }

    @Test
    fun `missing snapshot yield loads with positions and settles unavailable on failure`() = runTest {
        prepare()
        `when`(validators.yields(BittensorNetuid.Root)).thenThrow(IllegalStateException("offline"))
        val first = backgroundScope.launch { source.observe(account, listOf(chain)).collect() }
        runCurrent()
        first.cancel()

        portfolioState.value = BittensorPortfolioState(positions = null, status = BittensorDataStatus.Loading)
        val pending = source.observe(account, listOf(chain)).first().getValue("chain")
        assertSame(StakingDashboardValue.Loading, pending.rootAnnualPercentageYield)
        assertEquals(StakingDashboardValue.Known(BigInteger.TEN), pending.totalStake)

        portfolioState.value = BittensorPortfolioState(positions = null, status = BittensorDataStatus.Stale(IllegalStateException("offline")))
        val settled = source.observe(account, listOf(chain)).first().getValue("chain")
        assertSame(StakingDashboardValue.Unavailable, settled.rootAnnualPercentageYield)
        assertEquals(StakingDashboardValue.Known(BigInteger.TEN), settled.totalStake)
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
        assertEquals(StakingDashboardValue.Loading, emitted.last().totalStake)
        assertEquals(BigInteger.TEN, snapshots.snapshot("chain", coldKey)?.totalStakeInPlanks)

        portfolioState.value = BittensorPortfolioState(withSubnet, BittensorDataStatus.Fresh, pricesPending = false)
        runCurrent()
        assertEquals(StakingDashboardValue.Unavailable, emitted.last().totalStake)
        assertNull(snapshots.snapshot("chain", coldKey)?.totalStakeInPlanks)
        job.cancel()
    }

    @Test
    fun `stored maximum yield is shown while positions are unknown and kept when its request fails`() = runTest {
        prepare()
        val noStake = BittensorPortfolioState(emptyPositions, BittensorDataStatus.Fresh)
        portfolioState.value = noStake
        val first = backgroundScope.launch { source.observe(account, listOf(chain)).collect() }
        runCurrent()
        first.cancel()

        portfolioState.value = BittensorPortfolioState(positions = null, BittensorDataStatus.Loading)
        val emitted = mutableListOf<BittensorDashboardDetails>()
        val job = backgroundScope.launch { source.observe(account, listOf(chain)).collect { it["chain"]?.let(emitted::add) } }
        runCurrent()
        assertEquals(StakingDashboardValue.Known(BigDecimal.ONE), emitted.last().maximumAnnualPercentageYield)
        assertTrue(emitted.last().maximumAnnualPercentageYield !is StakingDashboardValue.Loading)

        `when`(recommendations.maximumYields()).thenThrow(IllegalStateException("offline"))
        portfolioState.value = noStake
        runCurrent()
        assertEquals(StakingDashboardValue.Known(BigDecimal.ONE), emitted.last().maximumAnnualPercentageYield)
        assertEquals(BigDecimal.ONE, snapshots.snapshot("chain", coldKey)?.maximumAnnualPercentageYield)
        verify(recommendations, times(2)).maximumYields()
        job.cancel()
    }

    @Test
    fun `identical fresh details do not write the same snapshot again`() = runTest {
        prepare()
        val recordingSnapshots = spy(snapshots)
        val observedSource = BittensorDashboardDetailsSource(positions, validators, recommendations, recordingSnapshots, logger)
        val job = backgroundScope.launch { observedSource.observe(account, listOf(chain)).collect() }
        runCurrent()
        val persisted = requireNotNull(snapshots.snapshot("chain", coldKey))

        portfolioState.value = BittensorPortfolioState(portfolio, BittensorDataStatus.Stale(IllegalStateException("offline")))
        runCurrent()
        portfolioState.value = BittensorPortfolioState(portfolio, BittensorDataStatus.Fresh)
        runCurrent()

        verify(recordingSnapshots, times(1)).saveSnapshot("chain", coldKey, persisted)
        job.cancel()
    }

    @Test
    fun `changed stale positions are displayed but only fresh figures replace the saved snapshot`() = runTest {
        prepare()
        val emitted = mutableListOf<BittensorDashboardDetails>()
        val job = backgroundScope.launch { source.observe(account, listOf(chain)).collect { it["chain"]?.let(emitted::add) } }
        runCurrent()
        assertEquals(BigInteger.TEN, snapshots.snapshot("chain", coldKey)?.rootStakeInPlanks)

        val changedPositions = portfolio.copy(root = listOf(BittensorRootPosition(hotkey, BigInteger.valueOf(20))))
        portfolioState.value = BittensorPortfolioState(changedPositions, BittensorDataStatus.Stale(IllegalStateException("offline")))
        runCurrent()
        assertEquals(BigInteger.valueOf(20), emitted.last().rootStakeInPlanks)
        assertEquals(BigInteger.TEN, snapshots.snapshot("chain", coldKey)?.rootStakeInPlanks)

        portfolioState.value = BittensorPortfolioState(changedPositions, BittensorDataStatus.Fresh)
        runCurrent()
        assertEquals(BigInteger.valueOf(20), snapshots.snapshot("chain", coldKey)?.rootStakeInPlanks)
        job.cancel()
    }

    @Test
    fun `successful empty recommendations clear the previously shown maximum yield`() = runTest {
        prepare()
        portfolioState.value = BittensorPortfolioState(emptyPositions, BittensorDataStatus.Fresh)
        val emitted = mutableListOf<BittensorDashboardDetails>()
        val job = backgroundScope.launch { source.observe(account, listOf(chain)).collect { it["chain"]?.let(emitted::add) } }
        runCurrent()
        assertEquals(StakingDashboardValue.Known(BigDecimal.ONE), emitted.last().maximumAnnualPercentageYield)

        `when`(recommendations.maximumYields()).thenReturn(emptyMap())
        source.refreshMaximumYield()
        runCurrent()

        assertSame(StakingDashboardValue.Unavailable, emitted.last().maximumAnnualPercentageYield)
        assertTrue(emitted.last().maximumAnnualPercentageYield !is StakingDashboardValue.Loading)
        assertNull(snapshots.snapshot("chain", coldKey)?.maximumAnnualPercentageYield)
        verify(recommendations, times(2)).maximumYields()
        job.cancel()
    }

    @Test
    fun `recommendations wait for unknown positions and load once they resolve to no stake`() = runTest {
        prepare()
        portfolioState.value = BittensorPortfolioState(positions = null, BittensorDataStatus.Loading)
        val emitted = mutableListOf<BittensorDashboardDetails>()
        val job = backgroundScope.launch { source.observe(account, listOf(chain)).collect { it["chain"]?.let(emitted::add) } }
        runCurrent()
        assertSame(StakingDashboardValue.Loading, emitted.last().maximumAnnualPercentageYield)
        verifyNoInteractions(recommendations)

        portfolioState.value = BittensorPortfolioState(positions = null, BittensorDataStatus.Stale(IllegalStateException("offline")))
        runCurrent()
        assertTrue(emitted.last().maximumAnnualPercentageYield !is StakingDashboardValue.Loading)
        verifyNoInteractions(recommendations)

        portfolioState.value = BittensorPortfolioState(emptyPositions, BittensorDataStatus.Fresh)
        runCurrent()
        assertEquals(StakingDashboardValue.Known(BigDecimal.ONE), emitted.last().maximumAnnualPercentageYield)
        verify(recommendations, times(1)).maximumYields()
        job.cancel()
    }

    @Test
    fun `without a stored snapshot unknown positions stay loading`() = runTest {
        prepare()
        portfolioState.value = BittensorPortfolioState(positions = null, BittensorDataStatus.Loading)
        val details = source.observe(account, listOf(chain)).first().getValue("chain")
        assertEquals(StakingDashboardValue.Loading, details.totalStake)
    }

    @Test
    fun `failed initial positions settle pricing and recover after retry without a snapshot`() = runTest {
        prepare()
        portfolioState.value = BittensorPortfolioState(positions = null, BittensorDataStatus.Loading)
        val stakingState = AggregatedStakingDashboardOption.HasStake(
            showStakingType = false,
            stakingType = Chain.Asset.StakingType.BITTENSOR_ROOT,
            stake = StakingDashboardValue.Known(BigInteger.TEN),
            stats = ExtendedLoadingState.Loading
        )
        val option = AggregatedStakingDashboardOption(chain, token, stakingState, AggregatedStakingDashboardOption.SyncingStage.SYNCED)
        val emitted = mutableListOf<BittensorDashboardDetails>()
        val dashboardDetails = mutableListOf<BittensorDashboardDetails>()
        val job = backgroundScope.launch {
            source.observe(account, listOf(chain)).collect { details ->
                details["chain"]?.let(emitted::add)
                dashboardDetails += requireNotNull(option.withBittensorDetails(details).bittensorDetails)
            }
        }
        runCurrent()
        assertEquals(StakingDashboardValue.Loading, emitted.last().totalStake)
        assertEquals(StakingDashboardValue.Loading, dashboardDetails.last().totalStake)

        portfolioState.value = BittensorPortfolioState(positions = null, BittensorDataStatus.Stale(IllegalStateException("offline")))
        runCurrent()
        val unavailable = emitted.last()
        assertEquals(StakingDashboardValue.Unavailable, dashboardDetails.last().totalStake)
        assertSame(unavailable, dashboardDetails.last())
        assertSame(StakingDashboardValue.Unavailable, unavailable.totalStake)
        assertNull(unavailable.rootStakeInPlanks)
        assertSame(StakingDashboardValue.Unavailable, unavailable.panelStake)
        assertTrue(unavailable.rootAnnualPercentageYield !is StakingDashboardValue.Loading)
        assertTrue(unavailable.subnetAnnualPercentageYield !is StakingDashboardValue.Loading)
        assertTrue(unavailable.maximumAnnualPercentageYield !is StakingDashboardValue.Loading)
        assertNull(snapshots.snapshot("chain", coldKey))
        verifyNoInteractions(validators, recommendations)

        portfolioState.value = BittensorPortfolioState(positions = null, BittensorDataStatus.Loading)
        runCurrent()
        assertEquals(StakingDashboardValue.Loading, emitted.last().totalStake)
        assertEquals(StakingDashboardValue.Loading, dashboardDetails.last().totalStake)

        portfolioState.value = BittensorPortfolioState(portfolio, BittensorDataStatus.Fresh)
        runCurrent()
        assertSame(emitted.last(), dashboardDetails.last())
        assertEquals(StakingDashboardValue.Known(BigInteger.TEN), emitted.last().totalStake)
        assertEquals(BigInteger.TEN, snapshots.snapshot("chain", coldKey)?.totalStakeInPlanks)
        verify(validators, times(1)).yields(BittensorNetuid.Root)
        verifyNoInteractions(recommendations)
        job.cancel()
    }

    @Test
    fun `failed initial positions preserve claimable root rewards without inventing a stake`() = runTest {
        prepare()
        portfolioState.value = BittensorPortfolioState(positions = null, BittensorDataStatus.Loading)
        snapshots.saveClaimableRootRewards("chain", coldKey, BigInteger.TEN)
        val emitted = mutableListOf<BittensorDashboardDetails>()
        val job = backgroundScope.launch { source.observe(account, listOf(chain)).collect { it["chain"]?.let(emitted::add) } }
        runCurrent()
        assertEquals(StakingDashboardValue.Loading, emitted.last().totalStake)

        portfolioState.value = BittensorPortfolioState(positions = null, BittensorDataStatus.Stale(IllegalStateException("offline")))
        runCurrent()
        val unavailable = emitted.last()
        assertSame(StakingDashboardValue.Unavailable, unavailable.totalStake)
        assertNull(unavailable.rootStakeInPlanks)
        assertTrue(unavailable.showsClaimableRewardsPanel)
        assertEquals(StakingDashboardValue.Known(BigInteger.TEN), unavailable.panelStake)
        assertSame(StakingDashboardValue.Unavailable, unavailable.panelAnnualPercentageYield)
        assertTrue(unavailable.panelAnnualPercentageYield !is StakingDashboardValue.Loading)
        assertNull(snapshots.snapshot("chain", coldKey))
        verifyNoInteractions(validators, recommendations)
        job.cancel()
    }

    private fun assertKnownYieldEquals(expected: BigDecimal, actual: StakingDashboardValue<BigDecimal>) {
        val knownYield = actual as StakingDashboardValue.Known<BigDecimal>
        assertEquals(0, expected.compareTo(knownYield.value))
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
