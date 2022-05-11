package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.main.welcome

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.common.validation.isTrueOrError
import io.novafoundation.nova.feature_account_api.domain.model.hasAccountIn

class HasEthereumAccountValidation : ParachainStakingWelcomeValidation {

    override suspend fun validate(value: ParachainStakingWelcomeValidationPayload): ValidationStatus<ParachainStakingWelcomeValidationFailure> {
       val hasEthereumAccount = value.account.hasAccountIn(value.chain)

        return hasEthereumAccount isTrueOrError {
            ParachainStakingWelcomeValidationFailure.MissingEthereumAccount(value.chain, value.account)
        }
    }
}

fun ValidationSystemBuilder<ParachainStakingWelcomeValidationPayload, ParachainStakingWelcomeValidationFailure>.hasEthereumAccount() {
    validate(HasEthereumAccountValidation())
}
