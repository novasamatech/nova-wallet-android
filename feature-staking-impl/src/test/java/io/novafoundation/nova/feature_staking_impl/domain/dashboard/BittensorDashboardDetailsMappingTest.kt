package io.novafoundation.nova.feature_staking_impl.domain.dashboard

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.AggregatedStakingDashboardOption
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.BittensorDashboardDetails.Composition
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.BittensorDashboardDetails.Pricing
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioPositions
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootPosition
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetPosition
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.math.BigInteger

class BittensorDashboardDetailsMappingTest {

    private val first = AccountIdKey(ByteArray(32))

    private val second = AccountIdKey(ByteArray(32) { 1 })

    private val subnet = BittensorNetuid.Subnet(1)

    private val otherSubnet = BittensorNetuid.Subnet(2)

    private val chain = mock(Chain::class.java)

    private val token = mock(Token::class.java)

    @Test
    fun `root APY is stake weighted and remains percent`() {
        val positions = BittensorPortfolioPositions(
            listOf(BittensorRootPosition(first, n(1)), BittensorRootPosition(second, n(3))),
            emptyList(),
            emptyMap()
        )
        val details = mapBittensorDashboardDetails(positions, mapOf(first to d(10), second to d(20)), emptyMap(), emptyMap())
        assertEquals(Composition.ROOT, details.composition)
        assertEquals(n(4), details.panelStakeInPlanks)
        assertEquals(0, d(17.5).compareTo(details.panelAnnualPercentageYield))
    }

    @Test
    fun `subnet APY weights TAO value rather than alpha units`() {
        val positions = BittensorPortfolioPositions(
            emptyList(),
            listOf(BittensorSubnetPosition(subnet, first, n(10)), BittensorSubnetPosition(otherSubnet, second, n(10))),
            mapOf(subnet to n(1_000_000_000), otherSubnet to n(3_000_000_000))
        )
        val rates = mapOf(subnet to mapOf(first to d(10)), otherSubnet to mapOf(second to d(20)))
        val details = mapBittensorDashboardDetails(positions, emptyMap(), rates, emptyMap())
        assertEquals(Composition.SUBNETS, details.composition)
        assertEquals(n(40), details.panelStakeInPlanks)
        assertEquals(0, d(17.5).compareTo(details.panelAnnualPercentageYield))
    }

    @Test
    fun `mixed composition shows total above and root stake and yield in panel`() {
        val positions = portfolio()
        val details = mapBittensorDashboardDetails(positions, mapOf(first to d(8)), mapOf(subnet to mapOf(second to d(30))), emptyMap())
        assertEquals(Composition.MIXED, details.composition)
        assertEquals(n(30), details.totalStakeInPlanks)
        assertEquals(n(10), details.panelStakeInPlanks)
        assertEquals(0, d(8).compareTo(details.panelAnnualPercentageYield))
    }

    @Test
    fun `unknown subnet price never becomes partial total or suppresses root panel`() {
        val details = mapBittensorDashboardDetails(portfolio().copy(subnetPrices = emptyMap()), mapOf(first to d(8)), emptyMap(), emptyMap())
        assertEquals(Pricing.UNKNOWN, details.pricing)
        assertNull(details.totalStakeInPlanks)
        assertNull(details.subnetAnnualPercentageYield)
        assertEquals(n(10), details.panelStakeInPlanks)
        assertEquals(0, d(8).compareTo(details.panelAnnualPercentageYield))
    }

    @Test
    fun `initial loading is distinct from settled unavailable`() {
        val details = mapBittensorDashboardDetails(null, emptyMap(), emptyMap(), emptyMap())
        assertEquals(Pricing.LOADING, details.pricing)
        assertNull(details.totalStakeInPlanks)
    }

    @Test
    fun `missing validator yield does not produce partial weighted APY`() {
        val positions = BittensorPortfolioPositions(
            listOf(BittensorRootPosition(first, n(1)), BittensorRootPosition(second, n(3))),
            emptyList(),
            emptyMap()
        )
        val details = mapBittensorDashboardDetails(positions, mapOf(first to d(10)), emptyMap(), emptyMap())
        assertNull(details.rootAnnualPercentageYield)
    }

