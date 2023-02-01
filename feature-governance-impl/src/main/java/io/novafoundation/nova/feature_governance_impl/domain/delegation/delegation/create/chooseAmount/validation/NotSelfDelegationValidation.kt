package io.novafoundation.nova.feature_governance_impl.domain.delegation.delegation.create.chooseAmount.validation

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.isFalseOrError
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.requireIdOfSelectedMetaAccountIn
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.runtime.state.chain

class NotSelfDelegationValidation(
    private val governanceSharedState: GovernanceSharedState,
    private val accountRepository: AccountRepository,
) : ChooseDelegationAmountValidation {

    override suspend fun validate(value: ChooseDelegationAmountValidationPayload): ValidationStatus<ChooseDelegationAmountValidationFailure> {
        val chain = governanceSharedState.chain()
        val origin = accountRepository.requireIdOfSelectedMetaAccountIn(chain)

        return origin.contentEquals(value.delegate) isFalseOrError {
            ChooseDelegationAmountValidationFailure.CannotDelegateToSelf
        }
    }
}

fun ChooseDelegationAmountValidationSystemBuilder.notSelfDelegation(
    governanceSharedState: GovernanceSharedState,
    accountRepository: AccountRepository
) {
    validate(NotSelfDelegationValidation(governanceSharedState, accountRepository))
}
