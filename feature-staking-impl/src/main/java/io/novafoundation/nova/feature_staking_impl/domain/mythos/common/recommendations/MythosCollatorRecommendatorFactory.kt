package io.novafoundation.nova.feature_staking_impl.domain.mythos.common.recommendations

import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.data.memory.ComputationalScope
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.chain
import io.novafoundation.nova.feature_staking_impl.data.mythos.repository.MythosStakingRepository
import io.novafoundation.nova.feature_staking_impl.data.validators.ValidatorsPreferencesSource
import io.novafoundation.nova.feature_staking_impl.domain.common.singleSelect.recommendations.FilteringSingleSelectRecommendatorFactory
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.collator.MythosCollatorProvider
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.collator.MythosCollatorProvider.MythosCollatorSource
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.model.MythosCollator
import javax.inject.Inject

@FeatureScope
class MythosCollatorRecommendatorFactory @Inject constructor(
    private val mythosCollatorProvider: MythosCollatorProvider,
    private val mythosStakingRepository: MythosStakingRepository,
    computationalCache: ComputationalCache,
    validatorsPreferencesSource: ValidatorsPreferencesSource
) : FilteringSingleSelectRecommendatorFactory<MythosCollator>(computationalCache, validatorsPreferencesSource) {

    context(ComputationalScope)
    override suspend fun getAllTargets(stakingOption: StakingOption): List<MythosCollator> {
        val maxStakers = mythosStakingRepository.maxDelegatorsPerCollator(stakingOption.chain.id)

        return mythosCollatorProvider.getCollators(stakingOption, MythosCollatorSource.ElectedCandidates)
            .filter { it.delegators < maxStakers }
    }
}
