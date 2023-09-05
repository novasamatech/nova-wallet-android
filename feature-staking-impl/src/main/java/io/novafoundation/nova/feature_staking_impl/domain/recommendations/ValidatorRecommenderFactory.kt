package io.novafoundation.nova.feature_staking_impl.domain.recommendations

import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.feature_staking_api.domain.model.Validator
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.validators.ValidatorProvider
import io.novafoundation.nova.feature_staking_impl.domain.validators.ValidatorSource
import io.novafoundation.nova.runtime.state.selectedOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val ELECTED_VALIDATORS_CACHE = "ELECTED_VALIDATORS_CACHE"

class ValidatorRecommenderFactory(
    private val validatorProvider: ValidatorProvider,
    private val sharedState: StakingSharedState,
    private val computationalCache: ComputationalCache
) {

    suspend fun awaitValidatorLoading(scope: CoroutineScope) {
        loadValidators(scope)
    }

    private suspend fun loadValidators(scope: CoroutineScope) = computationalCache.useCache(ELECTED_VALIDATORS_CACHE, scope) {
        val stakingOption = sharedState.selectedOption()

        validatorProvider.getValidators(stakingOption, ValidatorSource.Elected, scope)
    }

    suspend fun create(scope: CoroutineScope): ValidatorRecommender = withContext(Dispatchers.IO) {
        val validators: List<Validator> = loadValidators(scope)

        ValidatorRecommender(validators)
    }
}
