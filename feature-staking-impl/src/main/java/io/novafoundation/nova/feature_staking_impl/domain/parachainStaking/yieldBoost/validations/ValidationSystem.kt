package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.yieldBoost.validations

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.feature_staking_api.data.parachainStaking.turing.repository.TuringAutomationTasksRepository
import io.novafoundation.nova.feature_wallet_api.domain.validation.sufficientBalance

typealias YieldBoostValidationSystem = ValidationSystem<YieldBoostValidationPayload, YieldBoostValidationFailure>
typealias YieldBoostValidation = Validation<YieldBoostValidationPayload, YieldBoostValidationFailure>
typealias YieldBoostValidationBuilder = ValidationSystemBuilder<YieldBoostValidationPayload, YieldBoostValidationFailure>

fun ValidationSystem.Companion.yieldBoost(
    automationTasksRepository: TuringAutomationTasksRepository
): YieldBoostValidationSystem = ValidationSystem {
    sufficientBalance(
        fee = { it.fee },
        available = { it.asset.transferable },
        error = { payload, availableToPayFees ->
            YieldBoostValidationFailure.NotEnoughToPayToPayFees(
                chainAsset = payload.asset.token.configuration,
                maxUsable = availableToPayFees,
                fee = payload.fee
            )
        }
    )

    cancelActiveTasks()

    firstTaskCanExecute(automationTasksRepository)
}
