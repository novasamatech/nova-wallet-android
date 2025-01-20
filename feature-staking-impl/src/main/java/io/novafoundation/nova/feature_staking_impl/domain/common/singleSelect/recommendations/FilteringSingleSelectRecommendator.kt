package io.novafoundation.nova.feature_staking_impl.domain.common.singleSelect.recommendations

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.WithAccountId
import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.data.memory.ComputationalScope
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.chain
import io.novafoundation.nova.feature_staking_impl.data.validators.ValidatorsPreferencesSource
import io.novafoundation.nova.feature_staking_impl.data.validators.getExcludedValidatorIds
import io.novafoundation.nova.feature_staking_impl.data.validators.getRecommendedValidatorIds
import io.novafoundation.nova.feature_staking_impl.domain.common.singleSelect.SingleSelectRecommendator

class FilteringSingleSelectRecommendator<T : WithAccountId>(
    private val allTargets: List<T>,
    private val recommended: Set<AccountIdKey>,
    private val excluded: Set<AccountIdKey>
) : SingleSelectRecommendator<T> {

    fun recommendations(sorting: Comparator<T>): List<T> {
        return allTargets.filter { it.accountId !in excluded }
            .sortedWith(
                // accounts from recommended list first
                compareByDescending<T> { it.accountId in recommended }
                    // then by the supplied sorting rule
                    .then(sorting)
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
        stakingOption: StakingOption, computationalScope: ComputationalScope
    ) = computationalCache.useCache(javaClass.name, computationalScope) {
        val allTargets = getAllTargets(stakingOption)

        val recommended = validatorsPreferencesSource.getRecommendedValidatorIds(stakingOption.chain)
        val excluded = validatorsPreferencesSource.getExcludedValidatorIds(stakingOption.chain)

        FilteringSingleSelectRecommendator(allTargets, recommended = recommended, excluded = excluded)
    }
}
