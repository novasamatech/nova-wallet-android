package io.novafoundation.nova.feature_staking_impl.domain.recommendations

import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.feature_staking_api.domain.model.Validator
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.validators.ValidatorProvider
import io.novafoundation.nova.feature_staking_impl.domain.validators.ValidatorSource
import io.novafoundation.nova.runtime.state.chainAndAsset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val ELECTED_VALIDATORS_CACHE = "ELECTED_VALIDATORS_CACHE"

class ValidatorRecommendatorFactory(
    private val validatorProvider: ValidatorProvider,
    private val sharedState: StakingSharedState,
    private val computationalCache: ComputationalCache
) {

    suspend fun awaitValidatorLoading(scope: CoroutineScope) {
        loadValidators(scope)
    }

    private suspend fun loadValidators(scope: CoroutineScope) = computationalCache.useCache(ELECTED_VALIDATORS_CACHE, scope) {
        val (chain, chainAsset) = sharedState.chainAndAsset()

        validatorProvider.getValidators(chain, chainAsset, ValidatorSource.Elected)
    }

    suspend fun create(scope: CoroutineScope): ValidatorRecommendator = withContext(Dispatchers.IO) {
        val validators: List<Validator> = loadValidators(scope)

        ValidatorRecommendator(validators)
    }
}
