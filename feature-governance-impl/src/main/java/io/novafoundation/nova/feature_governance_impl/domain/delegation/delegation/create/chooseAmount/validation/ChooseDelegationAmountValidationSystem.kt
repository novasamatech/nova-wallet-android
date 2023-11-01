package io.novafoundation.nova.feature_governance_impl.domain.delegation.delegation.create.chooseAmount.validation

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_wallet_api.domain.validation.hasEnoughFreeBalance
import io.novafoundation.nova.feature_wallet_api.domain.validation.sufficientBalance

typealias ChooseDelegationAmountValidationSystem = ValidationSystem<ChooseDelegationAmountValidationPayload, ChooseDelegationAmountValidationFailure>
typealias ChooseDelegationAmountValidation = Validation<ChooseDelegationAmountValidationPayload, ChooseDelegationAmountValidationFailure>
typealias ChooseDelegationAmountValidationSystemBuilder =
    ValidationSystemBuilder<ChooseDelegationAmountValidationPayload, ChooseDelegationAmountValidationFailure>

fun ValidationSystem.Companion.chooseDelegationAmount(
    governanceSharedState: GovernanceSharedState,
    accountRepository: AccountRepository,
): ChooseDelegationAmountValidationSystem = ValidationSystem {
    sufficientBalance(
        fee = { it.fee },
        available = { it.asset.transferable },
        error = { payload, leftForFees ->
            ChooseDelegationAmountValidationFailure.NotEnoughToPayFees(
                chainAsset = payload.asset.token.configuration,
                maxUsable = leftForFees,
                fee = payload.fee
            )
        }
    )

    hasEnoughFreeBalance(
        asset = { it.asset },
        fee = { it.fee },
        requestedAmount = { it.amount },
        error = ChooseDelegationAmountValidationFailure::AmountIsTooBig
    )

    notSelfDelegation(governanceSharedState, accountRepository)

    // we do not validate for track availability since logic should take care of undelegate calls in case some delegation is being changed
}
