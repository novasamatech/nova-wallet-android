package io.novafoundation.nova.feature_staking_impl.domain.dashboard

import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.StakingDashboardValue
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.isKnownAndZero
import java.math.BigInteger
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StakingDashboardValueTest {

    @Test
    fun `only a known zero matches the zero predicate`() {
        assertTrue(StakingDashboardValue.Known(BigInteger.ZERO).isKnownAndZero())
        assertFalse(StakingDashboardValue.Known(BigInteger.ONE).isKnownAndZero())
        assertFalse(StakingDashboardValue.Known(BigInteger.ONE.negate()).isKnownAndZero())
        assertFalse(StakingDashboardValue.Loading.isKnownAndZero())
        assertFalse(StakingDashboardValue.Unavailable.isKnownAndZero())
    }
}
