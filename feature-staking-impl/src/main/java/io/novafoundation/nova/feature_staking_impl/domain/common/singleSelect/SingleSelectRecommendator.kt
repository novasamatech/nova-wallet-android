package io.novafoundation.nova.feature_staking_impl.domain.common.singleSelect

import io.novafoundation.nova.common.data.memory.ComputationalScope
import io.novafoundation.nova.feature_staking_impl.data.StakingOption

interface SingleSelectRecommendator<T> {

    interface Factory<T> {

        suspend fun create(stakingOption: StakingOption, computationalScope: ComputationalScope): SingleSelectRecommendator<T>
    }

    fun defaultRecommendation(): T?
}
