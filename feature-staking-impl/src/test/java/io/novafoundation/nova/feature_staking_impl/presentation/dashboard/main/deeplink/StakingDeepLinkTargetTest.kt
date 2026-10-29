package io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.deeplink

import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.AggregatedStakingDashboardOption
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.AggregatedStakingDashboardOption.HasStake
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.AggregatedStakingDashboardOption.NoStake
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.AggregatedStakingDashboardOption.SyncingStage
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.StakingDashboard
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.StakingDashboardValue
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigInteger

class StakingDeepLinkTargetTest {

    private val chain = mock(Chain::class.java)

    private val asset = mock(Chain.Asset::class.java)

    private val token = mock(Token::class.java)

    private val target = StakingDeepLinkTarget("chain", 0)

    private val noStake = NoStake(
        ExtendedLoadingState.Loading,
        NoStake.FlowType.Single(Chain.Asset.StakingType.BITTENSOR_ROOT, false),
        BigInteger.ZERO
    )

    private val hasStake = HasStake(
        showStakingType = false,
        Chain.Asset.StakingType.BITTENSOR_ROOT,
        StakingDashboardValue.Unavailable,
        ExtendedLoadingState.Loading
    )

    @Before
    fun prepare() {
        `when`(chain.id).thenReturn("chain")
        `when`(token.configuration).thenReturn(asset)
        `when`(asset.id).thenReturn(0)
    }

    @Test
    fun `missing chain keeps dashboard navigation`() {
        assertNull(parseStakingDeepLinkTarget(null, "1"))
    }

    @Test
    fun `native asset defaults to zero and chain prefix is normalized`() {
        assertEquals(target, parseStakingDeepLinkTarget("0xchain", null))
    }

    @Test
    fun `explicit asset is preserved`() {
        assertEquals(StakingDeepLinkTarget("chain", 12), parseStakingDeepLinkTarget("chain", "12"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `malformed asset does not silently select native asset`() {
        parseStakingDeepLinkTarget("chain", "wrong")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative asset is rejected`() {
        parseStakingDeepLinkTarget("chain", "-1")
    }

    @Test
    fun `initial no stake model must wait for synchronization`() {
        val option = AggregatedStakingDashboardOption(chain, token, noStake, SyncingStage.SYNCING_ALL)
        val dashboard = StakingDashboard(emptyList(), listOf(option))

        assertNull(resolveStakingDeepLink(target, dashboard, waitingForSync = true))
    }

    @Test
    fun `no stake waits only for primary synchronization`() {
        val option = AggregatedStakingDashboardOption(chain, token, noStake, SyncingStage.SYNCING_SECONDARY)
        val dashboard = StakingDashboard(emptyList(), listOf(option))

        assertSame(option, resolveStakingDeepLink(target, dashboard, waitingForSync = true)?.option)
    }

    @Test
    fun `existing stake opens without waiting for synchronization`() {
        val state = HasStake(showStakingType = false, Chain.Asset.StakingType.RELAYCHAIN, StakingDashboardValue.Unavailable, ExtendedLoadingState.Loading)
        val option = AggregatedStakingDashboardOption(chain, token, state, SyncingStage.SYNCING_ALL)
        val dashboard = StakingDashboard(listOf(option), emptyList())

        assertSame(option, resolveStakingDeepLink(target, dashboard, waitingForSync = true)?.option)
    }

    @Test
    fun `timed out link takes a still syncing no stake item`() {
        val option = AggregatedStakingDashboardOption(chain, token, noStake, SyncingStage.SYNCING_ALL)
        val dashboard = StakingDashboard(emptyList(), listOf(option))

        assertSame(option, resolveStakingDeepLink(target, dashboard, waitingForSync = false)?.option)
    }

    @Test
    fun `unresolved item never navigates`() {
        val option = AggregatedStakingDashboardOption(chain, token, AggregatedStakingDashboardOption.NotYetResolved, SyncingStage.SYNCING_ALL)
        val dashboard = StakingDashboard(emptyList(), listOf(option))

        assertNull(resolveStakingDeepLink(target, dashboard, waitingForSync = false))
    }

    @Test
    fun `settled no stake takes the start route`() {
        val option = AggregatedStakingDashboardOption(chain, token, noStake, SyncingStage.SYNCED)
        val dashboard = StakingDashboard(emptyList(), listOf(option))

        assertSame(option, resolveStakingDeepLink(target, dashboard, waitingForSync = true)?.option)
    }

    @Test
    fun `unknown or different asset does not navigate`() {
        val option = AggregatedStakingDashboardOption(chain, token, hasStake, SyncingStage.SYNCED)
        val dashboard = StakingDashboard(listOf(option), emptyList())

        assertNull(resolveStakingDeepLink(target.copy(assetId = 1), dashboard, waitingForSync = false))
        assertNull(resolveStakingDeepLink(target.copy(chainId = "another"), dashboard, waitingForSync = false))
    }
}
