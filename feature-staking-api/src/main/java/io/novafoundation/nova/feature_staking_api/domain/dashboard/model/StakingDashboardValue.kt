package io.novafoundation.nova.feature_staking_api.domain.dashboard.model

import io.novafoundation.nova.common.utils.isZero
import java.math.BigInteger

/** A dashboard calculation or read. Known zero remains a value. */
sealed interface StakingDashboardValue<out T : Any> {

    object Loading : StakingDashboardValue<Nothing>

    object Unavailable : StakingDashboardValue<Nothing>

    data class Known<T : Any>(val value: T) : StakingDashboardValue<T>
}

fun StakingDashboardValue<BigInteger>.isKnownAndZero(): Boolean {
    return this is StakingDashboardValue.Known && value.isZero
}
