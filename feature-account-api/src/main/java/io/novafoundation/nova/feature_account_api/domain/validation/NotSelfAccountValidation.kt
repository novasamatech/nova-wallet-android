package io.novafoundation.nova.feature_account_api.domain.validation

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.common.validation.validOrError
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.AccountId

class NotSelfAccountValidation<P, F>(
    private val chainProvider: (P) -> Chain,
    private val accountIdProvider: (P) -> AccountId,
    private val failure: (P) -> F,
    private val accountRepository: AccountRepository,
) : Validation<P, F> {

    override suspend fun validate(value: P): ValidationStatus<F> {
        val chain = chainProvider(value)
        val accountId = accountIdProvider(value)
        val selfAccountId = accountRepository.getSelectedMetaAccount()
            .accountIdIn(chain)

        val isDifferentAccounts = !accountId.contentEquals(selfAccountId)
        return validOrError(isDifferentAccounts) {
            failure(value)
        }
    }
}

fun <P, F> ValidationSystemBuilder<P, F>.notSelfAccount(
    chainProvider: (P) -> Chain,
    accountIdProvider: (P) -> AccountId,
    failure: (P) -> F,
    accountRepository: AccountRepository,
) {
    validate(NotSelfAccountValidation(chainProvider, accountIdProvider, failure, accountRepository))
}
