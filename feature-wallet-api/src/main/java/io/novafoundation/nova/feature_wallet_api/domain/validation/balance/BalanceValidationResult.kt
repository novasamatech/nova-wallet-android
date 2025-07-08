package io.novafoundation.nova.feature_wallet_api.domain.validation.balance

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.feature_account_api.data.model.FeeBase
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.validation.balance.ValidatingBalance.BalancePreservation

sealed class BalanceValidationResult {

    class Success(val newBalance: ValidatingBalance) : BalanceValidationResult()

    class Failure(val negativeImbalance: NegativeImbalance, val newBalanceAfterFixingImbalance: ValidatingBalance) : BalanceValidationResult()
}

fun ValidatingBalance.beginValidation(): BalanceValidationResult {
    return BalanceValidationResult.Success(newBalance = this)
}

fun BalanceValidationResult.tryWithdraw(amount: Balance, preservation: BalancePreservation): BalanceValidationResult {
    return tryDeduct { balance -> balance.tryWithdraw(amount, preservation) }
}

fun BalanceValidationResult.tryReserve(amount: Balance): BalanceValidationResult {
    return tryDeduct { balance -> balance.tryReserve(amount) }
}

fun BalanceValidationResult.tryFreeze(amount: Balance): BalanceValidationResult {
    return tryDeduct { balance -> balance.tryFreeze(amount) }
}

fun BalanceValidationResult.tryWithdrawFee(fee: FeeBase): BalanceValidationResult {
    return tryWithdraw(fee.amount, BalancePreservation.KEEP_ALIVE)
}

fun <E> BalanceValidationResult.toValidationStatus(onError: (BalanceValidationResult.Failure) -> ValidationStatus<E>): ValidationStatus<E> {
    return when (this) {
        is BalanceValidationResult.Failure -> onError(this)
        is BalanceValidationResult.Success -> valid()
    }
}

private fun BalanceValidationResult.tryDeduct(deduct: (ValidatingBalance) -> BalanceValidationResult): BalanceValidationResult {
    return when (this) {
        is BalanceValidationResult.Failure -> deduct(newBalanceAfterFixingImbalance).increaseImbalance(negativeImbalance)
        is BalanceValidationResult.Success -> deduct(newBalance)
    }
}

private fun BalanceValidationResult.increaseImbalance(increase: NegativeImbalance): BalanceValidationResult {
    return when (this) {
        is BalanceValidationResult.Failure -> BalanceValidationResult.Failure(
            newBalanceAfterFixingImbalance = newBalanceAfterFixingImbalance,
            negativeImbalance = negativeImbalance + increase
        )

        is BalanceValidationResult.Success -> BalanceValidationResult.Failure(
            newBalanceAfterFixingImbalance = newBalance,
            negativeImbalance = increase
        )
    }
}
