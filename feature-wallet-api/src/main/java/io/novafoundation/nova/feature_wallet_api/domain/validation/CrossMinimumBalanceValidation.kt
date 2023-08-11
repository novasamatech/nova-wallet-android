package io.novafoundation.nova.feature_wallet_api.domain.validation

import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.atLeastZero
import io.novafoundation.nova.common.utils.isZero
import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.validOrWarning
import io.novafoundation.nova.feature_wallet_api.R
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.domain.validation.CrossMinimumBalanceValidation.ErrorContext
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatTokenAmount
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal

class CrossMinimumBalanceValidation<P, E>(
    private val minimumBalance: suspend (P) -> Balance,
    private val chainAsset: (P) -> Chain.Asset,
    private val currentBalance: (P) -> BigDecimal,
    private val deductingAmount: (P) -> BigDecimal,
    private val error: (ErrorContext) -> E
) : Validation<P, E> {

    class ErrorContext(
        val balanceAfterDeduction: BigDecimal,
        val minimumBalance: BigDecimal,
        val wholeAmount: BigDecimal,
        val chainAsset: Chain.Asset
    )

    override suspend fun validate(value: P): ValidationStatus<E> {
        val existentialBalanceInPlanks = minimumBalance(value)
        val chainAsset = chainAsset(value)
        val existentialBalance = chainAsset.amountFromPlanks(existentialBalanceInPlanks)

        val currentBalance = currentBalance(value)
        val balanceAfterDeduction = currentBalance - deductingAmount(value)

        val resultGreaterThanExistential = balanceAfterDeduction >= existentialBalance
        val resultIsZero = balanceAfterDeduction.atLeastZero().isZero

        return validOrWarning(resultGreaterThanExistential || resultIsZero) {
            val errorContext = ErrorContext(
                balanceAfterDeduction = balanceAfterDeduction,
                minimumBalance = existentialBalance,
                wholeAmount = currentBalance,
                chainAsset = chainAsset
            )
            error(errorContext)
        }
    }
}

interface CrossMinimumBalanceValidationFailure {

    val errorContext: ErrorContext
}

fun CrossMinimumBalanceValidationFailure.formatWith(resourceManager: ResourceManager): TitleAndMessage = with(errorContext) {
    val balanceAfterDeductionFormatted = balanceAfterDeduction.formatTokenAmount(chainAsset)
    val minimumBalanceFormatted = minimumBalance.formatTokenAmount(chainAsset)

    return resourceManager.getString(R.string.staking_unbond_crossed_existential_title) to
        resourceManager.getString(R.string.staking_unbond_crossed_existential, minimumBalanceFormatted, balanceAfterDeductionFormatted)
}
