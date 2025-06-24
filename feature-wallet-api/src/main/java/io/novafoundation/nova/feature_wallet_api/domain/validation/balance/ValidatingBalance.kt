package io.novafoundation.nova.feature_wallet_api.domain.validation.balance

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.model.ChainAssetBalance
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.model.ensureMeetsEdOrDust
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance

data class ValidatingBalance(
    val assetBalance: ChainAssetBalance,
    val existentialDeposit: Balance,
) {

    fun tryReserve(amount: Balance): BalanceValidationResult {
        return safeDeduct(checkCanReserve(amount)) { balance ->
            balance.copy(
                free = balance.free - amount,
                reserved = balance.reserved + amount
            )
        }
    }

    fun tryFreeze(amount: Balance): BalanceValidationResult {
        return safeDeduct(checkCanFreeze(amount)) { balance ->
            balance.copy(
                frozen = balance.frozen.max(amount)
            )
        }
    }

    fun tryWithdraw(
        amount: Balance,
        preservation: BalancePreservation
    ): BalanceValidationResult {
        return safeDeduct(checkCanWithdraw(amount, preservation)) { balance ->
            balance.copy(
                free = balance.free - amount
            )
        }
    }

    private fun safeDeduct(
        checkResult: BalanceCheckResult,
        unsafeDeduct: (ChainAssetBalance) -> ChainAssetBalance
    ): BalanceValidationResult {
        return when (checkResult) {
            is BalanceCheckResult.NotEnoughBalance -> {
                BalanceValidationResult.Failure(
                    checkResult.negativeImbalance,
                    checkResult.newBalanceAfterFixingImbalance
                )
            }

            BalanceCheckResult.Ok -> {
                val newBalance = unsafeDeduct(assetBalance).ensureMeetsEdOrDust()
                BalanceValidationResult.Success(copy(assetBalance = newBalance))
            }
        }
    }

    enum class BalancePreservation {
        /**
         * We do not want account's balance to become lower than ED
         */
        KEEP_ALIVE,

        /**
         * We do not care about account's balance becoming lower than ED
         */
        ALLOW_DEATH
    }

    private fun checkCanReserve(amount: Balance): BalanceCheckResult {
        val imbalance = NegativeImbalance.from(
            have = assetBalance.reservable(existentialDeposit),
            want = amount
        )

        return createBalanceCheckResult(
            imbalance = imbalance,
            deductOnResolvedImbalance = { balance -> balance.tryReserve(amount) }
        )
    }

    private fun checkCanWithdraw(
        amount: Balance,
        preservation: BalancePreservation
    ): BalanceCheckResult {
        val transferableImbalance = NegativeImbalance.from(
            have = assetBalance.transferable,
            want = amount
        )

        val countedTowardsEdImbalance = when (preservation) {
            BalancePreservation.KEEP_ALIVE -> {
                NegativeImbalance.from(
                    have = assetBalance.countedTowardsEd,
                    want = existentialDeposit + amount
                )
            }

            BalancePreservation.ALLOW_DEATH -> null
        }

        val totalImbalance = transferableImbalance.max(countedTowardsEdImbalance)

        return createBalanceCheckResult(
            imbalance = totalImbalance,
            deductOnResolvedImbalance = { balance -> balance.tryWithdraw(amount, preservation) }
        )
    }

    private fun checkCanFreeze(amount: Balance): BalanceCheckResult {
        val imbalance = NegativeImbalance.from(
            have = assetBalance.total,
            want = amount
        )

        return createBalanceCheckResult(
            imbalance = imbalance,
            deductOnResolvedImbalance = { balance -> balance.tryFreeze(amount) }
        )
    }


    private fun ChainAssetBalance.ensureMeetsEdOrDust(): ChainAssetBalance {
        return ensureMeetsEdOrDust(existentialDeposit)
    }

    private fun createBalanceCheckResult(
        imbalance: NegativeImbalance?,
        deductOnResolvedImbalance: (ValidatingBalance) -> BalanceValidationResult
    ): BalanceCheckResult {
        return if (imbalance != null) {
            val withImbalanceResolved = copy(
                assetBalance = assetBalance.copy(
                    free = assetBalance.free + imbalance.value
                )
            )

            val newBalanceAfterImbalanceResolved = deductOnResolvedImbalance(withImbalanceResolved)
            require(newBalanceAfterImbalanceResolved is BalanceValidationResult.Success) {
                "Calculated imbalance was not enough to result in successfully execution"
            }

            BalanceCheckResult.NotEnoughBalance(
                imbalance,
                newBalanceAfterImbalanceResolved.newBalance
            )
        } else {
            BalanceCheckResult.Ok
        }
    }

    private sealed class BalanceCheckResult {

        data object Ok : BalanceCheckResult()

        class NotEnoughBalance(
            val negativeImbalance: NegativeImbalance,
            val newBalanceAfterFixingImbalance: ValidatingBalance
        ) : BalanceCheckResult()
    }
}
