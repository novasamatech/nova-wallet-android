package io.novafoundation.nova.feature_staking_impl.domain.validators.current.search

import android.annotation.SuppressLint
import io.novafoundation.nova.feature_staking_api.domain.model.Validator
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.validators.ValidatorProvider
import io.novafoundation.nova.runtime.ext.isValidAddress
import io.novafoundation.nova.runtime.state.chain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SearchCustomValidatorsInteractor(
    private val validatorProvider: ValidatorProvider,
    private val sharedState: StakingSharedState
) {

    @SuppressLint("DefaultLocale")
    suspend fun searchValidator(query: String, localValidators: Collection<Validator>): List<Validator> = withContext(Dispatchers.Default) {
        val queryLower = query.lowercase()

        val searchInLocal = localValidators.filter {
            val foundInIdentity = it.identity?.display?.lowercase()?.contains(queryLower) ?: false

            it.address.startsWith(query) || foundInIdentity
        }

        if (searchInLocal.isNotEmpty()) {
            return@withContext searchInLocal
        }

        val chain = sharedState.chain()

        if (chain.isValidAddress(query)) {
            val validator = validatorProvider.getValidatorWithoutElectedInfo(chain.id, query)

            if (validator.prefs != null) {
                listOf(validator)
            } else {
                emptyList()
            }
        } else {
            emptyList()
        }
    }
}