    @Test
    fun `no stake selects maximum reported recommendation yield including zero`() {
        val positions = BittensorPortfolioPositions(emptyList(), emptyList(), emptyMap())
        val details = mapBittensorDashboardDetails(positions, emptyMap(), emptyMap(), mapOf(subnet to d(0), otherSubnet to d(15)))
        assertEquals(Composition.NONE, details.composition)
        assertEquals(d(15), details.maximumAnnualPercentageYield)
        assertNull(details.panelAnnualPercentageYield)
    }

    @Test
    fun `emptied stake stays loading until the updater reclassifies it`() {
        `when`(chain.id).thenReturn("chain")
        val state = AggregatedStakingDashboardOption.HasStake(false, Chain.Asset.StakingType.BITTENSOR_ROOT, n(0), ExtendedLoadingState.Loading)
        val option = AggregatedStakingDashboardOption(chain, token, state, AggregatedStakingDashboardOption.SyncingStage.SYNCED)
        val empty = BittensorPortfolioPositions(emptyList(), emptyList(), emptyMap())
        val details = mapBittensorDashboardDetails(empty, emptyMap(), emptyMap(), emptyMap())
        val mapped = option.withBittensorDetails(mapOf("chain" to details))
        assertSame(state, mapped.stakingState)
        assertEquals(Composition.NONE, mapped.bittensorDetails?.composition)
        assertEquals(Pricing.LOADING, mapped.bittensorDetails?.pricing)
        assertNull(mapped.bittensorDetails?.totalStakeInPlanks)
        assertNull(mapped.bittensorDetails?.panelStakeInPlanks)
    }

    @Test
    fun `claimable root rewards add root to a subnet stake`() {
        val subnetOnly = BittensorPortfolioPositions(emptyList(), listOf(BittensorSubnetPosition(subnet, second, n(20))), mapOf(subnet to n(1_000_000_000)))
        val details = mapBittensorDashboardDetails(subnetOnly, emptyMap(), emptyMap(), emptyMap(), pricingPending = false, claimableRootRewards = n(7))
        assertEquals(Composition.MIXED, details.composition)
        assertFalse(details.showsClaimableRewardsPanel)
        assertEquals(n(0), details.panelStakeInPlanks)
        assertNull(details.panelAnnualPercentageYield)
    }

    @Test
    fun `claimable rewards without stake get their own panel instead of a zero root stake`() {
        `when`(chain.id).thenReturn("chain")
        val state = AggregatedStakingDashboardOption.HasStake(false, Chain.Asset.StakingType.BITTENSOR_ROOT, n(0), ExtendedLoadingState.Loading)
        val option = AggregatedStakingDashboardOption(chain, token, state, AggregatedStakingDashboardOption.SyncingStage.SYNCED)
        val empty = BittensorPortfolioPositions(emptyList(), emptyList(), emptyMap())
        val details = mapBittensorDashboardDetails(empty, emptyMap(), emptyMap(), emptyMap())
            .copy(rootYieldLoading = true, claimableRootRewardsInPlanks = n(7))
        val mapped = requireNotNull(option.withBittensorDetails(mapOf("chain" to details)).bittensorDetails)
        assertEquals(Composition.NONE, mapped.composition)
        assertTrue(mapped.showsClaimableRewardsPanel)
        assertEquals(n(7), mapped.panelStakeInPlanks)
        assertNull(mapped.panelAnnualPercentageYield)
        assertFalse(mapped.panelYieldLoading)
    }

    @Test
    fun `claimable rewards do not replace an existing stake panel`() {
        val details = mapBittensorDashboardDetails(portfolio(), mapOf(first to d(8)), emptyMap(), emptyMap())
            .copy(claimableRootRewardsInPlanks = n(7))
        assertFalse(details.showsClaimableRewardsPanel)
        assertEquals(n(10), details.panelStakeInPlanks)
    }

    @Test
    fun `root panel does not wait for subnet yields`() {
        val details = mapBittensorDashboardDetails(portfolio(), mapOf(first to d(8)), emptyMap(), emptyMap())
            .copy(rootYieldLoading = false, subnetYieldLoading = true)
        assertTrue(details.showsRootPanel)
        assertEquals(false, details.panelYieldLoading)
    }

    private fun portfolio() = BittensorPortfolioPositions(
        listOf(BittensorRootPosition(first, n(10))),
        listOf(BittensorSubnetPosition(subnet, second, n(20))),
        mapOf(subnet to n(1_000_000_000))
    )

    private fun n(value: Long): BigInteger = BigInteger.valueOf(value)

    private fun d(value: Number): BigDecimal = value.toString().toBigDecimal()
}
