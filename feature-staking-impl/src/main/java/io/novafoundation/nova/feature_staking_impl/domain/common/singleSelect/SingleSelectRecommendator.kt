package io.novafoundation.nova.feature_staking_impl.domain.common.singleSelect

import io.novafoundation.nova.common.data.memory.ComputationalScope
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.domain.common.singleSelect.recommendations.SingleSelectRecommendatorConfig

interface SingleSelectRecommendator<T> {

    interface Factory<T> {

        context(ComputationalScope)
        suspend fun create(stakingOption: StakingOption, computationalScope: ComputationalScope): SingleSelectRecommendator<T>
    }

    fun recommendations(config: SingleSelectRecommendatorConfig<T>): List<T>

    fun defaultRecommendation(): T?
}
