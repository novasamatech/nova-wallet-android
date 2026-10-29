package io.novafoundation.nova.feature_staking_impl.presentation.dashboard.common

import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.BittensorDashboardDetails
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.StakingDashboardValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Test
import org.mockito.Mockito.mock
import java.math.BigInteger

class BittensorDashboardMappingTest {

    private val resources = mock(ResourceManager::class.java)

    private val root = BittensorDashboardDetails(
        totalStake = StakingDashboardValue.Known(BigInteger.TEN),
        rootStakeInPlanks = BigInteger.TEN,
        subnetCount = 0,
        rootAnnualPercentageYield = StakingDashboardValue.Unavailable,
        subnetAnnualPercentageYield = StakingDashboardValue.Unavailable,
        maximumAnnualPercentageYield = StakingDashboardValue.Unavailable
    )

    @Test
    fun `positive root stake stops shimmer when yield request settles without a rate`() {
        assertSame(ExtendedLoadingState.Loading, mapBittensorPanelYield(root.copy(rootAnnualPercentageYield = StakingDashboardValue.Loading)))

        val settled = mapBittensorPanelYield(root)
        assertEquals("—", settled.dataOrNull?.data)
        assertFalse(requireNotNull(settled.dataOrNull).isSyncing)
    }

    @Test
    fun `pricing pending keeps shimmer until settled unknown valuation`() {
        val subnet = root.copy(rootStakeInPlanks = null, totalStake = StakingDashboardValue.Unavailable, subnetCount = 1)
        assertSame(
            ExtendedLoadingState.Loading,
            mapBittensorPanelYield(subnet.copy(totalStake = StakingDashboardValue.Loading))
        )
        val settled = mapBittensorPanelYield(subnet.copy(totalStake = StakingDashboardValue.Unavailable))
        assertEquals("—", settled.dataOrNull?.data)
    }

    @Test
    fun `maximum yield loading is distinct from settled unavailable`() {
        val loading = root.copy(maximumAnnualPercentageYield = StakingDashboardValue.Loading)
        assertSame(ExtendedLoadingState.Loading, mapBittensorMaximumYield(loading, resources))

        val settled = mapBittensorMaximumYield(root, resources)
        assertEquals("—", settled.dataOrNull?.data)
        assertFalse(requireNotNull(settled.dataOrNull).isSyncing)
    }

    @Test
    fun `rewards only zero root stake does not wait for irrelevant yield`() {
        val rewardsOnly = root.copy(
            rootStakeInPlanks = BigInteger.ZERO,
            totalStake = StakingDashboardValue.Known(BigInteger.ZERO),
            rootAnnualPercentageYield = StakingDashboardValue.Loading
        )
        assertEquals("—", mapBittensorPanelYield(rewardsOnly).dataOrNull?.data)
    }
}
