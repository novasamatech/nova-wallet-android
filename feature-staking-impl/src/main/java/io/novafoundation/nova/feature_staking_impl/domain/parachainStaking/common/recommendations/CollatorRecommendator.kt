package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.recommendations

import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.data.memory.ComputationalScope
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.validators.ValidatorsPreferencesSource
import io.novafoundation.nova.feature_staking_impl.domain.common.singleSelect.recommendations.FilteringSingleSelectRecommendatorFactory
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.CollatorProvider
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.CollatorProvider.CollatorSource
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model.Collator

class CollatorRecommendatorFactory(
    private val collatorProvider: CollatorProvider,
    computationalCache: ComputationalCache,
    validatorsPreferencesSource: ValidatorsPreferencesSource
) : FilteringSingleSelectRecommendatorFactory<Collator>(computationalCache, validatorsPreferencesSource) {

    context(ComputationalScope)
    override suspend fun getAllTargets(stakingOption: StakingOption): List<Collator> {
        return collatorProvider.getCollators(stakingOption, CollatorSource.Elected)
    }
}
