package io.novafoundation.nova.feature_staking_impl.domain.validations

import io.novafoundation.nova.common.validation.DefaultFailureLevel
import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.state.chain

class AccountRequiredValidation<P, E>(
    val accountRepository: AccountRepository,
    val accountAddressExtractor: (P) -> String,
    val sharedState: StakingSharedState,
    val errorProducer: (controllerAddress: String) -> E
) : Validation<P, E> {

    override suspend fun validate(value: P): ValidationStatus<E> {
        val accountAddress = accountAddressExtractor(value)
        val chain = sharedState.chain()

        return if (accountRepository.isAccountExists(chain.accountIdOf(accountAddress), chain.id)) {
            ValidationStatus.Valid()
        } else {
            ValidationStatus.NotValid(DefaultFailureLevel.ERROR, errorProducer(accountAddress))
        }
    }
}
