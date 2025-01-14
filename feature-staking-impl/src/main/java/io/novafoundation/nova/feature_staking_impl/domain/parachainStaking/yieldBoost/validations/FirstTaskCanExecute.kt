package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.yieldBoost.validations

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validationError
import io.novafoundation.nova.feature_account_api.data.model.decimalAmountByExecutingAccount
import io.novafoundation.nova.feature_staking_api.data.parachainStaking.turing.repository.AutomationAction
import io.novafoundation.nova.feature_staking_api.data.parachainStaking.turing.repository.TuringAutomationTasksRepository
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.yieldBoost.YieldBoostConfiguration
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.yieldBoost.validations.YieldBoostValidationFailure.FirstTaskCannotExecute.Type.EXECUTION_FEE
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.yieldBoost.validations.YieldBoostValidationFailure.FirstTaskCannotExecute.Type.THRESHOLD
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks

class FirstTaskCanExecute(
    private val automationTasksRepository: TuringAutomationTasksRepository,
) : YieldBoostValidation {

    override suspend fun validate(value: YieldBoostValidationPayload): ValidationStatus<YieldBoostValidationFailure> {
        if (value.configuration !is YieldBoostConfiguration.On) return valid()

        val token = value.asset.token

        val balanceBeforeTransaction = value.asset.transferable
        val balanceAfterTransaction = balanceBeforeTransaction - value.fee.decimalAmountByExecutingAccount

        val chainId = value.asset.token.configuration.chainId

        val taskExecutionFeePlanks = automationTasksRepository.getTimeAutomationFees(chainId, AutomationAction.AUTO_COMPOUND_DELEGATED_STAKE, executions = 1)
        val taskExecutionFee = token.amountFromPlanks(taskExecutionFeePlanks)

        val threshold = token.amountFromPlanks(value.configuration.threshold)

        return when {
            taskExecutionFee > balanceAfterTransaction -> YieldBoostValidationFailure.FirstTaskCannotExecute(
                minimumBalanceRequired = taskExecutionFee,
                networkFee = value.fee.decimalAmountByExecutingAccount,
                availableBalanceBeforeFees = balanceBeforeTransaction,
                type = EXECUTION_FEE,
                chainAsset = token.configuration
            ).validationError()

            threshold > balanceAfterTransaction -> YieldBoostValidationFailure.FirstTaskCannotExecute(
                minimumBalanceRequired = threshold,
                networkFee = value.fee.decimalAmountByExecutingAccount,
                availableBalanceBeforeFees = balanceBeforeTransaction,
                type = THRESHOLD,
                chainAsset = token.configuration
            ).validationError()

            else -> valid()
        }
    }
}

fun YieldBoostValidationBuilder.firstTaskCanExecute(automationTasksRepository: TuringAutomationTasksRepository) {
    validate(FirstTaskCanExecute(automationTasksRepository))
}
