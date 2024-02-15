package io.novafoundation.nova.feature_wallet_api.domain.model

import io.novafoundation.nova.common.data.network.runtime.binding.AccountBalance
import io.novafoundation.nova.common.utils.atLeastZero
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset.Companion.calculateTransferable
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset.Companion.holdAndFreezesTransferable
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset.Companion.legacyTransferable
import java.math.BigDecimal

data class Asset(
    val token: Token,

    // Non-reserved part of the balance. There may still be restrictions on
    // this, but it is the total pool what may in principle be transferred,
    // reserved.
    val freeInPlanks: Balance,

    // Balance which is reserved and may not be used at all.
    // This balance is a 'reserve' balance that different subsystems use in
    // order to set aside tokens that are still 'owned' by the account
    // holder, but which are suspendable
    val reservedInPlanks: Balance,

    // / The amount that `free` may not drop below when withdrawing.
    val frozenInPlanks: Balance,

    val transferableMode: TransferableMode,
    val edCountingMode: EDCountingMode,

    // TODO move to runtime storage
    val bondedInPlanks: Balance,
    val redeemableInPlanks: Balance,
    val unbondingInPlanks: Balance
) {

    companion object {

        fun TransferableMode.calculateTransferable(free: Balance, frozen: Balance, reserved: Balance): Balance {
            return when (this) {
                TransferableMode.REGULAR -> legacyTransferable(free, frozen)
                TransferableMode.HOLDS_AND_FREEZES -> holdAndFreezesTransferable(free, frozen, reserved)
            }
        }

        fun TransferableMode.calculateTransferable(accountBalance: AccountBalance): Balance {
            return calculateTransferable(accountBalance.free, accountBalance.frozen, accountBalance.reserved)
        }

        fun EDCountingMode.calculateBalanceCountedTowardsEd(free: Balance, reserved: Balance): Balance {
            return when (this) {
                EDCountingMode.TOTAL -> totalBalance(free, reserved)
                EDCountingMode.FREE -> free
            }
        }

        fun legacyTransferable(free: Balance, frozen: Balance): Balance {
            return (free - frozen).atLeastZero()
        }

        fun holdAndFreezesTransferable(free: Balance, frozen: Balance, reserved: Balance): Balance {
            val freeCannotDropBelow = (frozen - reserved).atLeastZero()

            return (free - freeCannotDropBelow).atLeastZero()
        }

        fun totalBalance(free: Balance, reserved: Balance): Balance {
            return free + reserved
        }
    }

    enum class TransferableMode {
        REGULAR, HOLDS_AND_FREEZES
    }

    enum class EDCountingMode {
        TOTAL, FREE
    }

    /**
     *  Liquid balance that can be transferred from an account
     *  There are multiple ways it is identified, see [legacyTransferable] and [holdAndFreezesTransferable]
     */
    val transferableInPlanks: Balance = transferableMode.calculateTransferable(freeInPlanks, frozenInPlanks, reservedInPlanks)

    /**
     * Balance that is counted towards meeting the requirement of Existential Deposit
     * When the balance
     */
    val balanceCountedTowardsEDInPlanks: Balance = edCountingMode.calculateBalanceCountedTowardsEd(freeInPlanks, reservedInPlanks)

    // Non-reserved plus reserved
    val totalInPlanks = totalBalance(freeInPlanks, reservedInPlanks)

    // balance that cannot be used for transfers (non-transferable) for any reason
    val lockedInPlanks = totalInPlanks - transferableInPlanks

    // TODO maybe move to extension fields?
    //  Check affect on performance, if those fields will be recalculated on each usage
    val total = token.amountFromPlanks(totalInPlanks)
    val reserved = token.amountFromPlanks(reservedInPlanks)
    val locked = token.amountFromPlanks(lockedInPlanks)
    val transferable = token.amountFromPlanks(transferableInPlanks)

    val free = token.amountFromPlanks(freeInPlanks)
    val frozen = token.amountFromPlanks(frozenInPlanks)

    // TODO move to runtime storage
    val bonded = token.amountFromPlanks(bondedInPlanks)
    val redeemable = token.amountFromPlanks(redeemableInPlanks)
    val unbonding = token.amountFromPlanks(unbondingInPlanks)
}

fun Asset.balanceCountedTowardsED(): BigDecimal {
    return token.amountFromPlanks(balanceCountedTowardsEDInPlanks)
}

fun Asset.transferableReplacingFrozen(newFrozen: Balance): Balance {
    return transferableMode.calculateTransferable(freeInPlanks, newFrozen, reservedInPlanks)
}

fun Asset.regularTransferableBalance(): Balance {
    return Asset.TransferableMode.REGULAR.calculateTransferable(freeInPlanks, frozenInPlanks, reservedInPlanks)
}
