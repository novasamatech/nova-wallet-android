package io.novafoundation.nova.feature_staking_impl.presentation.dashboard.common

import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.BittensorDashboardDetails
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Test
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
