package io.novafoundation.nova.feature_staking_impl.domain.dashboard

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.asLoaded
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.AggregatedStakingDashboardOption
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.BittensorDashboardDetails.Composition
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.StakingDashboardValue
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

    private val noValidatorYields = emptyMap<AccountIdKey, BigDecimal>()

    @Test
    fun `loading details have pending values without invented holdings or rewards`() {
        val details = loadingBittensorDashboardDetails()

        assertSame(StakingDashboardValue.Loading, details.totalStake)
        assertSame(StakingDashboardValue.Loading, details.rootAnnualPercentageYield)
        assertSame(StakingDashboardValue.Loading, details.subnetAnnualPercentageYield)
        assertSame(StakingDashboardValue.Loading, details.maximumAnnualPercentageYield)
        assertSame(StakingDashboardValue.Loading, details.panelStake)
        assertEquals(Composition.NONE, details.composition)
        assertNull(details.rootStakeInPlanks)
        assertNull(details.claimableRootRewardsInPlanks)
    }

    @Test
    fun `root APY is stake weighted and remains percent`() {
        val positions = BittensorPortfolioPositions(
            listOf(BittensorRootPosition(first, planks(1)), BittensorRootPosition(second, planks(3))),
            subnets = emptyList(),
            subnetPrices = emptyMap()
        )
        val details = mapBittensorDashboardDetails(
            positions,
            rootYields = mapOf(first to percentage(10), second to percentage(20)).asLoaded(),
            subnetYields = emptyMap()
        )
        assertEquals(Composition.ROOT, details.composition)
        assertEquals(StakingDashboardValue.Known(planks(4)), details.panelStake)
        assertKnownYieldEquals(percentage(17.5), details.panelAnnualPercentageYield)
        assertSame(StakingDashboardValue.Unavailable, details.maximumAnnualPercentageYield)
    }

    @Test
    fun `subnet APY weights TAO value rather than alpha units`() {
        val positions = BittensorPortfolioPositions(
            root = emptyList(),
            listOf(BittensorSubnetPosition(subnet, first, planks(10)), BittensorSubnetPosition(otherSubnet, second, planks(10))),
            mapOf(subnet to planks(1_000_000_000), otherSubnet to planks(3_000_000_000))
        )
        val rates = mapOf(subnet to mapOf(first to percentage(10)).asLoaded(), otherSubnet to mapOf(second to percentage(20)).asLoaded())
        val details = mapBittensorDashboardDetails(positions, rootYields = noValidatorYields.asLoaded(), subnetYields = rates)
        assertEquals(Composition.SUBNETS, details.composition)
        assertEquals(StakingDashboardValue.Known(planks(40)), details.panelStake)
        assertKnownYieldEquals(percentage(17.5), details.panelAnnualPercentageYield)
    }

    @Test
    fun `mixed composition shows total above and root stake and yield in panel`() {
        val positions = portfolio()
        val details = mapBittensorDashboardDetails(
            positions,
            rootYields = mapOf(first to percentage(8)).asLoaded(),
            subnetYields = mapOf(subnet to mapOf(second to percentage(30)).asLoaded())
        )
        assertEquals(Composition.MIXED, details.composition)
        assertEquals(StakingDashboardValue.Known(planks(30)), details.totalStake)
        assertEquals(StakingDashboardValue.Known(planks(10)), details.panelStake)
        assertKnownYieldEquals(percentage(8), details.panelAnnualPercentageYield)
    }

    @Test
    fun `unknown subnet price never becomes partial total or suppresses root panel`() {
        val details = mapBittensorDashboardDetails(
            portfolio().copy(subnetPrices = emptyMap()),
            rootYields = mapOf(first to percentage(8)).asLoaded(),
            subnetYields = emptyMap()
        )
        assertSame(StakingDashboardValue.Unavailable, details.totalStake)
        assertSame(StakingDashboardValue.Unavailable, details.subnetAnnualPercentageYield)
        assertEquals(StakingDashboardValue.Known(planks(10)), details.panelStake)
        assertKnownYieldEquals(percentage(8), details.panelAnnualPercentageYield)
    }

    @Test
    fun `initial loading is distinct from settled unavailable`() {
        val details = mapBittensorDashboardDetails(
            positions = null,
            rootYields = ExtendedLoadingState.Loading,
            subnetYields = emptyMap()
        )
        assertSame(StakingDashboardValue.Loading, details.totalStake)
        assertSame(StakingDashboardValue.Loading, details.panelStake)
    }

    @Test
    fun `confirmed empty positions have a known zero valuation`() {
        val emptyPositions = BittensorPortfolioPositions(root = emptyList(), subnets = emptyList(), subnetPrices = emptyMap())
        val details = mapBittensorDashboardDetails(
            emptyPositions,
            rootYields = noValidatorYields.asLoaded(),
            subnetYields = emptyMap()
        )

        assertEquals(StakingDashboardValue.Known(BigInteger.ZERO), details.totalStake)
        assertEquals(Composition.NONE, details.composition)
        assertSame(StakingDashboardValue.Unavailable, details.panelStake)
    }

    @Test
    fun `pending prices do not expose a previously calculable total`() {
        val details = mapBittensorDashboardDetails(
            portfolio(),
            rootYields = mapOf(first to percentage(8)).asLoaded(),
            subnetYields = mapOf(subnet to mapOf(second to percentage(30)).asLoaded()),
            pricingPending = true
        )

        assertSame(StakingDashboardValue.Loading, details.totalStake)
        assertEquals(StakingDashboardValue.Known(planks(10)), details.panelStake)
        assertKnownYieldEquals(percentage(8), details.rootAnnualPercentageYield)
        assertKnownYieldEquals(percentage(8), details.panelAnnualPercentageYield)
        assertSame(StakingDashboardValue.Loading, details.subnetAnnualPercentageYield)
    }

    @Test
    fun `missing validator yield does not produce partial weighted APY`() {
        val positions = BittensorPortfolioPositions(
            listOf(BittensorRootPosition(first, planks(1)), BittensorRootPosition(second, planks(3))),
            subnets = emptyList(),
            subnetPrices = emptyMap()
        )
        val details = mapBittensorDashboardDetails(
            positions,
            rootYields = mapOf(first to percentage(10)).asLoaded(),
            subnetYields = emptyMap()
        )
        assertSame(StakingDashboardValue.Unavailable, details.rootAnnualPercentageYield)
    }

    @Test
    fun `no stake selects maximum reported recommendation yield including zero`() {
        val positions = BittensorPortfolioPositions(root = emptyList(), subnets = emptyList(), subnetPrices = emptyMap())
        val rates = mapOf(subnet to percentage(0), otherSubnet to percentage(15))
        val details = mapBittensorDashboardDetails(
            positions,
            rootYields = noValidatorYields.asLoaded(),
            subnetYields = emptyMap(),
            maximumAnnualPercentageYield = maximumYield(rates)
        )
        assertEquals(Composition.NONE, details.composition)
        assertEquals(StakingDashboardValue.Known(percentage(15)), details.maximumAnnualPercentageYield)
        assertSame(StakingDashboardValue.Unavailable, details.panelAnnualPercentageYield)
    }

    @Test
    fun `reported zero yields remain known for held stake and start staking`() {
        val positions = BittensorPortfolioPositions(
            root = listOf(BittensorRootPosition(first, BigInteger.ONE)),
            subnets = emptyList(),
            subnetPrices = emptyMap()
        )
        val rates = mapOf(subnet to BigDecimal.ZERO)
        val details = mapBittensorDashboardDetails(
            positions,
            rootYields = mapOf(first to BigDecimal.ZERO).asLoaded(),
            subnetYields = emptyMap(),
            maximumAnnualPercentageYield = maximumYield(rates)
        )

        val rootYield = details.rootAnnualPercentageYield as StakingDashboardValue.Known<BigDecimal>
        assertEquals(0, BigDecimal.ZERO.compareTo(rootYield.value))
        assertEquals(StakingDashboardValue.Known(BigDecimal.ZERO), details.maximumAnnualPercentageYield)
    }

    @Test
    fun `emptied stake stays loading until the updater reclassifies it`() {
        `when`(chain.id).thenReturn("chain")
        val state = AggregatedStakingDashboardOption.HasStake(
            showStakingType = false,
            Chain.Asset.StakingType.BITTENSOR_ROOT,
            stake = StakingDashboardValue.Known(planks(0)),
            ExtendedLoadingState.Loading
        )
        val option = AggregatedStakingDashboardOption(chain, token, state, AggregatedStakingDashboardOption.SyncingStage.SYNCED)
        val empty = BittensorPortfolioPositions(root = emptyList(), subnets = emptyList(), subnetPrices = emptyMap())
        val details = mapBittensorDashboardDetails(empty, rootYields = noValidatorYields.asLoaded(), subnetYields = emptyMap())
        val mapped = option.withBittensorDetails(mapOf("chain" to details))
        assertSame(state, mapped.stakingState)
        assertEquals(Composition.NONE, mapped.bittensorDetails?.composition)
        assertSame(StakingDashboardValue.Loading, mapped.bittensorDetails?.totalStake)
        assertSame(StakingDashboardValue.Loading, mapped.bittensorDetails?.panelStake)
    }

    @Test
    fun `claimable root rewards add root to a subnet stake`() {
        val subnetOnly = BittensorPortfolioPositions(
            root = emptyList(),
            listOf(BittensorSubnetPosition(subnet, second, planks(20))),
            mapOf(subnet to planks(1_000_000_000))
        )
        val details = mapBittensorDashboardDetails(
            subnetOnly,
            rootYields = noValidatorYields.asLoaded(),
            subnetYields = emptyMap(),
            pricingPending = false,
            claimableRootRewards = planks(7)
        )
        assertEquals(Composition.MIXED, details.composition)
        assertFalse(details.showsClaimableRewardsPanel)
        assertEquals(StakingDashboardValue.Known(planks(0)), details.panelStake)
        assertSame(StakingDashboardValue.Unavailable, details.panelAnnualPercentageYield)
    }

    @Test
    fun `claimable rewards without stake get their own panel instead of a zero root stake`() {
        `when`(chain.id).thenReturn("chain")
        val state = AggregatedStakingDashboardOption.HasStake(
            showStakingType = false,
            Chain.Asset.StakingType.BITTENSOR_ROOT,
            stake = StakingDashboardValue.Known(planks(0)),
            ExtendedLoadingState.Loading
        )
        val option = AggregatedStakingDashboardOption(chain, token, state, AggregatedStakingDashboardOption.SyncingStage.SYNCED)
        val empty = BittensorPortfolioPositions(root = emptyList(), subnets = emptyList(), subnetPrices = emptyMap())
        val details = mapBittensorDashboardDetails(
            empty,
            rootYields = ExtendedLoadingState.Loading,
            subnetYields = emptyMap(),
            claimableRootRewards = planks(7)
        )
        assertSame(StakingDashboardValue.Loading, details.rootAnnualPercentageYield)
        assertEquals(planks(7), details.claimableRootRewardsInPlanks)
        val mapped = requireNotNull(option.withBittensorDetails(mapOf("chain" to details)).bittensorDetails)
        assertEquals(Composition.NONE, mapped.composition)
        assertTrue(mapped.showsClaimableRewardsPanel)
        assertEquals(StakingDashboardValue.Known(planks(7)), mapped.panelStake)
        assertSame(StakingDashboardValue.Unavailable, mapped.panelAnnualPercentageYield)
        assertTrue(mapped.panelAnnualPercentageYield !is StakingDashboardValue.Loading)
    }

    @Test
    fun `claimable rewards do not replace an existing stake panel`() {
        val details = mapBittensorDashboardDetails(
            portfolio(),
            rootYields = mapOf(first to percentage(8)).asLoaded(),
            subnetYields = emptyMap(),
            claimableRootRewards = planks(7)
        )
        assertFalse(details.showsClaimableRewardsPanel)
        assertEquals(StakingDashboardValue.Known(planks(10)), details.panelStake)
        assertEquals(planks(7), details.claimableRootRewardsInPlanks)
    }

    @Test
    fun `root panel does not wait for subnet yields`() {
        val details = mapBittensorDashboardDetails(
            portfolio(),
            rootYields = mapOf(first to percentage(8)).asLoaded(),
            subnetYields = mapOf(subnet to ExtendedLoadingState.Loading)
        )
        assertTrue(details.showsRootPanel)
        assertKnownYieldEquals(percentage(8), details.panelAnnualPercentageYield)
        assertSame(StakingDashboardValue.Loading, details.subnetAnnualPercentageYield)
    }

    @Test
    fun `loading root yield leaves priced holdings and loaded subnet yield available`() {
        val details = mapBittensorDashboardDetails(
            portfolio(),
            rootYields = ExtendedLoadingState.Loading,
            subnetYields = mapOf(subnet to mapOf(second to percentage(30)).asLoaded())
        )

        assertSame(StakingDashboardValue.Loading, details.rootAnnualPercentageYield)
        assertSame(StakingDashboardValue.Loading, details.panelAnnualPercentageYield)
        assertEquals(StakingDashboardValue.Known(planks(30)), details.totalStake)
        assertEquals(StakingDashboardValue.Known(planks(10)), details.panelStake)
        assertKnownYieldEquals(percentage(30), details.subnetAnnualPercentageYield)
    }

    @Test
    fun `failed root yield settles unavailable without hiding holdings or subnet yield`() {
        val details = mapBittensorDashboardDetails(
            portfolio(),
            rootYields = ExtendedLoadingState.Error(IllegalStateException("Root yields failed")),
            subnetYields = mapOf(subnet to mapOf(second to percentage(30)).asLoaded())
        )

        assertSame(StakingDashboardValue.Unavailable, details.rootAnnualPercentageYield)
        assertSame(StakingDashboardValue.Unavailable, details.panelAnnualPercentageYield)
        assertEquals(StakingDashboardValue.Known(planks(30)), details.totalStake)
        assertEquals(StakingDashboardValue.Known(planks(10)), details.panelStake)
        assertKnownYieldEquals(percentage(30), details.subnetAnnualPercentageYield)
    }

    @Test
    fun `loaded and loading subnet yields keep aggregate pending without hiding root yield`() {
        val positions = portfolio().copy(
            subnets = listOf(BittensorSubnetPosition(subnet, second, planks(20)), BittensorSubnetPosition(otherSubnet, first, planks(10))),
            subnetPrices = mapOf(subnet to planks(1_000_000_000), otherSubnet to planks(1_000_000_000))
        )
        val details = mapBittensorDashboardDetails(
            positions,
            rootYields = mapOf(first to percentage(8)).asLoaded(),
            subnetYields = mapOf(
                subnet to mapOf(second to percentage(30)).asLoaded(),
                otherSubnet to ExtendedLoadingState.Loading
            )
        )

        assertSame(StakingDashboardValue.Loading, details.subnetAnnualPercentageYield)
        assertKnownYieldEquals(percentage(8), details.panelAnnualPercentageYield)
        assertEquals(StakingDashboardValue.Known(planks(40)), details.totalStake)
        assertEquals(StakingDashboardValue.Known(planks(10)), details.panelStake)
        assertEquals(2, details.subnetCount)
    }

    @Test
    fun `loaded and failed subnet yields settle unavailable without a partial APY`() {
        val positions = portfolio().copy(
            subnets = listOf(BittensorSubnetPosition(subnet, second, planks(20)), BittensorSubnetPosition(otherSubnet, first, planks(10))),
            subnetPrices = mapOf(subnet to planks(1_000_000_000), otherSubnet to planks(1_000_000_000))
        )
        val details = mapBittensorDashboardDetails(
            positions,
            rootYields = mapOf(first to percentage(8)).asLoaded(),
            subnetYields = mapOf(
                subnet to mapOf(second to percentage(30)).asLoaded(),
                otherSubnet to ExtendedLoadingState.Error(IllegalStateException("Subnet yields failed"))
            )
        )

        assertSame(StakingDashboardValue.Unavailable, details.subnetAnnualPercentageYield)
        assertKnownYieldEquals(percentage(8), details.panelAnnualPercentageYield)
        assertEquals(StakingDashboardValue.Known(planks(40)), details.totalStake)
        assertEquals(StakingDashboardValue.Known(planks(10)), details.panelStake)
        assertEquals(2, details.subnetCount)
    }

    @Test
    fun `subnet panel does not wait for root yield`() {
        val subnetOnly = portfolio().copy(root = emptyList())
        val details = mapBittensorDashboardDetails(
            subnetOnly,
            rootYields = ExtendedLoadingState.Loading,
            subnetYields = mapOf(subnet to mapOf(second to percentage(30)).asLoaded())
        )

        assertEquals(Composition.SUBNETS, details.composition)
        assertFalse(details.showsRootPanel)
        assertSame(StakingDashboardValue.Loading, details.rootAnnualPercentageYield)
        assertKnownYieldEquals(percentage(30), details.panelAnnualPercentageYield)
        assertEquals(StakingDashboardValue.Known(planks(20)), details.panelStake)
    }

    @Test
    fun `provided maximum yield states are preserved while holdings and yields load`() {
        val maximumYieldStates = listOf(
            StakingDashboardValue.Known(percentage(15)),
            StakingDashboardValue.Loading,
            StakingDashboardValue.Unavailable
        )
        for (maximumYieldState in maximumYieldStates) {
            val details = mapBittensorDashboardDetails(
                positions = null,
                rootYields = ExtendedLoadingState.Loading,
                subnetYields = mapOf(subnet to ExtendedLoadingState.Loading),
                maximumAnnualPercentageYield = maximumYieldState
            )

            assertSame(maximumYieldState, details.maximumAnnualPercentageYield)
            assertSame(StakingDashboardValue.Loading, details.totalStake)
            assertSame(StakingDashboardValue.Loading, details.rootAnnualPercentageYield)
            assertSame(StakingDashboardValue.Loading, details.subnetAnnualPercentageYield)
        }
    }

    @Test
    fun `provided maximum yield remains known after holdings and yields fail`() {
        val maximumYieldState = StakingDashboardValue.Known(percentage(15))
        val details = mapBittensorDashboardDetails(
            positions = null,
            rootYields = ExtendedLoadingState.Error(IllegalStateException("Root yields failed")),
            subnetYields = mapOf(subnet to ExtendedLoadingState.Error(IllegalStateException("Subnet yields failed"))),
            maximumAnnualPercentageYield = maximumYieldState,
            pricingPending = false
        )

        assertSame(maximumYieldState, details.maximumAnnualPercentageYield)
        assertSame(StakingDashboardValue.Unavailable, details.totalStake)
        assertSame(StakingDashboardValue.Unavailable, details.rootAnnualPercentageYield)
        assertSame(StakingDashboardValue.Unavailable, details.subnetAnnualPercentageYield)
        assertSame(StakingDashboardValue.Unavailable, details.panelStake)
    }

    @Test
    fun `provided maximum yield is independent of loaded position yields`() {
        val maximumYieldState = StakingDashboardValue.Known(percentage(15))
        val details = mapBittensorDashboardDetails(
            portfolio(),
            rootYields = mapOf(first to percentage(8)).asLoaded(),
            subnetYields = mapOf(subnet to mapOf(second to percentage(30)).asLoaded()),
            maximumAnnualPercentageYield = maximumYieldState
        )

        assertSame(maximumYieldState, details.maximumAnnualPercentageYield)
        assertKnownYieldEquals(percentage(8), details.panelAnnualPercentageYield)
        assertKnownYieldEquals(percentage(30), details.subnetAnnualPercentageYield)
    }

    private fun assertKnownYieldEquals(expected: BigDecimal, actual: StakingDashboardValue<BigDecimal>) {
        val knownYield = actual as StakingDashboardValue.Known<BigDecimal>
        assertEquals(0, expected.compareTo(knownYield.value))
    }

    private fun portfolio() = BittensorPortfolioPositions(
        listOf(BittensorRootPosition(first, planks(10))),
        listOf(BittensorSubnetPosition(subnet, second, planks(20))),
        mapOf(subnet to planks(1_000_000_000))
    )

    private fun planks(value: Long): BigInteger = BigInteger.valueOf(value)

    private fun percentage(value: Number): BigDecimal = value.toString().toBigDecimal()
}
