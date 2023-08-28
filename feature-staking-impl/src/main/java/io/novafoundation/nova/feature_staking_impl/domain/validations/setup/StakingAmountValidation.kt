package io.novafoundation.nova.feature_staking_impl.domain.validations.setup

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validationError
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.SingleStakingProperties
import java.math.BigInteger

class StakingAmountValidation<P, E>(
    private val singleStakingProperties: SingleStakingProperties,
    private val amount: (P) -> BigInteger,
    private val error: (P) -> E,
) : Validation<P, E> {

    override suspend fun validate(value: P): ValidationStatus<E> {
        return if (singleStakingProperties.minStake() <= amount(value)) {
            valid()
        } else {
            validationError(error(value))
        }
    }
}

fun <P, E> ValidationSystemBuilder<P, E>.stakingAmountValidation(
    singleStakingProperties: SingleStakingProperties,
    amount: (P) -> BigInteger,
    errorFormatter: (P) -> E,
) {
    validate(
        StakingAmountValidation(
            singleStakingProperties,
            amount,
            errorFormatter,
        )
    )
}
