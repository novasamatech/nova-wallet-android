package io.novafoundation.nova.feature_staking_impl.domain.recommendations

import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.chain
import io.novafoundation.nova.feature_staking_impl.data.validators.KnownNovaValidators
import io.novafoundation.nova.feature_staking_impl.domain.validators.ValidatorProvider
import io.novafoundation.nova.feature_staking_impl.domain.validators.ValidatorSource
import io.novafoundation.nova.runtime.state.selectedOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val ELECTED_VALIDATORS_CACHE = "ELECTED_VALIDATORS_CACHE"

class ValidatorRecommendatorFactory(
    private val validatorProvider: ValidatorProvider,
    private val sharedState: StakingSharedState,
    private val computationalCache: ComputationalCache,
    private val knownNovaValidators: KnownNovaValidators,
) {

    suspend fun awaitRecommendatorLoading(scope: CoroutineScope) = withContext(Dispatchers.IO) {
        loadRecommendator(scope)
    }

    suspend fun create(scope: CoroutineScope): ValidatorRecommendator = withContext(Dispatchers.IO) {
        loadRecommendator(scope)
    }

    private suspend fun loadRecommendator(scope: CoroutineScope) = computationalCache.useCache(ELECTED_VALIDATORS_CACHE, scope) {
        val stakingOption = sharedState.selectedOption()

        val sources = listOf(ValidatorSource.Elected, ValidatorSource.NovaValidators)
        val validators = validatorProvider.getValidators(stakingOption, sources, scope)
        val knownNovaValidatorIds = knownNovaValidators.getValidatorIds(stakingOption.chain.id)

        ValidatorRecommendator(validators, knownNovaValidatorIds)
    }
}
