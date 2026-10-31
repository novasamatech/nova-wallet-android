package io.novafoundation.nova.feature_staking_impl.presentation.dashboard.common

import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.BittensorDashboardDetails
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Test
import io.novafoundation.nova.common.utils.formatting.format
import java.math.BigInteger

class BittensorDashboardMappingTest {

    private val root = BittensorDashboardDetails(
        totalStakeInPlanks = BigInteger.TEN,
        rootStakeInPlanks = BigInteger.TEN,
        subnetCount = 0,
        rootAnnualPercentageYield = null,
        subnetAnnualPercentageYield = null,
        maximumAnnualPercentageYield = null,
        pricing = BittensorDashboardDetails.Pricing.KNOWN
    )

    @Test
    fun `cached dashboard keeps the same maximum yield text as live data`() {
        val resources = org.mockito.Mockito.mock(io.novafoundation.nova.common.resources.ResourceManager::class.java)
        val details = root.copy(maximumAnnualPercentageYield = java.math.BigDecimal("10"))
        val percent = io.novafoundation.nova.common.utils.Percent(10.0).format()
        org.mockito.Mockito.`when`(
            resources.getString(io.novafoundation.nova.feature_staking_impl.R.string.bittensor_dashboard_max_apy, percent)
        ).thenReturn("Max. APY 10%")

        val live = mapBittensorMaximumYield(details, resources).dataOrNull?.data
        val cached = mapBittensorMaximumYield(details.copy(stale = true), resources).dataOrNull?.data

        assertEquals("Max. APY 10%", live)
        assertEquals(live, cached)
    }

    @Test
    fun `positive root stake stops shimmer when yield request settles without a rate`() {
        assertSame(ExtendedLoadingState.Loading, mapBittensorPanelYield(root.copy(rootYieldLoading = true)))

        val settled = mapBittensorPanelYield(root)
        assertEquals("—", settled.dataOrNull?.data)
        assertFalse(requireNotNull(settled.dataOrNull).isSyncing)
    }

    @Test
    fun `pricing pending keeps shimmer until settled unknown valuation`() {
        val subnet = root.copy(rootStakeInPlanks = null, totalStakeInPlanks = null, subnetCount = 1)
        assertSame(
            ExtendedLoadingState.Loading,
            mapBittensorPanelYield(subnet.copy(pricing = BittensorDashboardDetails.Pricing.LOADING))
        )
        val settled = mapBittensorPanelYield(subnet.copy(pricing = BittensorDashboardDetails.Pricing.UNKNOWN))
        assertEquals("—", settled.dataOrNull?.data)
    }

    @Test
    fun `rewards only zero root stake does not wait for irrelevant yield`() {
        val rewardsOnly = root.copy(rootStakeInPlanks = BigInteger.ZERO, totalStakeInPlanks = BigInteger.ZERO, rootYieldLoading = true)
        assertEquals("—", mapBittensorPanelYield(rewardsOnly).dataOrNull?.data)
    }
}
