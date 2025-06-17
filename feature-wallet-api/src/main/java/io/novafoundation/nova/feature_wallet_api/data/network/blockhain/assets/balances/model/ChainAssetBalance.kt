package io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.model

import io.novafoundation.nova.common.data.network.runtime.binding.AccountBalance
import io.novafoundation.nova.common.data.network.runtime.binding.AccountData
import io.novafoundation.nova.common.data.network.runtime.binding.edCountingMode
import io.novafoundation.nova.common.data.network.runtime.binding.transferableMode
import io.novafoundation.nova.common.domain.balance.EDCountingMode
import io.novafoundation.nova.common.domain.balance.TransferableMode
import io.novafoundation.nova.common.domain.balance.calculateBalanceCountedTowardsEd
import io.novafoundation.nova.common.domain.balance.calculateReservable
import io.novafoundation.nova.common.domain.balance.calculateTransferable
import io.novafoundation.nova.common.domain.balance.totalBalance
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal
import java.math.BigInteger

data class ChainAssetBalance(
    val chainAsset: Chain.Asset,
    val free: Balance,
    val reserved: Balance,
    val frozen: Balance,
    val transferableMode: TransferableMode,
    val edCountingMode: EDCountingMode
) {

    companion object {

        fun default(chainAsset: Chain.Asset, free: Balance, reserved: Balance, frozen: Balance): ChainAssetBalance {
            return ChainAssetBalance(chainAsset, free, reserved, frozen, TransferableMode.REGULAR, EDCountingMode.TOTAL)
        }

        fun default(chainAsset: Chain.Asset, accountBalance: AccountBalance): ChainAssetBalance {
            return default(chainAsset, free = accountBalance.free, reserved = accountBalance.reserved, frozen = accountBalance.free)
        }

        fun fromFree(chainAsset: Chain.Asset, free: Balance): ChainAssetBalance {
            return default(chainAsset, free = free, reserved = BigInteger.ZERO, frozen = BigInteger.ZERO)
        }
    }

    /**
     * Can be used to view current balance from the legacy perspective
     * Useful for pallets that still use old Currencies implementation instead of Fungibles
     */
    fun legacyAdapter(): ChainAssetBalance {
        return copy(transferableMode = TransferableMode.REGULAR, edCountingMode = EDCountingMode.TOTAL)
    }

    val total = totalBalance(free, reserved)

    val transferable = transferableMode.calculateTransferable(free, frozen, reserved)

    val countedTowardsEd = edCountingMode.calculateBalanceCountedTowardsEd(free, reserved)

    fun reservable(existentialDeposit: Balance): Balance {
        return transferableMode.calculateReservable(free = free, frozen = frozen, ed = existentialDeposit)
    }
}

fun ChainAssetBalance.transferableAmount(): BigDecimal = chainAsset.amountFromPlanks(transferable)

fun ChainAssetBalance.countedTowardsEdAmount(): BigDecimal = chainAsset.amountFromPlanks(countedTowardsEd)

fun AccountData.toChainAssetBalance(chainAsset: Chain.Asset): ChainAssetBalance {
    return ChainAssetBalance(
        chainAsset = chainAsset,
        free = free,
        reserved = reserved,
        frozen = frozen,
        transferableMode = flags.transferableMode(),
        edCountingMode = flags.edCountingMode()
    )
}
