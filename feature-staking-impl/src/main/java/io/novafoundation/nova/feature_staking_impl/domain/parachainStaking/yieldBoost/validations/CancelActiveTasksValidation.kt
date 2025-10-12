package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.yieldBoost.validations

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validationWarning
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model.accountId
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.yieldBoost.YieldBoostConfiguration
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.yieldBoost.findByCollator
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.yieldBoost.validations.YieldBoostValidationFailure.WillCancelAllExistingTasks

class CancelActiveTasksValidation : YieldBoostValidation {

    override suspend fun validate(value: YieldBoostValidationPayload): ValidationStatus<YieldBoostValidationFailure> {
        return when {
            // there is no active yield boost tasks so we won't cancel anything
            value.activeTasks.isEmpty() -> valid()

            // cancel transactions are always OK
            value.configuration is YieldBoostConfiguration.Off -> valid()

            // user wants to change existing task
            value.activeTasks.findByCollator(value.collator.accountId()) != null -> valid()

            else -> WillCancelAllExistingTasks(newCollator = value.collator).validationWarning()
        }
    }
}

fun YieldBoostValidationBuilder.cancelActiveTasks() {
    validate(CancelActiveTasksValidation())
}
