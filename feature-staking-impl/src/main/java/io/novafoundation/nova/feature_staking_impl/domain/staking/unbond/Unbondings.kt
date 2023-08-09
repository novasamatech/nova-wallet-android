package io.novafoundation.nova.feature_staking_impl.domain.staking.unbond

import io.novafoundation.nova.common.utils.sumByBigInteger
import io.novafoundation.nova.feature_staking_impl.domain.model.Unbonding
import io.novafoundation.nova.feature_staking_impl.domain.model.isRedeemable
import io.novafoundation.nova.feature_staking_impl.domain.staking.unbond.Unbondings.RebondState
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import java.math.BigInteger

typealias UnbondingList = List<Unbonding>

class Unbondings(
    val unbondings: UnbondingList,
    val anythingToRedeem: Boolean,
    val rebondState: RebondState
) {

    enum class RebondState {
        CAN_REBOND, NOTHING_TO_REBOND, REBOND_NOT_POSSIBLE
    }

    companion object
}

fun UnbondingList.totalRedeemable(): Balance {
    return sumByBigInteger { unbonding -> if (unbonding.isRedeemable) unbonding.amount else BigInteger.ZERO }
}

fun Unbondings.Companion.from(unbondings: List<Unbonding>, rebondPossible: Boolean) = Unbondings(
    unbondings = unbondings,
    anythingToRedeem = unbondings.any { it.status is Unbonding.Status.Redeemable },
    rebondState = when {
        !rebondPossible -> RebondState.REBOND_NOT_POSSIBLE
        unbondings.any { it.status is Unbonding.Status.Unbonding } -> RebondState.CAN_REBOND
        else -> RebondState.NOTHING_TO_REBOND
    }
)
