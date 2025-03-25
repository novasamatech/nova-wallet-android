package io.novafoundation.nova.feature_staking_impl.domain.common.singleSelect.recommendations

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.WithAccountId
import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.data.memory.ComputationalScope
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.chain
import io.novafoundation.nova.feature_staking_impl.data.validators.ValidatorsPreferencesSource
import io.novafoundation.nova.feature_staking_impl.data.validators.getExcludedValidatorIdKeys
import io.novafoundation.nova.feature_staking_impl.data.validators.getRecommendedValidatorIdKeys
import io.novafoundation.nova.feature_staking_impl.domain.common.singleSelect.SingleSelectRecommendator

class FilteringSingleSelectRecommendator<T : WithAccountId>(
    private val allTargets: List<T>,
    private val recommended: Set<AccountIdKey>,
    private val excluded: Set<AccountIdKey>
) : SingleSelectRecommendator<T> {

    override fun recommendations(config: SingleSelectRecommendatorConfig<T>): List<T> {
        return allTargets.filter { it.accountId !in excluded }
            .sortedWith(
                // accounts from recommended list first
                compareByDescending<T> { it.accountId in recommended }
                    // then by the supplied sorting rule
                    .then(config)
            )
    }

    override fun defaultRecommendation(): T? {
        return allTargets.find { it.accountId in recommended }
    }
}

abstract class FilteringSingleSelectRecommendatorFactory<T : WithAccountId>(
    private val computationalCache: ComputationalCache,
    private val validatorsPreferencesSource: ValidatorsPreferencesSource
) : SingleSelectRecommendator.Factory<T> {

    context(ComputationalScope)
    protected abstract suspend fun getAllTargets(stakingOption: StakingOption): List<T>

    context(ComputationalScope)
    final override suspend fun create(
        stakingOption: StakingOption,
        computationalScope: ComputationalScope
    ) = computationalCache.useCache(javaClass.name, computationalScope) {
        val allTargets = getAllTargets(stakingOption)

        val recommended = validatorsPreferencesSource.getRecommendedValidatorIdKeys(stakingOption.chain.id)
        val excluded = validatorsPreferencesSource.getExcludedValidatorIdKeys(stakingOption.chain.id)

        FilteringSingleSelectRecommendator(allTargets, recommended = recommended, excluded = excluded)
    }
}
